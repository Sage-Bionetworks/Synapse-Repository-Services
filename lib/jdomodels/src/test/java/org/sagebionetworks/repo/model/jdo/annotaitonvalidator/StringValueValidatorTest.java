package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;

class StringValueValidatorTest {

	StringValueValidator valueValidator = new StringValueValidator();

	@Test
	void isValidValue_valid() {
		//anything except values exceeding the limit should return true
		assertTrue(valueValidator.isValidValue(""));
		assertTrue(valueValidator.isValidValue("asdf"));
		assertTrue(valueValidator.isValidValue("123"));
		assertTrue(valueValidator.isValidValue("123.456"));
		assertTrue(valueValidator.isValidValue(Strings.repeat("a", StringValueValidator.MAX_STRING_SIZE)));

	}

	@Test
	void isValidValue_exceedLength() {
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.isValidValue(Strings.repeat("a", StringValueValidator.MAX_STRING_SIZE + 1));
		});
	}

	}