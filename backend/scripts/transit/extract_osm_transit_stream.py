"""Memory-bounded offline transit extraction from an OSM PBF.

Three streaming passes are used: route relations, their member ways/node IDs,
then coordinates for only those node IDs. No Overpass or live service is used.
"""
from __future__ import annotations

import argparse
import json
import os
import time
from collections import Counter
from pathlib import Path

import osmium

BBOX = (76.2, 27.7, 78.0, 29.4)
ROUTE_TYPES = {"bus", "subway", "train", "light_rail", "monorail"}
STOP_KEYS = {"station", "stop", "subway_entrance", "halt"}


def inside(lon, lat):
    return BBOX[0] <= lon <= BBOX[2] and BBOX[1] <= lat <= BBOX[3]


def tags(obj):
    return {str(k): str(v) for k, v in obj.tags}


def record_relation(rel, t, route):
    return {
        "id": rel.id,
        "route": route,
        "name": t.get("name"),
        "ref": t.get("ref"),
        "operator": t.get("operator"),
        "from": t.get("from"),
        "to": t.get("to"),
        "direction": t.get("direction"),
        "members": [{"type": m.type, "ref": m.ref, "role": m.role} for m in rel.members if m.type in {"w", "n", "r"}],
    }


class RelationPass(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.routes = []
        self.route_masters = []
        self.seen = 0

    def relation(self, rel):
        self.seen += 1
        t = tags(rel)
        if t.get("route") in ROUTE_TYPES:
            self.routes.append(record_relation(rel, t, t["route"]))
        elif t.get("type") == "route_master":
            self.route_masters.append(record_relation(rel, t, "route_master"))


class WayPass(osmium.SimpleHandler):
    def __init__(self, way_ids):
        super().__init__()
        self.way_ids = way_ids
        self.ways = {}
        self.node_ids = set()
        self.seen = 0

    def way(self, way):
        self.seen += 1
        if way.id not in self.way_ids:
            return
        refs = [n.ref for n in way.nodes]
        self.node_ids.update(refs)
        t = tags(way)
        self.ways[way.id] = {
            "id": way.id,
            "tags": {k: t[k] for k in ("name", "ref", "highway", "railway", "route") if k in t},
            "nodeRefs": refs,
        }


class NodePass(osmium.SimpleHandler):
    def __init__(self, node_ids):
        super().__init__()
        self.node_ids = node_ids
        self.nodes = {}
        self.stops = {}
        self.seen = 0

    def node(self, node):
        self.seen += 1
        if node.id not in self.node_ids or not node.location.valid():
            return
        lon, lat = node.location.lon, node.location.lat
        self.nodes[node.id] = (lon, lat)
        t = tags(node)
        if (t.get("public_transport") in {"stop_position", "platform", "stop_area"}
                or t.get("highway") == "bus_stop" or t.get("railway") in STOP_KEYS):
            self.stops[node.id] = {
                "id": node.id, "name": t.get("name"), "ref": t.get("ref"),
                "operator": t.get("operator"), "publicTransport": t.get("public_transport"),
                "railway": t.get("railway"), "highway": t.get("highway"),
                "longitude": lon, "latitude": lat,
            }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("pbf", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not args.pbf.is_file() or args.pbf.stat().st_size == 0:
        raise SystemExit(f"PBF is missing or empty: {args.pbf}")
    started = time.monotonic()

    print("[1/3] scanning route relations", flush=True)
    relation_pass = RelationPass()
    relation_pass.apply_file(str(args.pbf), locations=False)
    print(f"[1/3] {len(relation_pass.routes)} transit relations, {relation_pass.seen} relations scanned ({time.monotonic()-started:.1f}s)", flush=True)

    way_ids = {m["ref"] for r in relation_pass.routes for m in r["members"] if m["type"] == "w"}
    print(f"[2/3] scanning {len(way_ids)} referenced ways", flush=True)
    way_pass = WayPass(way_ids)
    way_pass.apply_file(str(args.pbf), locations=False)
    print(f"[2/3] retained {len(way_pass.ways)} ways and {len(way_pass.node_ids)} node IDs ({time.monotonic()-started:.1f}s)", flush=True)

    print("[3/3] resolving referenced node coordinates", flush=True)
    node_pass = NodePass(way_pass.node_ids)
    node_pass.apply_file(str(args.pbf), locations=False)
    print(f"[3/3] resolved {len(node_pass.nodes)} coordinates and {len(node_pass.stops)} stops ({time.monotonic()-started:.1f}s)", flush=True)

    routes, counts = [], Counter()
    complete = incomplete = 0
    for route in relation_pass.routes:
        ways = []
        route_nodes = []
        all_way_members = [m for m in route["members"] if m["type"] == "w"]
        for member in all_way_members:
            way = way_pass.ways.get(member["ref"])
            if not way:
                continue
            geometry = [[node_pass.nodes[n][0], node_pass.nodes[n][1]] for n in way["nodeRefs"] if n in node_pass.nodes]
            ways.append({"id": way["id"], "role": member["role"], "tags": way["tags"], "geometry": geometry})
            route_nodes.extend(n for n in way["nodeRefs"] if n in node_pass.nodes)
        geometry = [[node_pass.nodes[n][0], node_pass.nodes[n][1]] for n in route_nodes]
        in_bbox = any(inside(lon, lat) for lon, lat in geometry)
        if not in_bbox:
            continue
        complete_geometry = bool(geometry) and len(ways) == len(all_way_members) and all(len(w["geometry"]) >= 2 for w in ways)
        stops = [node_pass.stops[n] for n in dict.fromkeys(route_nodes) if n in node_pass.stops]
        routes.append({**route, "ways": ways, "geometry": geometry, "completeGeometry": complete_geometry, "stops": stops})
        counts[route["route"]] += 1
        complete += int(complete_geometry)
        incomplete += int(not complete_geometry)

    output = {
        "source": "Geofabrik Northern Zone latest OSM PBF",
        "sourceUrl": "https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf",
        "attribution": "© OpenStreetMap contributors",
        "extractionDate": time.strftime("%Y-%m-%d", time.gmtime()),
        "bbox": {"minLongitude": BBOX[0], "minLatitude": BBOX[1], "maxLongitude": BBOX[2], "maxLatitude": BBOX[3]},
        "routes": routes,
        "routeMasters": relation_pass.route_masters,
        "stops": list(node_pass.stops.values()),
        "coverage": {
            "routeRelations": len(routes), "busRoutes": counts["bus"],
            "metroRoutes": counts["subway"] + counts["light_rail"] + counts["monorail"],
            "trainRoutes": counts["train"], "rapidMetroRoutes": sum(1 for r in routes if "rapid" in (r.get("name") or "").lower()),
            "stopCount": len(node_pass.stops), "routesWithCompleteGeometry": complete, "routesWithIncompleteGeometry": incomplete,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.output.with_name(args.output.name + ".tmp")
    tmp.write_text(json.dumps(output, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    os.replace(tmp, args.output)
    report = args.output.with_name("coverage-report.md")
    c = output["coverage"]
    report_tmp = report.with_name(report.name + ".tmp")
    report_tmp.write_text("# NCR transit coverage\n\n" + "\n".join(f"- **{k}:** {v}" for k, v in c.items()) + "\n\nSource: © OpenStreetMap contributors; Geofabrik Northern Zone extract. Counts are mapped OSM relations, not live service or schedules.\n", encoding="utf-8")
    os.replace(report_tmp, report)
    print(json.dumps(c, indent=2), flush=True)
    print(f"completed in {time.monotonic()-started:.1f}s", flush=True)


if __name__ == "__main__":
    main()
