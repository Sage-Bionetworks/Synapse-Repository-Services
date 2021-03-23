package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.google.common.base.Strings;

public class StringValueListValidatorTest {

	StringValueListValidator valueValidator = new StringValueListValidator();
	String key = "myKey";

	@Test
	void testValidate_valid() {
		//anything except values exceeding the limit should return true
		valueValidator.validate(key, Collections.singletonList(""));
		valueValidator.validate(key, Collections.singletonList("asdf"));
		valueValidator.validate(key, Collections.singletonList("123"));
		valueValidator.validate(key, Collections.singletonList("123.456"));
		valueValidator.validate(key, Collections.singletonList(Strings.repeat("a", StringValueListValidator.MAX_STRING_SIZE)));
		int secondStringSize = 42;
		valueValidator.validate(key, Arrays.asList(Strings.repeat("a", StringValueListValidator.MAX_STRING_SIZE - secondStringSize ),
				Strings.repeat("a", secondStringSize )));
	}

	@Test
	void testValidate_containsNull(){
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, Collections.singletonList(null));
		});
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, Arrays.asList("asdf", null, "asdf"));
		});

	}

	@Test
	void testValidate_exceedLength() {
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, Collections.singletonList(Strings.repeat("a", StringValueListValidator.MAX_STRING_SIZE + 1)));
		});

		int secondStringSize = 42;
		assertThrows(IllegalArgumentException.class, ()->{
			valueValidator.validate(key, Arrays.asList(Strings.repeat("a", StringValueListValidator.MAX_STRING_SIZE - secondStringSize ),
					Strings.repeat("a", secondStringSize ),
					"a"));
		});
	}

}