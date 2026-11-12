package com.wiseway.api;
import com.wiseway.domain.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.*;
public final class ApiDtos { private ApiDtos(){}
 public record RouteRequest(@NotBlank String label,@NotBlank String origin,@NotBlank String destination,TransportMode preferredMode,PreferenceProfile preferenceProfile){}
 public record RouteResponse(Long id,String label,String origin,String destination,TransportMode preferredMode,PreferenceProfile preferenceProfile,Instant createdAt,Instant updatedAt){}
 public record ConstraintRequest(@DecimalMin(value="0.0") BigDecimal maxFare,String currency,LocalDateTime arriveBy){}
 public record CompareRequest(@NotBlank String origin,@NotBlank String destination,Double originLatitude,Double originLongitude,Double destinationLatitude,Double destinationLongitude,LocalDateTime departureAt,PreferenceProfile preference,@Valid ConstraintRequest constraints){}
 public record OptionResponse(String id,TransportMode mode,int durationMinutes,BigDecimal estimatedFare,String currency,int transfers,int walkingMinutes,String description,BigDecimal score,List<String> reasons,List<String> infeasibleReasons,String availabilityStatus,String fareType,String dataSource){}
 public record CompareResponse(String dataQuality,String disclaimer,List<OptionResponse> feasibleOptions,List<OptionResponse> infeasibleOptions,boolean noFeasibleOption,String message){}
 public record PlanBRequest(@NotBlank String origin,@NotBlank String destination,Double originLatitude,Double originLongitude,Double destinationLatitude,Double destinationLongitude,LocalDateTime departureAt,PreferenceProfile preference,@NotNull PlanBScenario scenario,List<String> excludeOptionIds,@Valid ConstraintRequest constraints){}
 public record PlanBResponse(PlanBScenario scenario,String dataQuality,String disclaimer,OptionResponse recommendedOption,List<OptionResponse> alternatives,boolean noFeasibleOption,String message){}
 public record ExpenseRequest(@NotNull LocalDate travelDate,@NotBlank String origin,@NotBlank String destination,@NotNull TransportMode mode,@NotNull @DecimalMin(value="0.0") BigDecimal actualAmount,@Pattern(regexp="[A-Z]{3}") String currency,String notes){}
 public record ExpenseResponse(Long id,LocalDate travelDate,String origin,String destination,TransportMode mode,BigDecimal actualAmount,String currency,String notes,Instant createdAt){}
 public record Group(String key,BigDecimal amount,long tripCount){}
 public record Summary(LocalDate from,LocalDate to,String currency,BigDecimal totalActualSpend,long tripCount,List<Group> groups){}
 public record LocationResponse(String id,String name,String context,double latitude,double longitude){}
}
