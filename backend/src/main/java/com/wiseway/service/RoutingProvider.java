package com.wiseway.service;
import java.util.*;
public interface RoutingProvider {
  record Route(double distanceMetres, double durationSeconds, String geometry, String source) {}
  Optional<Route> route(LocationService.Location origin, LocationService.Location destination);
}
