package com.wiseway.config;
import com.wiseway.domain.PreferenceProfile; import com.wiseway.domain.TransportMode; import com.wiseway.persistence.*; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@Configuration public class DemoDataConfig { @Bean CommandLineRunner seed(SavedRouteRepository routes){return args->{if(routes.count()==0)routes.save(new SavedRouteEntity("Home to Campus","Sector 14","Main Campus",TransportMode.METRO,PreferenceProfile.BALANCED));};}}
