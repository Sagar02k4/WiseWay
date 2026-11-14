package com.wiseway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiseway.domain.TransportMode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Offline OSM relation matcher. It combines mapped transit relations with evidence-based stop walking links.
 * Schedules are deliberately not inferred; all transit times remain estimates. */
@Service
public class OsmTransitRoutingProvider implements TransitRoutingProvider {
  private static final double STOP_RADIUS_METRES = 1_500;
  private final List<Route> routes;
  private final Map<Long, List<Edge>> adjacency;

  public OsmTransitRoutingProvider(ObjectMapper mapper,
      @Value("${wiseway.transit.dataset:data/transit/normalized/ncr-transit.json}") String dataset) {
    this.routes = loadSafely(mapper, Path.of(dataset));
    this.adjacency = buildGraph(routes);
  }

  @Override
  public Optional<Itinerary> route(LocationService.Location origin, LocationService.Location destination, Set<TransportMode> modes) {
    return journeys(origin, destination, modes).stream().findFirst();
  }

  @Override
  public List<Itinerary> journeys(LocationService.Location origin, LocationService.Location destination, Set<TransportMode> modes) {
    var starts = nearestStops(origin, modes); var ends = nearestStops(destination, modes);
    if (starts.isEmpty() || ends.isEmpty()) return List.of();
    var results = new ArrayList<Itinerary>();
    for (var s : starts) for (var e : ends) for (var found : search(s.stop.id, e.stop.id, modes)) {
      var legs = new ArrayList<Leg>();
      if (s.distance() > 25) legs.add(walk(origin, s.stop(), s.distance()));
      legs.addAll(found.legs());
      if (e.distance() > 25) legs.add(walk(e.stop(), destination, e.distance()));
      results.add(new Itinerary(List.copyOf(legs), "OSM_MAPPED_NETWORK_WITH_WALK_CONNECTORS", Instant.now()));
    }
    return results.stream().filter(x -> !x.legs().isEmpty()).distinct().limit(12).toList();
  }

  public List<LocationService.Location> searchStops(String query) {
    if (query == null || query.isBlank()) return List.of();
    var q = query.trim().toLowerCase(Locale.ROOT);
    return routes.stream().flatMap(r -> r.stops.stream()).filter(s -> s.name.toLowerCase(Locale.ROOT).contains(q))
        .map(s -> new LocationService.Location("osm-stop-" + s.id, s.name, "OSM transit stop · " + s.mode, s.lat, s.lon))
        .distinct().sorted(Comparator.comparing(LocationService.Location::name).thenComparing(LocationService.Location::id)).limit(12).toList();
  }

  private Optional<Itinerary> direct(Route route, LocationService.Location origin, LocationService.Location destination) {
    var from = nearest(route.stops, origin.latitude(), origin.longitude());
    var to = nearest(route.stops, destination.latitude(), destination.longitude());
    if (from.isEmpty() || to.isEmpty() || from.get().index() >= to.get().index()) return Optional.empty();
    var geometry = route.geometryBetween(from.get().stop(), to.get().stop());
    if (geometry.isEmpty()) return Optional.empty();
    return Optional.of(new Itinerary(List.of(new Leg(route.mode, route.name, from.get().name(), to.get().name(), 0, geometry)), "OSM_RELATIONS_NO_SCHEDULE", Instant.now()));
  }

  private Optional<StopMatch> nearest(List<Stop> stops, double lat, double lon) {
    return stops.stream().map(s -> new StopMatch(s, haversine(lat, lon, s.lat, s.lon))).filter(x -> x.distance <= STOP_RADIUS_METRES).min(Comparator.comparingDouble(x -> x.distance));
  }

  private List<StopMatch> nearestStops(LocationService.Location location, Set<TransportMode> modes) {
    var candidates = routes.stream().filter(r -> modes.contains(r.mode)).flatMap(r -> r.stops.stream())
        .collect(java.util.stream.Collectors.toMap(Stop::id, s -> s, (a,b) -> a, LinkedHashMap::new)).values().stream()
        .map(s -> new StopMatch(s, haversine(location.latitude(), location.longitude(), s.lat, s.lon)))
        .filter(x -> x.distance <= STOP_RADIUS_METRES).sorted(Comparator.comparingDouble(x -> x.distance)).toList();
    if (candidates.isEmpty()) return List.of();
    var cutoff = Math.min(STOP_RADIUS_METRES, candidates.get(0).distance + 100);
    return candidates.stream().filter(x -> x.distance <= cutoff).toList();
  }

  private List<Itinerary> search(long start, long target, Set<TransportMode> modes) {
    record State(long stop, List<Edge> path, Set<Long> visited, Set<Long> routeIds) {}
    var queue = new ArrayDeque<State>(); queue.add(new State(start, List.of(), Set.of(start), Set.of()));
    var out = new ArrayList<Itinerary>();
    while (!queue.isEmpty() && out.size() < 4) {
      var state = queue.removeFirst();
      if (state.stop == target && !state.path.isEmpty()) { out.add(toItinerary(state.path)); continue; }
      if (state.path.size() >= 24 || state.routeIds.size() > 3) continue;
      for (var edge : adjacency.getOrDefault(state.stop, List.of())) {
        if (!modes.contains(edge.mode) || state.visited.contains(edge.toId)) continue;
        var visited = new HashSet<>(state.visited); visited.add(edge.toId);
        var routesUsed = new HashSet<>(state.routeIds); routesUsed.add(edge.relationId);
        queue.addLast(new State(edge.toId, append(state.path, edge), visited, routesUsed));
      }
    }
    return out.stream().distinct().toList();
  }

  private Leg walk(LocationService.Location from, Stop to, double metres) { return new Leg(TransportMode.WALK, "Approximate walking connection", from.name(), to.name, walkingSeconds(metres), geo(List.of(List.of(from.longitude(),from.latitude()),List.of(to.lon,to.lat)))); }
  private Leg walk(Stop from, LocationService.Location to, double metres) { return new Leg(TransportMode.WALK, "Approximate walking connection", from.name, to.name(), walkingSeconds(metres), geo(List.of(List.of(from.lon,from.lat),List.of(to.longitude(),to.latitude())))); }
  private int walkingSeconds(double metres) { return (int)Math.max(30, Math.round(metres / 1.25)); }

  private List<Edge> append(List<Edge> path, Edge edge) { var copy = new ArrayList<>(path); copy.add(edge); return List.copyOf(copy); }

  private Itinerary toItinerary(List<Edge> edges) {
    var legs = new ArrayList<Leg>(); Edge first = edges.get(0); var mode = first.mode; var name = first.routeName; var relationId=first.relationId; var from = first.from; var to = first.to; var geometries = new ArrayList<List<Double>>(); geometries.addAll(first.coordinates);
    for (int i=1;i<edges.size();i++) { var e=edges.get(i); if(e.mode==mode && e.relationId==relationId) { to=e.to; geometries.addAll(e.coordinates.subList(Math.min(1,e.coordinates.size()),e.coordinates.size())); } else { legs.add(new Leg(mode,name,from.name,to.name,estimateSeconds(geometries),geo(geometries))); mode=e.mode; name=e.routeName; relationId=e.relationId; from=e.from; geometries=new ArrayList<>(e.coordinates); to=e.to; } }
    legs.add(new Leg(mode,name,from.name,to.name,estimateSeconds(geometries),geo(geometries)));
    return new Itinerary(List.copyOf(legs), "OSM_MAPPED_NETWORK_NO_SCHEDULE", Instant.now());
  }
  private int estimateSeconds(List<List<Double>> points) { double metres=0; for(int i=1;i<points.size();i++) metres+=haversine(points.get(i-1).get(1),points.get(i-1).get(0),points.get(i).get(1),points.get(i).get(0)); return (int)Math.max(60, Math.round(metres/8.0)); }
  private String geo(List<List<Double>> points) { try{return new ObjectMapper().writeValueAsString(Map.of("type","LineString","coordinates",points));}catch(IOException e){return "";} }

  private Map<Long,List<Edge>> buildGraph(List<Route> input) {
    var graph=new HashMap<Long,List<Edge>>();
    var stops=new LinkedHashMap<Long,Stop>();
    for(var r:input) for(var s:r.stops) stops.putIfAbsent(s.id,s);
    for (var r:input) for(int i=0;i<r.stops.size()-1;i++) { var a=r.stops.get(i); var b=r.stops.get(i+1); var coords=r.segment(a,b); if(coords.size()<2) continue; graph.computeIfAbsent(a.id,k->new ArrayList<>()).add(new Edge(a.id,b.id,r.mode,r.name,r.relationId,a,b,coords)); }
    // Shared physical stops and nearby stations are valid transfer evidence. The 900m cap
    // prevents arbitrary network jumps while allowing bus-to-metro interchanges.
    var all=new ArrayList<>(stops.values());
    for(var a:all) for(var b:all) if(a.id!=b.id && !a.mode.equals(b.mode)) { var d=haversine(a.lat,a.lon,b.lat,b.lon); if(d<=900) graph.computeIfAbsent(a.id,k->new ArrayList<>()).add(new Edge(a.id,b.id,TransportMode.WALK,"Validated station transfer",0,a,b,List.of())); }
    return graph;
  }

  private static double haversine(double aLat, double aLon, double bLat, double bLon) {
    var p = Math.PI / 180; var a = .5 - Math.cos((bLat-aLat)*p)/2 + Math.cos(aLat*p)*Math.cos(bLat*p)*(1-Math.cos((bLon-aLon)*p))/2;
    return 12_742_000 * Math.asin(Math.sqrt(a));
  }

  private static List<Route> load(ObjectMapper mapper, Path path) {
    try {
      var root = mapper.readTree(path.toFile()); var result = new ArrayList<Route>();
      for (var n : root.path("routes")) {
        if (!n.path("completeGeometry").asBoolean(false)) continue;
        var mode = switch (n.path("route").asText()) { case "bus" -> TransportMode.BUS; case "subway", "light_rail", "monorail" -> TransportMode.METRO; case "train" -> TransportMode.TRAIN; default -> null; };
        if (mode == null) continue;
        var stops = new ArrayList<Stop>(); for (var s : n.path("stops")) stops.add(new Stop(s.path("id").asLong(), s.path("name").asText("Unnamed stop"), s.path("latitude").asDouble(), s.path("longitude").asDouble(), mode.name(), -1));
        if (stops.size() < 2) continue;
        // The extractor preserves stop order in the route's ordered member-node sequence.
        for (int i=0;i<stops.size();i++) stops.set(i, new Stop(stops.get(i).id, stops.get(i).name, stops.get(i).lat, stops.get(i).lon, stops.get(i).mode, i));
        var coordinates = new ArrayList<List<Double>>(); n.path("geometry").forEach(p -> coordinates.add(List.of(p.get(0).asDouble(), p.get(1).asDouble())));
        result.add(new Route(n.path("id").asLong(), mode, n.path("name").asText("Unnamed mapped route"), stops, coordinates));
      }
      return List.copyOf(result);
    } catch (IOException e) { throw new IllegalStateException("Transit dataset could not be loaded: " + path, e); }
  }

  private static List<Route> loadSafely(ObjectMapper mapper, Path path) {
    try {
      return load(mapper, path);
    } catch (RuntimeException ex) {
      // Transit data is optional. Road routing and the rest of the API remain usable.
      return List.of();
    }
  }

  private record Stop(long id, String name, double lat, double lon, String mode, int index) {}
  private record StopMatch(Stop stop, double distance) { int index(){ return stop.index; } String name(){ return stop.name; } }
  private record Route(long relationId, TransportMode mode, String name, List<Stop> stops, List<List<Double>> coordinates) {
    String geometryBetween(Stop from, Stop to) {
      int start = nearestCoordinate(from), end = nearestCoordinate(to);
      if (start < 0 || end < 0 || start >= end) return "";
      var segment = coordinates.subList(start, end + 1);
      if (segment.size() < 2) return "";
      try { return new ObjectMapper().writeValueAsString(Map.of("type", "LineString", "coordinates", segment)); }
      catch (IOException e) { return ""; }
    }
    private int nearestCoordinate(Stop stop) {
      var best = -1; var distance = Double.MAX_VALUE;
      for (int i=0;i<coordinates.size();i++) { var p=coordinates.get(i); var d=haversine(stop.lat,stop.lon,p.get(1),p.get(0)); if(d<distance){distance=d;best=i;} }
      return distance <= STOP_RADIUS_METRES ? best : -1;
    }
    List<List<Double>> segment(Stop from, Stop to) { int a=nearestCoordinate(from), b=nearestCoordinate(to); if(a<0||b<=a)return List.of(); return List.copyOf(coordinates.subList(a,b+1)); }
  }
  private record Edge(long fromId,long toId,TransportMode mode,String routeName,long relationId,Stop from,Stop to,List<List<Double>> coordinates) {}
}
