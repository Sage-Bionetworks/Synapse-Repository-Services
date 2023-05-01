package org.sagebionetworks.controller.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;

public class OperationTest {
	@Test
	public void testGetWithNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operation.get(null);
		});
	}
	
	@Test
	public void testGetWithUnhandledRequestMethodType() {
		assertThrows(IllegalArgumentException.class, () -> {
			Operation.get(RequestMethod.TRACE);
		});
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
