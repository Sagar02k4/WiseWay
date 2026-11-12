package com.wiseway.service;
import java.util.*; import org.springframework.stereotype.Service;
@Service public class GeocodingService { private final LocationService catalog; private final Optional<GeocodingProvider> external; public GeocodingService(LocationService c,Optional<GeocodingProvider> e){catalog=c;external=e;} public List<LocationService.Location> search(String q){return external.map(x->x.search(q)).orElseGet(()->catalog.search(q));} }
