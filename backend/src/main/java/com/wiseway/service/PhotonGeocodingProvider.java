package com.wiseway.service;
import com.fasterxml.jackson.databind.JsonNode; import java.time.Duration; import java.util.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.http.client.SimpleClientHttpRequestFactory; import org.springframework.stereotype.Service; import org.springframework.web.client.RestClient;
@Service @ConditionalOnProperty(name="wiseway.geocoding.provider",havingValue="photon")
public class PhotonGeocodingProvider implements GeocodingProvider {
 private final RestClient client;
 public PhotonGeocodingProvider(@Value("${wiseway.geocoding.photon-base-url}") String url,@Value("${wiseway.geocoding.timeout-ms:5000}") int timeout){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofMillis(timeout));f.setReadTimeout(Duration.ofMillis(timeout));client=RestClient.builder().baseUrl(url).requestFactory(f).build();}
 public List<LocationService.Location> search(String query){try{var root=client.get().uri(u->u.path("/api").queryParam("q",query).queryParam("limit",8).build()).retrieve().body(JsonNode.class);if(root==null)return List.of();var out=new ArrayList<LocationService.Location>();for(var f:root.path("features")){var c=f.path("geometry").path("coordinates");var p=f.path("properties");if(c.size()<2)continue;out.add(new LocationService.Location("photon-"+out.size(),p.path("name").asText(query),context(p),c.get(1).asDouble(),c.get(0).asDouble()));}return out;}catch(RuntimeException e){return List.of();}}
 private String context(JsonNode p){var city=p.path("city").asText(p.path("county").asText(""));var state=p.path("state").asText("");return (city+", "+state).replaceAll("^, |, $","");}
}
