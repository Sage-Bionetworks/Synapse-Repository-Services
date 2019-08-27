package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LongValueValidatorTest {

	LongValueValidator valueValidator = new LongValueValidator();

	@Test
	void isValidValue_valid() {
		assertTrue(valueValidator.isValidValue("-123"));
		assertTrue(valueValidator.isValidValue("0"));
		assertTrue(valueValidator.isValidValue("123"));
	}

	@Test
	void isValidValue_invalid() {
		assertFalse(valueValidator.isValidValue("-123.1"));
		assertFalse(valueValidator.isValidValue("0.0"));
		assertFalse(valueValidator.isValidValue("123.0"));
		assertFalse(valueValidator.isValidValue("asdf"));
		assertFalse(valueValidator.isValidValue(""));

	}
}