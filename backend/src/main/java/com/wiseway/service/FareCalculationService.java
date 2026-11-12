package com.wiseway.service;

import com.wiseway.domain.TransportMode;
import java.math.*;
import org.springframework.stereotype.Service;

@Service
public class FareCalculationService {
  public record Fare(BigDecimal amount, String type, String source) {}
  public Fare calculate(TransportMode mode, int metres) {
    var km = BigDecimal.valueOf(metres).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    return switch (mode) {
      case AUTO -> new Fare(round(BigDecimal.valueOf(30).add(km.multiply(BigDecimal.valueOf(18))).max(BigDecimal.valueOf(50))), "CALCULATED_ESTIMATE", "WiseWay demo auto formula: ₹30 base + ₹18/km; minimum ₹50.");
      case CAB -> new Fare(round(BigDecimal.valueOf(60).add(km.multiply(BigDecimal.valueOf(22))).add(BigDecimal.valueOf(2).multiply(km))), "CALCULATED_ESTIMATE", "WiseWay demo cab formula: ₹60 base + ₹22/km + ₹2/km time allowance.");
      case BUS -> new Fare(round(BigDecimal.valueOf(12).add(km.multiply(BigDecimal.valueOf(2)))), "ILLUSTRATIVE_ESTIMATE", "Illustrative bus estimate; not a published fare table.");
      case METRO -> new Fare(round(BigDecimal.valueOf(18).add(km.multiply(BigDecimal.valueOf(3)))), "ILLUSTRATIVE_ESTIMATE", "Illustrative metro estimate; no verified fare table is configured.");
      case TRAIN -> new Fare(round(BigDecimal.valueOf(25).add(km.multiply(BigDecimal.valueOf(2.5)))), "ILLUSTRATIVE_ESTIMATE", "Illustrative train estimate; no verified fare table is configured.");
      case WALK, OTHER -> new Fare(null, "UNAVAILABLE", "No fare model is configured for this mode.");
    };
  }
  private BigDecimal round(BigDecimal value) { return value.setScale(0, RoundingMode.HALF_UP); }
}
