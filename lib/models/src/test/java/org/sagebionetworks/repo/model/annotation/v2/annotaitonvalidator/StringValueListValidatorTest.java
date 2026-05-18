package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnConstants;

import com.google.common.base.Strings;

public class StringValueListValidatorTest {

	StringValueListValidator valueValidator = new StringValueListValidator();
	String key = "myKey";

	@Test
	void testValidate_valid() {
		valueValidator.validate(key, Collections.singletonList(""));
		valueValidator.validate(key, Collections.singletonList("asdf"));
		valueValidator.validate(key, Collections.singletonList("123"));
		valueValidator.validate(key, Collections.singletonList("123.456"));
		// single value at the per-value limit
		valueValidator.validate(key, Collections.singletonList(
				Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue())));
		// two values each within the per-value limit, total within budget
		int secondStringSize = 42;
		valueValidator.validate(key, Arrays.asList(
				Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue() - secondStringSize),
				Strings.repeat("a", secondStringSize)));
		// 100 values each at the per-value limit: exactly at the total budget (100 * 1000 = 100,000)
		int maxItems = (int) (ColumnConstants.MAX_ALLOWED_LIST_TOTAL_CHARACTERS / ColumnConstants.MAX_ALLOWED_STRING_SIZE);
		List<String> maxList = new ArrayList<>();
		for(int i = 0; i < maxItems; i++){
			maxList.add(Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue()));
		}
		valueValidator.validate(key, maxList);
	}

	@Test
	void testValidate_containsNull(){
		assertThrows(IllegalArgumentException.class, () ->
			valueValidator.validate(key, Collections.singletonList(null)));
		assertThrows(IllegalArgumentException.class, () ->
			valueValidator.validate(key, Arrays.asList("asdf", null, "asdf")));
	}

	@Test
	void testValidate_singleValueExceedsPerValueLimit() {
		// call under test - one character over the per-value limit
		String message = assertThrows(IllegalArgumentException.class, () ->
			valueValidator.validate(key, Collections.singletonList(
					Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue() + 1)))
		).getMessage();

		assertEquals("A single string annotation value cannot exceed "
				+ ColumnConstants.MAX_ALLOWED_STRING_SIZE + " characters.", message);
	}

	@Test
	void testValidate_sumOfValuesExceedsTotalLimit() {
		// 100 values each at the per-value limit = exactly the total budget (100,000 chars)
		int maxItems = (int) (ColumnConstants.MAX_ALLOWED_LIST_TOTAL_CHARACTERS / ColumnConstants.MAX_ALLOWED_STRING_SIZE);
		List<String> values = new ArrayList<>();
		for(int i = 0; i < maxItems; i++){
			values.add(Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue()));
		}
		// one more character pushes the sum over the total limit
		values.add("a");

		// call under test
		String message = assertThrows(IllegalArgumentException.class, () ->
			valueValidator.validate(key, values)
		).getMessage();

		assertEquals("The sum of all string values for annotation '"
				+ key + "' cannot exceed " + ColumnConstants.MAX_ALLOWED_LIST_TOTAL_CHARACTERS + " characters.", message);
	}

}