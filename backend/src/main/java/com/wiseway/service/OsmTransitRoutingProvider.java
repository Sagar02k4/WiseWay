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

/** Offline OSM relation matcher. It exposes only mapped stop-to-stop journeys; it is not a schedule/feed provider. */
@Service
public class OsmTransitRoutingProvider implements TransitRoutingProvider {
  private static final double STOP_RADIUS_METRES = 1_500;
  private final List<Route> routes;

  public OsmTransitRoutingProvider(ObjectMapper mapper,
      @Value("${wiseway.transit.dataset:data/transit/normalized/ncr-transit.json}") String dataset) {
    this.routes = loadSafely(mapper, Path.of(dataset));
  }

  @Override
  public Optional<Itinerary> route(LocationService.Location origin, LocationService.Location destination, Set<TransportMode> modes) {
    var direct = routes.stream().filter(r -> modes.contains(r.mode)).map(r -> direct(r, origin, destination)).flatMap(Optional::stream).findFirst();
    if (direct.isPresent()) return direct;
    return Optional.empty(); // Crossing lines is not treated as a transfer without a shared mapped stop.
  }

  private Optional<Itinerary> direct(Route route, LocationService.Location origin, LocationService.Location destination) {
    var from = nearest(route.stops, origin.latitude(), origin.longitude());
    var to = nearest(route.stops, destination.latitude(), destination.longitude());
    if (from.isEmpty() || to.isEmpty() || from.get().index() >= to.get().index()) return Optional.empty();
    var geometry = route.geometryBetween(from.get().index(), to.get().index());
    if (geometry.isEmpty()) return Optional.empty();
    return Optional.of(new Itinerary(List.of(new Leg(route.mode, route.name, from.get().name(), to.get().name(), 0, geometry)), "OSM_RELATIONS_NO_SCHEDULE", Instant.now()));
  }

  private Optional<StopMatch> nearest(List<Stop> stops, double lat, double lon) {
    return stops.stream().map(s -> new StopMatch(s, haversine(lat, lon, s.lat, s.lon))).filter(x -> x.distance <= STOP_RADIUS_METRES).min(Comparator.comparingDouble(x -> x.distance));
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
        var stops = new ArrayList<Stop>(); for (var s : n.path("stops")) stops.add(new Stop(s.path("name").asText("Unnamed stop"), s.path("latitude").asDouble(), s.path("longitude").asDouble(), -1));
        if (stops.size() < 2) continue;
        // The extractor preserves stop order in the route's ordered member-node sequence.
        for (int i=0;i<stops.size();i++) stops.set(i, new Stop(stops.get(i).name, stops.get(i).lat, stops.get(i).lon, i));
        var geometry = mapper.writeValueAsString(Map.of("type", "LineString", "coordinates", n.path("geometry")));
        result.add(new Route(mode, n.path("name").asText("Unnamed mapped route"), stops, geometry));
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

  private record Stop(String name, double lat, double lon, int index) {}
  private record StopMatch(Stop stop, double distance) { int index(){ return stop.index; } String name(){ return stop.name; } }
  private record Route(TransportMode mode, String name, List<Stop> stops, String geometry) {
    String geometryBetween(int from, int to) { return geometry; }
  }
}
