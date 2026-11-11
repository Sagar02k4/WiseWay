package com.wiseway.persistence;

import com.wiseway.domain.TransportMode;
import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.*;

@Entity @Table(name="journey_logs")
public class JourneyLogEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private LocalDate travelDate; @Column(nullable=false) private String origin; @Column(nullable=false) private String destination;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private TransportMode mode; @Column(nullable=false,precision=12,scale=2) private BigDecimal actualAmount; @Column(nullable=false,length=3) private String currency; @Column(length=500) private String notes; @Column(nullable=false) private Instant createdAt=Instant.now();
  protected JourneyLogEntity() {}
  public JourneyLogEntity(LocalDate date,String o,String d,TransportMode m,BigDecimal amount,String currency,String notes){travelDate=date;origin=o;destination=d;mode=m;actualAmount=amount;this.currency=currency;this.notes=notes;}
  public Long getId(){return id;} public LocalDate getTravelDate(){return travelDate;} public String getOrigin(){return origin;} public String getDestination(){return destination;} public TransportMode getMode(){return mode;} public BigDecimal getActualAmount(){return actualAmount;} public String getCurrency(){return currency;} public String getNotes(){return notes;} public Instant getCreatedAt(){return createdAt;}
}
