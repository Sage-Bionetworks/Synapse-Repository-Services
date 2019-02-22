package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.InvalidModelException;

/**
 * Unit test for the field type cache
 *
 */
public class AnnotationUtilsTest {
	
	Set<String> uniqueNames;
	
	@Before
	public void before(){
		uniqueNames = new HashSet<String>();
	}
	

	@Test
	public void testInvalidNames() {
		// There are all invalid names
		String[] invalidNames = new String[] { "~", "!", "@", "#", "$", "%",
				"^", "&", "*", "(", ")", "\"", "\n\t", "'", "?", "<", ">", "/",
				";", "{", "}", "|", "=", "+", "-", "White\n\t Space", null, "" };
		for (int i = 0; i < invalidNames.length; i++) {
			try {
				// These are all bad names
				AnnotationUtils.checkKeyName(invalidNames[i], uniqueNames);
				fail("Name: " + invalidNames[i] + " is invalid");
			} catch (InvalidModelException e) {
				// Expected
			}
		}
	}

	@Test
	public void testValidNames() throws InvalidModelException {
		// There are all invalid names
		List<String> vlaidNames = new ArrayList<String>();
		// All lower
		for (char ch = 'a'; ch <= 'z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// All upper
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// all numbers
		for (char ch = '0'; ch <= '9'; ch++) {
			vlaidNames.add("" + ch);
		}
		// underscore
		vlaidNames.add("_");
		vlaidNames.add(" Trimable ");
		vlaidNames.add("A1_b3po");
		for (int i = 0; i < vlaidNames.size(); i++) {
			// These are all bad names
			AnnotationUtils.checkKeyName(vlaidNames.get(i), uniqueNames);
		}
	}
	
	@Test
	public void testValidateAnnotations(){
		Annotations annos = new Annotations();
		annos.addAnnotation("one", new Date(1));
		annos.addAnnotation("two", 1.2);
		annos.addAnnotation("three", 1L);
		AnnotationUtils.validateAnnotations(annos);
	}

	@Test
	public void testValidateAnnotationsDuplicateNames(){
		Annotations annos = new Annotations();
		// add two annotations with the same name but different type.
		annos.addAnnotation("two", 1.2);
		annos.addAnnotation("two", 1L);
		try {
			AnnotationUtils.validateAnnotations(annos);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Duplicate annotation name: 'two'", e.getMessage());
		}
	}
}
