"""Extract NCR public-transport relations from a Geofabrik PBF.

This is intentionally an offline import. It does not query Overpass and does
not infer passenger service from arbitrary railway ways.
"""
from __future__ import annotations

import argparse
import json
import os
import time
from collections import Counter
from pathlib import Path

import osmium

BBOX = (76.80, 28.30, 77.50, 28.90)  # lon/lat: Delhi NCR coverage envelope
ROUTE_TYPES = {"bus", "subway", "train", "light_rail", "monorail"}
STOP_KEYS = {"bus_stop", "station", "stop", "subway_entrance", "halt"}


def inside(lon: float, lat: float) -> bool:
    return BBOX[0] <= lon <= BBOX[2] and BBOX[1] <= lat <= BBOX[3]


def tags(obj):
    return {str(k): str(v) for k, v in obj.tags}


class NodeHandler(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.nodes = {}
        self.stop_nodes = {}

    def node(self, node):
        if not node.location.valid():
            return
        t = tags(node)
        if not t:
            return
        lon, lat = node.location.lon, node.location.lat
        if not inside(lon, lat):
            return
        self.nodes[node.id] = (lon, lat)
        public_stop = (
            t.get("public_transport") in {"stop_position", "platform", "stop_area"}
            or t.get("highway") == "bus_stop"
            or t.get("railway") in STOP_KEYS
        )
        if public_stop:
            self.stop_nodes[node.id] = {
                "id": node.id,
                "name": t.get("name"),
                "ref": t.get("ref"),
                "operator": t.get("operator"),
                "publicTransport": t.get("public_transport"),
                "railway": t.get("railway"),
                "highway": t.get("highway"),
                "longitude": lon,
                "latitude": lat,
            }

class WayHandler(osmium.SimpleHandler):
    def __init__(self, nodes):
        super().__init__()
        self.nodes = nodes
        self.ways = {}

    def way(self, way):
        t = tags(way)
        coords = []
        complete = True
        for node in way.nodes:
            if node.ref in self.nodes:
                coords.append(self.nodes[node.ref])
            else:
                complete = False
        if coords:
            self.ways[way.id] = {
                "id": way.id,
                "tags": {k: t[k] for k in ("name", "ref", "highway", "railway", "route") if k in t},
                "geometry": [[lon, lat] for lon, lat in coords],
                "complete": complete and len(coords) >= 2,
            }

class RelationHandler(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.routes = []
        self.route_masters = []

    def relation(self, relation):
        t = tags(relation)
        route = t.get("route")
        if route in ROUTE_TYPES:
            self.routes.append(self.relation_record(relation, t, route))
        elif t.get("type") == "route_master":
            self.route_masters.append(self.relation_record(relation, t, "route_master"))

    def relation_record(self, relation, t, route):
        return {
            "id": relation.id,
            "route": route,
            "name": t.get("name"),
            "ref": t.get("ref"),
            "operator": t.get("operator"),
            "from": t.get("from"),
            "to": t.get("to"),
            "direction": t.get("direction"),
            "members": [
                {"type": m.type, "ref": m.ref, "role": m.role}
                for m in relation.members
                if m.type in {"w", "n", "r"}
            ],
        }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pbf", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not args.pbf.is_file() or args.pbf.stat().st_size == 0:
        raise SystemExit(f"PBF is missing or empty: {args.pbf}")

    started = time.monotonic()
    print(f"[1/3] scanning nodes in {args.pbf} ({args.pbf.stat().st_size:,} bytes)", flush=True)
    nodes = NodeHandler()
    nodes.apply_file(str(args.pbf), locations=True)
    print(f"[1/3] retained {len(nodes.nodes):,} NCR nodes and {len(nodes.stop_nodes):,} stops ({time.monotonic() - started:.1f}s)", flush=True)
    print("[2/3] resolving way member coordinates", flush=True)
    ways_handler = WayHandler(nodes.nodes)
    ways_handler.apply_file(str(args.pbf), locations=False)
    print(f"[2/3] retained {len(ways_handler.ways):,} ways ({time.monotonic() - started:.1f}s)", flush=True)
    print("[3/3] reading transit relations", flush=True)
    relations = RelationHandler()
    relations.apply_file(str(args.pbf), locations=False)
    print(f"[3/3] found {len(relations.routes):,} transit relations ({time.monotonic() - started:.1f}s)", flush=True)

    routes = []
    counts = Counter()
    complete = 0
    incomplete = 0
    for route in relations.routes:
        ways = [ways_handler.ways[m["ref"]] for m in route["members"] if m["type"] == "w" and m["ref"] in ways_handler.ways]
        geometry = [point for way in ways for point in way["geometry"]]
        is_complete = bool(ways) and len(ways) == sum(1 for m in route["members"] if m["type"] == "w") and all(w["complete"] for w in ways)
        route = {**route, "ways": ways, "geometry": geometry, "completeGeometry": is_complete}
        routes.append(route)
        counts[route["route"]] += 1
        if is_complete:
            complete += 1
        else:
            incomplete += 1

    output = {
        "source": "Geofabrik Northern Zone latest OSM PBF",
        "sourceUrl": "https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf",
        "attribution": "© OpenStreetMap contributors",
        "bbox": {"minLongitude": BBOX[0], "minLatitude": BBOX[1], "maxLongitude": BBOX[2], "maxLatitude": BBOX[3]},
        "routes": routes,
        "routeMasters": relations.route_masters,
        "stops": list(nodes.stop_nodes.values()),
        "coverage": {
            "routeRelations": len(routes),
            "busRoutes": counts["bus"],
            "metroRoutes": counts["subway"] + counts["light_rail"] + counts["monorail"],
            "trainRoutes": counts["train"],
            "rapidMetroRoutes": sum(1 for r in routes if "rapid" in (r.get("name") or "").lower()),
            "stopCount": len(nodes.stop_nodes),
            "routesWithCompleteGeometry": complete,
            "routesWithIncompleteGeometry": incomplete,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_name(args.output.name + ".tmp")
    temporary.write_text(json.dumps(output, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    os.replace(temporary, args.output)
    print(json.dumps(output["coverage"], indent=2))
    print(f"completed in {time.monotonic() - started:.1f}s; wrote {args.output}", flush=True)


if __name__ == "__main__":
    main()
