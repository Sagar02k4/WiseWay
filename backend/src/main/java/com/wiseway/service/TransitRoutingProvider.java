package com.wiseway.service;

import com.wiseway.domain.TransportMode;
import java.time.Instant;
import java.util.*;

/** Optional seam for a validated GTFS/OTP deployment; no transit feed is bundled by WiseWay. */
public interface TransitRoutingProvider {
  record Leg(TransportMode mode, String routeName, String fromStop, String toStop, int durationSeconds, String geometryGeoJson) {}
  record Itinerary(List<Leg> legs, String source, Instant feedTimestamp) {}
  Optional<Itinerary> route(LocationService.Location origin, LocationService.Location destination, Set<TransportMode> modes);
  default List<Itinerary> journeys(LocationService.Location origin, LocationService.Location destination, Set<TransportMode> modes) {
    return route(origin, destination, modes).stream().toList();
  }
}
