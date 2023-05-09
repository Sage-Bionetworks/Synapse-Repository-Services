package org.sagebionetworks.controller.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ParameterModelTest {
	
	@Test
	public void testWithRequiredWhenInIsNull() {
		IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
			new ParameterModel().withRequired(true);
		});
		assertEquals("The 'in' field must be set before 'required' field.", exception1.getMessage());
		
		IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
			new ParameterModel().withRequired(false);
		});
		assertEquals("The 'in' field must be set before 'required' field.", exception2.getMessage());
	}
	
	@Test
	public void testWithRequiredVerifyRequiredForPathParameter() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new ParameterModel().withIn(ParameterLocation.path).withRequired(false);
		});
		assertEquals("Parameters must be required for path variables.", exception.getMessage());
	}
	
	@Test
	public void testWithRequrired() {
		assertDoesNotThrow(() -> {
			new ParameterModel().withIn(ParameterLocation.query).withRequired(false);
			new ParameterModel().withIn(ParameterLocation.path).withRequired(true);
		});
	}
}
