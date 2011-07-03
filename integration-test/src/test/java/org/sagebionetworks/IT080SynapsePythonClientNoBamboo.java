package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.Helpers.ExternalProcessResult;

/**
 * Invoke our Python integration tests here and check the output of the test
 * suite to confirm/deny that all tests passed
 * 
 * TODO consider switching the existing handful of tests to PyUnit
 * http://pyunit.sourceforge.net/ if it seems to be an improvement over the
 * unittest python library
 * 
 * TODO pass the endpoints and username to the tests
 * 
 * @author deflaux
 * 
 */
public class IT080SynapsePythonClientNoBamboo {

	/**
	 * @throws Exception
	 */
	@Test
	public void testPythonClient() throws Exception {
		String cmd[] = { Helpers.getPython27Path(),
				"target/non-java-dependencies/synapse/client.py" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);

		String results[] = result.getStderr().split("\n");
		assertTrue(results[results.length - 1].endsWith("OK"));
	}
}
