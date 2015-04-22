package org.sagebionetworks.util;

import static org.junit.Assert.*;

import org.apache.commons.validator.routines.UrlValidator;
import org.junit.Test;

public class UrlValidatorPatchedTest {

	@Test
	public void testBugInApacheStillExists() {
		assertFalse(
				"If we hit this test failure, apache validator has fixed a bug where it didn't validate file urls correctly. You can now remove the UrlValidatorPatched file",
				new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES).isValid("file:/C:/bin/file.txt"));
	}

	@Test
	public void testBugFixVersion() {
		assertTrue(new UrlValidatorPatched(UrlValidator.ALLOW_ALL_SCHEMES).isValid("file:/C:/bin/file.txt"));
	}
}
