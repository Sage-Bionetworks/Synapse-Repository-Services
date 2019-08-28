package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;

class DoubleValueValidatorTest {

	DoubleValueValidator valueValidator = new DoubleValueValidator();
	String key = "myKey";
	AnnotationsV2ValueType type = AnnotationsV2ValueType.DOUBLE;

	@Test
	public void testValidate_Null(){
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, null, type);
		});
	}

	@Test
	public void testValidate_valid(){
		valueValidator.validate(key, "123", type);
		valueValidator.validate(key, "12.3", type);
		valueValidator.validate(key, "0.0", type);
		valueValidator.validate(key, "-12.3", type);
		valueValidator.validate(key, "inf", type);
		valueValidator.validate(key, "-inf", type);
		valueValidator.validate(key, "nan", type);
	}

	@Test
	public void testValidate_invalid(){
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "", type);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "asdf", type);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "123.a", type);
		});
	}
}