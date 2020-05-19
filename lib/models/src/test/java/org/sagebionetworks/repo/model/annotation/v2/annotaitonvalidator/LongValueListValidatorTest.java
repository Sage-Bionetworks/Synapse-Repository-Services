package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class LongValueListValidatorTest {

	LongValueListValidator valueValidator = new LongValueListValidator(AnnotationsValueType.LONG);
	String key = "myKey";

	@Test
	void testValidate_valid() {
		valueValidator.validate(key, "-123");
		valueValidator.validate(key, "0");
		valueValidator.validate(key, "123");
	}

	@Test
	void testValidate_invalid() {
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, (String) null);
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "-123.1");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "0.0");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "123.0");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "asdf");
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, "");
		});
	}


	@Test
	public void testValidateList_allValid(){
		valueValidator.validate(key , Arrays.asList("123", "456", "678999787978"));
	}

	@Test
	public void testValidateList_containsInValid(){
		assertThrows(IllegalArgumentException.class, () -> {
			valueValidator.validate(key, Arrays.asList("123", "nope", "23131"));
		});
	}
}