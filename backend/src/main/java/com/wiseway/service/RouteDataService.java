package com.wiseway.service;

import java.util.*; import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service @ConditionalOnMissingBean(RoutingProvider.class)
public class RouteDataService implements RoutingProvider {
  public record RouteProfile(int roadDistanceMetres, int roadDurationSeconds, Set<String> verifiedTransitModes) {}
  private final Map<String, RouteProfile> routes = Map.of(
      key("sector-14", "main-campus"), new RouteProfile(7200, 1500, Set.of("BUS", "METRO")),
      key("old-town", "main-campus"), new RouteProfile(5100, 1200, Set.of("BUS")),
      key("sector-14", "airport"), new RouteProfile(18500, 3000, Set.of("BUS", "TRAIN")));

  public Optional<RouteProfile> find(String from, String to) {
    return Optional.ofNullable(routes.get(key(from, to))).or(() -> Optional.ofNullable(routes.get(key(to, from))));
  }
  public Optional<Route> route(LocationService.Location a, LocationService.Location b) { return find(a.id(), b.id()).map(x -> new Route(x.roadDistanceMetres(), x.roadDurationSeconds(), "", "WISEWAY_CURATED_ROUTE")); }
  private static String key(String a, String b) { return a + "->" + b; }
}
