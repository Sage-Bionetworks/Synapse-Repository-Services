package org.sagebionetworks.repo.model.jdo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.InvalidModelException;

public class NameValidationTest {

	@Test
	public void testInvalidNames() {
		// There are all invalid names
		String[] invalidNames = new String[] { "~", "!", "@", "#", "$", "%", "^", "&", "*", "\"", "?", "<",
				">", "/", ";", "{", "}", "|", "=", "White\n\t Space" };
		for (int i = 0; i < invalidNames.length; i++) {
			int index = i;
			String message = assertThrows(IllegalArgumentException.class, () -> {
				// These are all bad names
				NameValidation.validateName(invalidNames[index]);
			}).getMessage();
			String expected = String.format(
					"Invalid Name: '%s'. Names may only contain: letters, numbers, spaces, underscores, hyphens, periods, plus signs, apostrophes, and parentheses",
					invalidNames[index]);
			assertEquals(expected, message);
		}
	}
	
	@Test
	public void testInvalidNamesWhiteSpaceOnly() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// These are all bad names
			NameValidation.validateName("\n\t");
		}).getMessage();
		assertEquals("Name cannot be only whitespace or empty string", message);
	}
	
	@Test
	public void testInvalidNamesWhiteEmpty() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// These are all bad names
			NameValidation.validateName("");
		}).getMessage();
		assertEquals("Name cannot be only whitespace or empty string", message);
	}

	@Test
	public void testValidNames() throws InvalidModelException {
		// There are all invalid names
		List<String> validNames = new ArrayList<String>();
		// All lower
		for (char ch = 'a'; ch <= 'z'; ch++) {
			validNames.add("" + ch);
		}
		// All upper
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			validNames.add("" + ch);
		}
		// all numbers
		for (char ch = '0'; ch <= '9'; ch++) {
			validNames.add("" + ch);
		}
		// underscore
		validNames.add("_");
		validNames.add(" Trimable ");
		validNames.add("Has Space");
		validNames.add("A1_b3po");
		validNames.add("Has-Dash");
		validNames.add("Breast Cancer HER2+ ICGC");
		validNames.add("one(2)");
		validNames.add("o,2");
		validNames.add("o.2");
		for (int i = 0; i < validNames.size(); i++) {
			// These are all bad names
			NameValidation.validateName(validNames.get(i));
		}
	}
}
