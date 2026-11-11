package com.wiseway.domain;
import org.junit.jupiter.api.Test; import java.math.*; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
class RecommendationEngineTest { private final List<TransportOption> options=List.of(new TransportOption("slow-cheap",TransportMode.BUS,45,new BigDecimal("20"),"INR",1,8,""),new TransportOption("fast-costly",TransportMode.CAB,20,new BigDecimal("100"),"INR",0,1,""));
 @Test void budgetRanksCheaper(){var r=RecommendationEngine.recommend(options,PreferenceProfile.BUDGET_SAVER,new RecommendationEngine.Constraints(null,null,null),Set.of());assertEquals("slow-cheap",r.feasible().get(0).option().id());}
 @Test void fareLimitFiltersBeforeScoring(){var r=RecommendationEngine.recommend(options,PreferenceProfile.TIME_SAVER,new RecommendationEngine.Constraints(new BigDecimal("50"),null,null),Set.of());assertEquals(1,r.feasible().size());assertEquals("fast-costly",r.infeasible().get(0).option().id());}
 @Test void noFitIsExplicit(){var r=RecommendationEngine.recommend(options,PreferenceProfile.BALANCED,new RecommendationEngine.Constraints(new BigDecimal("10"),null,null),Set.of());assertTrue(r.noFit());assertTrue(r.feasible().isEmpty());}
 @Test void exactFareLimitIsFeasible(){var r=RecommendationEngine.recommend(options,PreferenceProfile.TIME_SAVER,new RecommendationEngine.Constraints(new BigDecimal("100"),null,null),Set.of());assertEquals(2,r.feasible().size());}
 @Test void exclusionIsApplied(){var r=RecommendationEngine.recommend(options,PreferenceProfile.TIME_SAVER,new RecommendationEngine.Constraints(null,null,null),Set.of("fast-costly"));assertEquals("slow-cheap",r.feasible().get(0).option().id());}
}
