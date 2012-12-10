package org.sagebionetworks.logging;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class LoggingUtilsTestHelper {

	/**
	 * Fake test to keep the test suite from failing because 
	 * this class has no test methods.
	 */
	@Test
	public void fakeTest() {

	}

	public void testMethod(String arg1, Integer arg2, HttpServletRequest request) {
	}

	public String testAnnotationsMethod(String id, String userId, HttpServletRequest request) {
		return null;
	}
}
