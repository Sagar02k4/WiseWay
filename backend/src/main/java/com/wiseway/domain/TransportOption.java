package com.wiseway.domain;

import java.math.BigDecimal;

public record TransportOption(String id, TransportMode mode, int durationMinutes, BigDecimal estimatedFare, String currency, int transfers, int walkingMinutes, String description) {}
