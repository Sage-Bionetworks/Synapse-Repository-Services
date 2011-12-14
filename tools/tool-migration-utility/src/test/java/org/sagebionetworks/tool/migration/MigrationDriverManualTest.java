package org.sagebionetworks.tool.migration;

import org.junit.Ignore;
import org.junit.Test;

/**
 * This test contains manual tests that will block waiting for user input.
 * All blocking tests are @Ignore by default.  Remove the ignores to run the manual tests.
 * @author John
 *
 */
public class MigrationDriverManualTest {
	
	@Ignore
	@Test
	public void testSafetyCheckBlocking(){
		// Do a safety check
		MigrationDriver.safetyCheck("http://staging/repo/v1", "http://localhost:8080/services-repository-0.9-SNAPSHOT/repo/v1", 101, 102);
	}
	
	@Test
	public void testSafetyCheckNonBlocking(){
		// This test should not block
		MigrationDriver.safetyCheck("http://staging/repo/v1", "http://localhost:8080/services-repository-0.9-SNAPSHOT/repo/v1", 102, 101);
	}

}
