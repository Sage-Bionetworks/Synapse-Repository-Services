package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.InvalidModelException;

public class NameValidationTest {

	
	@Test
	public void testInvalidNames() {
		// There are all invalid names
		String[] invalidNames = new String[] { "~", "!", "@", "#", "$", "%",
				"^", "&", "*", "\"", "\n\t", "'", "?", "<", ">", "/",
				";", "{", "}", "|", "=", "White\n\t Space", "" };
		for (int i = 0; i < invalidNames.length; i++) {
			try {
				// These are all bad names
				 NameValidation.validateName(invalidNames[i]);
				fail("Name: " + invalidNames[i] + " is invalid");
			} catch (IllegalArgumentException e) {
				// Expected
			}
		}
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
