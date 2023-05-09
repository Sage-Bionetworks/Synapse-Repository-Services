package org.sagebionetworks.controller.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;

public class OperationTest {
	@Test
	public void testGetWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			Operation.get(null);
		});
		assertEquals("method is required.", exception.getMessage());
	}
	
	@Test
	public void testGetWithUnhandledRequestMethodType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			Operation.get(RequestMethod.TRACE);
		});
		assertEquals("No operation found for RequestMethod TRACE", exception.getMessage());
	}
	
	@Test
	public void testGetWithRequestMethodGet() {
		assertEquals(Operation.get, Operation.get(RequestMethod.GET));
	}
	
	@Test
	public void testGetWithRequestMethodPost() {
		assertEquals(Operation.post, Operation.get(RequestMethod.POST));
	}
	
	@Test
	public void testGetWithRequestMethodPut() {
		assertEquals(Operation.put, Operation.get(RequestMethod.PUT));
	}
	
	@Test
	public void testGetWithRequestMethodDelete() {
		assertEquals(Operation.delete, Operation.get(RequestMethod.DELETE));
	}
}
