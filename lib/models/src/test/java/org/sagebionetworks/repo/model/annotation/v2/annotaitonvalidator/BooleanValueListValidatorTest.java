package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class BooleanValueListValidatorTest {
	
	BooleanValueListValidator valueValidator;
	String key = "hasBoolean";
	
	@BeforeEach
	public void before() {
		valueValidator = new BooleanValueListValidator();
		key = "hasBoolean";
	}

	@Test
	public void testValidateWithValid() {
		valueValidator.validate(key, "true");
		valueValidator.validate(key, "false");
		valueValidator.validate(key, "TRUE");
		valueValidator.validate(key, "FALSE");
		valueValidator.validate(key, "True");
		valueValidator.validate(key, "False");
	}
	
	@Test
	public void testValidateWithNull() {
		String value = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, value);
		}).getMessage();
		assertEquals(AnnotationsV2ValueListValidator.NULL_IS_NOT_ALLOWED, message);
	}
	
	@Test
	public void testValidateWithTooLongForBoolean() {
		String value = "false1";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, value);
		}).getMessage();
		assertEquals(AnnotationsV2ValueListValidator.getIllegalValueMessage(key, AnnotationsValueType.BOOLEAN, value), message);
	}
	
	@Test
	public void testValidateWithNotABoolean() {
		String value = "one";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, value);
		}).getMessage();
		assertEquals(AnnotationsV2ValueListValidator.getIllegalValueMessage(key, AnnotationsValueType.BOOLEAN, value), message);
	}
	
	@Test
	public void testValidateList_allValid(){
		valueValidator.validate(key , Arrays.asList("true", "True", "False"));
	}

	@Test
	public void testValidateList_containsInValid(){
		assertThrows(IllegalArgumentException.class, () -> {
			valueValidator.validate(key, Arrays.asList("true", "no-way"));
		});
	}
}
