package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;

class LongValueValidatorTest {

	LongValueValidator valueValidator = new LongValueValidator();
	String key = "myKey";
	AnnotationsV2ValueType type = AnnotationsV2ValueType.LONG;

	@Test
	void testValidate_valid() {
		valueValidator.validate(key, "-123", type);
		valueValidator.validate(key, "0", type);
		valueValidator.validate(key, "123", type);
	}

	@Test
	void testValidate_invalid() {
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "-123.1", type);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "0.0", type);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "123.0", type);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "asdf", type);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "", type);
		});

	}
}