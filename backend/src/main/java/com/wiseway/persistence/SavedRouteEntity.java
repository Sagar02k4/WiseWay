package com.wiseway.persistence;

import com.wiseway.domain.PreferenceProfile;
import com.wiseway.domain.TransportMode;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="saved_routes")
public class SavedRouteEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String label;
  @Column(nullable=false) private String origin;
  @Column(nullable=false) private String destination;
  @Enumerated(EnumType.STRING) private TransportMode preferredMode;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private PreferenceProfile preferenceProfile=PreferenceProfile.BALANCED;
  @Column(nullable=false) private Instant createdAt=Instant.now();
  @Column(nullable=false) private Instant updatedAt=Instant.now();
  protected SavedRouteEntity() {}
  public SavedRouteEntity(String label,String origin,String destination,TransportMode mode,PreferenceProfile profile){this.label=label;this.origin=origin;this.destination=destination;this.preferredMode=mode;this.preferenceProfile=profile==null?PreferenceProfile.BALANCED:profile;}
  public Long getId(){return id;} public String getLabel(){return label;} public String getOrigin(){return origin;} public String getDestination(){return destination;} public TransportMode getPreferredMode(){return preferredMode;} public PreferenceProfile getPreferenceProfile(){return preferenceProfile;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
  public void update(String l,String o,String d,TransportMode m,PreferenceProfile p){label=l;origin=o;destination=d;preferredMode=m;preferenceProfile=p==null?PreferenceProfile.BALANCED:p;updatedAt=Instant.now();}
}
