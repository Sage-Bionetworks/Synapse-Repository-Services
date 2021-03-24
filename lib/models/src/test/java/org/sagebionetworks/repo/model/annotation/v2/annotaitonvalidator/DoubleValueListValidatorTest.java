package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class DoubleValueListValidatorTest {

	DoubleValueListValidator valueValidator = new DoubleValueListValidator();
	String key = "myKey";

	@Test
	public void testValidate_Null(){
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, (String) null);
		});
	}

	@Test
	public void testValidate_valid(){
		valueValidator.validate(key, "123");
		valueValidator.validate(key, "12.3");
		valueValidator.validate(key, "0.0");
		valueValidator.validate(key, "-12.3");
		valueValidator.validate(key, "inf");
		valueValidator.validate(key, "-inf");
		valueValidator.validate(key, "nan");
	}

	@Test
	public void testValidate_invalid(){
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, (String) null);
		});

		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "asdf");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "123.a");
		});
	}

	@Test
	public void testValidateList_allValid(){
		valueValidator.validate(key , Arrays.asList("123", "12.3", "inf"));
	}

	@Test
	public void testValidateList_containsInValid(){
		assertThrows(IllegalArgumentException.class, () -> {
			valueValidator.validate(key, Arrays.asList("123", "nope", "inf"));
		});
	}
}