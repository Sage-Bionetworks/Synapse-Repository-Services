package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.Helpers.ExternalProcessResult;

/**
 * Run this integration test to clean all datasets out of the database
 * 
 * @author deflaux
 * 
 */
public class IT900DatasetNukerNoBamboo {

	/**
	 * @throws Exception
	 */
	@Test
	public void testDatasetNuker() throws Exception {
		String cmd[] = { Helpers.getPython27Path(),
				"target/non-java-dependencies/datasetNuker.py",
				"--repoEndpoint",
				StackConfiguration.getRepositoryServiceEndpoint(),
				"--authEndpoint",
				StackConfiguration.getAuthenticationServiceEndpoint(),
				"--user",
				Helpers.getIntegrationTestUser(),
				"--password",
				Helpers.getIntegrationTestUser(),
		};
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertEquals("", result.getStderr());
	}
}
