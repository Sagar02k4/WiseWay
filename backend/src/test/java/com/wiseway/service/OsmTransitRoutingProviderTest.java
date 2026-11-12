package com.wiseway.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
