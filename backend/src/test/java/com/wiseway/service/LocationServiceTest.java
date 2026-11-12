package com.wiseway.service;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class LocationServiceTest { @Test void searchReturnsContextAndCoordinates(){var x=new LocationService().search("sector");assertEquals(1,x.size());assertEquals("Gurugram, Haryana",x.get(0).context());assertNotEquals(0,x.get(0).latitude());} @Test void unknownLocationDoesNotResolve(){assertTrue(new LocationService().resolve("Nowhere").isEmpty());} }
