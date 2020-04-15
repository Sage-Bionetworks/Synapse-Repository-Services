package org.sagebionetworks.cloudwatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;

public class MetricUtilsTest {
	
	@Test
	public void testStackTraceToString() {
		
		String message = "Some message";
		
		Exception ex = new Exception(message);
		
		String origStackTrace = ExceptionUtils.getStackTrace(ex);
		
		String retrievedStackTrace = MetricUtils.stackTracetoString(ex);
		
		// check that the message has been removed
		assertTrue(retrievedStackTrace.indexOf(message)<0);
		// check that after the first (modified) line they are the same
		assertEquals(
				origStackTrace.substring(origStackTrace.indexOf("at")),
				retrievedStackTrace.substring(retrievedStackTrace.indexOf("at"))
		);
		
	}

}
