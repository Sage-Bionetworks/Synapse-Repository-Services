package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.Helpers.ExternalProcessResult;
import org.sagebionetworks.StackConfiguration;

/**
 * @author deflaux
 * 
 */
public class IT700SynapseRClient {

	/**
	 * @throws Exception
	 */
	@Test
	public void testBuildRClient() throws Exception {
		String cmd[] = { Helpers.getRPath(), "CMD", "build",
				"target/non-java-dependencies/synapseRClient" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertEquals(0, result.getReturnCode());
	}

	/**
	 * This fails due to dependencies upon pdflatex, etc.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCheckRClient() throws Exception {
		String cmd[] = { Helpers.getRPath(), "CMD", "check", "--no-manual",
				"target/non-java-dependencies/synapseRClient" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertEquals(0, result.getReturnCode());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testInstallRClient() throws Exception {
		String cmd[] = { Helpers.getRPath(), "CMD", "INSTALL",
				"target/non-java-dependencies/synapseRClient" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertTrue(0 <= result.getStderr().indexOf("DONE"));
	}

	/**
	 * We should not need to set endpoints for unit tests, but its easy in R to
	 * have a unit test accidentally be an integration test, so we'll do this to
	 * be on the safe side.
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore  // Fix me PLFM-402
	public void testRunRUnitTests() throws Exception {
		String cmd[] = {
				Helpers.getRPath(),
				"-e",
				"library(synapseClient)",
				"-e",
				"synapseAuthServiceEndpoint(endpoint='"
						+ StackConfiguration.getAuthenticationServiceEndpoint()
						+ "')",
				"-e",
				"synapseRepoServiceEndpoint(endpoint='"
						+ StackConfiguration.getRepositoryServiceEndpoint()
						+ "')",
				"-e",
				"synapseLogin(username='" + StackConfiguration.getIntegrationTestUserOneName()
						+ "', password='"
						+ StackConfiguration.getIntegrationTestUserOnePassword() + "')",
				"-e", "synapseClient:::.test()" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertTrue(0 <= result.getStdout().indexOf(" 0 errors, 0 failures"));
	}

	/**
	 * TODO for now skipping some R integration tests because they access S3
	 * functionality which is not yet stubbed out in the repo service.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRunRIntegrationTests() throws Exception {
		String cmd[] = {
				Helpers.getRPath(),
				"-e",
				"library(synapseClient)",
				"-e",
				"synapseAuthServiceEndpoint(endpoint='"
						+ StackConfiguration.getAuthenticationServiceEndpoint()
						+ "')",
				"-e",
				"synapseRepoServiceEndpoint(endpoint='"
						+ StackConfiguration.getRepositoryServiceEndpoint()
						+ "')", "-e",
				"synapseLogin(username='" + StackConfiguration.getIntegrationTestUserOneName()
						+ "', password='"
						+ StackConfiguration.getIntegrationTestUserOnePassword() + "')",
				"-e",
				"synapseClient:::.integrationTest(testFileRegexp=\"^test_[^_]*R$\")" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertTrue(0 <= result.getStdout().indexOf(" 0 errors, 0 failures"));
	}
}
