package com.wiseway.service;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class LocationServiceTest { @Test void searchReturnsContextAndCoordinates(){var x=new LocationService().search("sector");assertTrue(x.size()>=3);assertTrue(x.stream().allMatch(v->v.context().equals("Gurugram, Haryana")));assertTrue(x.stream().allMatch(v->v.latitude()!=0));} @Test void unknownLocationDoesNotResolve(){assertTrue(new LocationService().resolve("Nowhere").isEmpty());} }
