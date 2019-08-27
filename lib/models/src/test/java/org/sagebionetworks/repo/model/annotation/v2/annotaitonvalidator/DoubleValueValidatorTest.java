package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DoubleValueValidatorTest {

	DoubleValueValidator valueValidator = new DoubleValueValidator();

	@Test
	public void testIsValidValue_Null(){
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.isValidValue(null);
		});
	}

	@Test
	public void testIsValidValue_valid(){
		assertTrue(valueValidator.isValidValue("123"));
		assertTrue(valueValidator.isValidValue("12.3"));
		assertTrue(valueValidator.isValidValue("0.0"));
		assertTrue(valueValidator.isValidValue("-12.3"));
		assertTrue(valueValidator.isValidValue("inf"));
		assertTrue(valueValidator.isValidValue("-inf"));
		assertTrue(valueValidator.isValidValue("nan"));
	}

	@Test
	public void testIsValidValue_invalid(){
		assertFalse(valueValidator.isValidValue(""));
		assertFalse(valueValidator.isValidValue("asdf"));
		assertFalse(valueValidator.isValidValue("123.a"));
	}
}