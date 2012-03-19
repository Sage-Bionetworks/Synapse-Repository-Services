package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.utils.ExternalProcessHelper.ExternalProcessResult;
import org.sagebionetworks.utils.ExternalProcessHelper;
import org.sagebionetworks.StackConfiguration;

/**
 * Run this integration test to clean all datasets out of the database
 * 
 * @author deflaux
 * 
 */
public class IT900DatasetNuker {

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
				StackConfiguration.getAuthenticationServicePrivateEndpoint(),
				"--user",
				StackConfiguration.getIntegrationTestUserAdminName(),
				"--password",
				StackConfiguration.getIntegrationTestUserAdminPassword(),
		};
		ExternalProcessResult result = ExternalProcessHelper.runExternalProcess(cmd);
		assertEquals(0, result.getReturnCode());
		assertEquals("", result.getStderr());
	}
}
