package com.wiseway.service;
import com.fasterxml.jackson.databind.JsonNode; import java.time.Duration; import java.util.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.http.client.SimpleClientHttpRequestFactory; import org.springframework.stereotype.Service; import org.springframework.web.client.RestClient;
@Service @ConditionalOnProperty(name="wiseway.routing.provider",havingValue="osrm")
public class OsrmRoutingProvider implements RoutingProvider {
 private final RestClient client;
 public OsrmRoutingProvider(@Value("${wiseway.routing.osrm-base-url}") String baseUrl,@Value("${wiseway.routing.timeout-ms:5000}") int timeout){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofMillis(timeout));f.setReadTimeout(Duration.ofMillis(timeout));client=RestClient.builder().baseUrl(baseUrl).requestFactory(f).build();}
 public Optional<Route> route(LocationService.Location a,LocationService.Location b){try{JsonNode root=client.get().uri(uri->uri.path("/route/v1/driving/{coords}").queryParam("overview","full").queryParam("geometries","geojson").build(a.longitude()+","+a.latitude()+";"+b.longitude()+","+b.latitude())).retrieve().body(JsonNode.class);if(root==null||!"Ok".equals(root.path("code").asText())||root.path("routes").isEmpty())return Optional.empty();var x=root.path("routes").get(0);return Optional.of(new Route(x.path("distance").asDouble(),x.path("duration").asDouble(),x.path("geometry").toString(),"OSRM"));}catch(RuntimeException e){return Optional.empty();}}
}
