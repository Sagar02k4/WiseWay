package com.wiseway.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiseway.domain.TransportMode;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OsmTransitRoutingProviderTest {
  @Test
  void missingDatasetDegradesToNoTransitWithoutBreakingRoadRouting() {
    var provider = new OsmTransitRoutingProvider(new ObjectMapper(), Path.of("does-not-exist.json").toString());
    var result = provider.route(new LocationService.Location("a", "A", "", 28.6, 77.2), new LocationService.Location("b", "B", "", 28.7, 77.3), Set.of(TransportMode.BUS));
    assertTrue(result.isEmpty());
  }

  @Test
  void findsGurugramMetroWithFirstAndLastMileCandidates() {
    var provider = new OsmTransitRoutingProvider(new ObjectMapper(), "data/transit/normalized/ncr-transit.json");
    var result = provider.journeys(new LocationService.Location("o", "Sector 17", "", 28.4758035, 77.0607773),
        new LocationService.Location("d", "Sector 51", "", 28.4287011, 77.0666877), Set.of(TransportMode.METRO));
    assertFalse(result.isEmpty());
    assertTrue(result.get(0).legs().stream().anyMatch(x -> x.mode() == TransportMode.WALK));
    assertTrue(result.get(0).legs().stream().anyMatch(x -> x.mode() == TransportMode.METRO));
  }
}
