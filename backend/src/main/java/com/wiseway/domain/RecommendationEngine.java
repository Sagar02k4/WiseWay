package com.wiseway.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToDoubleFunction;

public final class RecommendationEngine {
  private RecommendationEngine() {}
  public record Constraints(BigDecimal maxFare, LocalDateTime departureAt, LocalDateTime arriveBy) {}
  public record RankedOption(TransportOption option, BigDecimal score, List<String> reasons) {}
  public record InfeasibleOption(TransportOption option, List<String> reasons) {}
  public record Result(List<RankedOption> feasible, List<InfeasibleOption> infeasible, boolean noFit, String message) {}

  public static Result recommend(List<TransportOption> source, PreferenceProfile profile, Constraints constraints, Set<String> excludedIds) {
    var feasible = new ArrayList<TransportOption>(); var infeasible = new ArrayList<InfeasibleOption>();
    for (var option : source) {
      var reasons = new ArrayList<String>();
      if (excludedIds.contains(option.id())) reasons.add("Unavailable for this simulated scenario.");
      if (constraints.maxFare() != null && option.estimatedFare().compareTo(constraints.maxFare()) > 0) reasons.add("Estimated fare exceeds your maximum by INR " + option.estimatedFare().subtract(constraints.maxFare()).stripTrailingZeros().toPlainString() + ".");
      if (constraints.departureAt() != null && constraints.arriveBy() != null && constraints.departureAt().plusMinutes(option.durationMinutes()).isAfter(constraints.arriveBy())) reasons.add("Estimated arrival is after your deadline.");
      if (reasons.isEmpty()) feasible.add(option); else infeasible.add(new InfeasibleOption(option, reasons));
    }
    var weights = weights(profile); var scored = feasible.stream().map(o -> new RankedOption(o, score(o, feasible, weights), reasons(o, feasible, profile, constraints))).sorted(Comparator.comparing(RankedOption::score).reversed().thenComparingInt(r -> r.option().durationMinutes()).thenComparing(r -> r.option().estimatedFare()).thenComparing(r -> r.option().id())).toList();
    return new Result(scored, infeasible, scored.isEmpty(), scored.isEmpty() ? "No option meets all your limits. Review the alternatives below." : null);
  }
  private static double[] weights(PreferenceProfile p) { return switch (p) { case BUDGET_SAVER -> new double[]{.70,.15,.10,.05}; case TIME_SAVER -> new double[]{.10,.70,.10,.10}; case COMFORT_FIRST -> new double[]{.10,.10,.45,.35}; case BALANCED -> new double[]{.30,.35,.20,.15}; }; }
  private static BigDecimal score(TransportOption o, List<TransportOption> all, double[] w) {
    double value = w[0]*normal(o.estimatedFare().doubleValue(), all, x -> x.estimatedFare().doubleValue()) + w[1]*normal(o.durationMinutes(), all, x -> x.durationMinutes()) + w[2]*normal(o.transfers(), all, x -> x.transfers()) + w[3]*normal(o.walkingMinutes(), all, x -> x.walkingMinutes());
    return BigDecimal.valueOf(value * 100).setScale(1, RoundingMode.HALF_UP);
  }
  private static double normal(double value, List<TransportOption> all, ToDoubleFunction<TransportOption> f) { double min=all.stream().mapToDouble(f).min().orElse(value), max=all.stream().mapToDouble(f).max().orElse(value); return max == min ? 1 : (max-value)/(max-min); }
  private static List<String> reasons(TransportOption o, List<TransportOption> all, PreferenceProfile p, Constraints c) {
    var out = new ArrayList<String>(); var minFare=all.stream().map(TransportOption::estimatedFare).min(BigDecimal::compareTo).orElse(o.estimatedFare()); var minDuration=all.stream().mapToInt(TransportOption::durationMinutes).min().orElse(o.durationMinutes());
    if (o.estimatedFare().compareTo(minFare)==0) out.add("Lowest estimated fare among feasible options.");
    if (o.durationMinutes()==minDuration) out.add("Fastest estimated journey among feasible options.");
    if (c.maxFare()!=null) out.add("Within your INR " + c.maxFare().stripTrailingZeros().toPlainString() + " fare limit.");
    if (out.isEmpty()) out.add("Strongest " + p.name().toLowerCase(Locale.ROOT).replace('_',' ') + " trade-off across the available estimates.");
    return List.copyOf(out);
  }
}
