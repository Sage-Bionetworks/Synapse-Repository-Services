package org.sagebionetworks.controller.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ParameterModelTest {
	
	@Test
	public void testWithRequiredWhenInIsNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			new ParameterModel().withRequired(true);
		});
		
		assertThrows(IllegalArgumentException.class, () -> {
			new ParameterModel().withRequired(false);
		});
	}
	
	@Test
	public void testWithRequiredVerifyRequiredForPathParameter() {
		assertThrows(IllegalArgumentException.class, () -> {
			new ParameterModel().withIn(ParameterLocation.path).withRequired(false);
		});
	}
	
	@Test
	public void testWithRequrired() {
		assertDoesNotThrow(() -> {
			new ParameterModel().withIn(ParameterLocation.query).withRequired(false);
			new ParameterModel().withIn(ParameterLocation.path).withRequired(true);
		});
	}
}
