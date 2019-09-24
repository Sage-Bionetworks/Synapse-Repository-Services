package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

class StringValueValidatorTest {

	StringValueValidator valueValidator = new StringValueValidator();
	String key = "myKey";
	AnnotationsValueType type = AnnotationsValueType.STRING;

	@Test
	void testValidate_valid() {
		//anything except values exceeding the limit should return true
		valueValidator.validate(key, "", type);
		valueValidator.validate(key, "asdf", type);
		valueValidator.validate(key, "123", type);
		valueValidator.validate(key, "123.456", type);
		valueValidator.validate(key, Strings.repeat("a", StringValueValidator.MAX_STRING_SIZE), type);

	}

	@Test
	void testValidate_exceedLength() {
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, Strings.repeat("a", StringValueValidator.MAX_STRING_SIZE + 1), type);
		});
	}

	}