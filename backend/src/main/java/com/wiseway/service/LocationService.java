package com.wiseway.service;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
  public record Location(String id, String name, String context, double latitude, double longitude) {}
  private final List<Location> locations = List.of(
      new Location("sector-14", "Sector 14", "Gurugram, Haryana", 28.4595, 77.0266),
      new Location("main-campus", "Main Campus", "Gurugram, Haryana", 28.4456, 77.0420),
      new Location("old-town", "Old Town", "Gurugram, Haryana", 28.4646, 77.0299),
      new Location("airport", "Airport", "New Delhi", 28.5562, 77.1000));

  public List<Location> search(String query) {
    if (query == null || query.isBlank()) return List.of();
    var q = query.trim().toLowerCase(Locale.ROOT);
    return locations.stream().filter(x -> (x.name()+" "+x.context()).toLowerCase(Locale.ROOT).contains(q)).toList();
  }

  public Optional<Location> resolve(String value) {
    return search(value).stream().filter(x -> x.name().equalsIgnoreCase(value.trim())).findFirst();
  }
}
