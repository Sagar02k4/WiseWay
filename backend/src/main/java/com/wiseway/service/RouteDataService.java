package com.wiseway.service;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RouteDataService {
  public record RouteProfile(int roadDistanceMetres, int roadDurationSeconds, Set<String> verifiedTransitModes) {}
  private final Map<String, RouteProfile> routes = Map.of(
      key("sector-14", "main-campus"), new RouteProfile(7200, 1500, Set.of("BUS", "METRO")),
      key("old-town", "main-campus"), new RouteProfile(5100, 1200, Set.of("BUS")),
      key("sector-14", "airport"), new RouteProfile(18500, 3000, Set.of("BUS", "TRAIN")));

  public Optional<RouteProfile> find(String from, String to) {
    return Optional.ofNullable(routes.get(key(from, to))).or(() -> Optional.ofNullable(routes.get(key(to, from))));
  }
  private static String key(String a, String b) { return a + "->" + b; }
}
