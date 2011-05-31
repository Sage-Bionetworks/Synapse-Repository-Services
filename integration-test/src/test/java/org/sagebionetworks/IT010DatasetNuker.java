package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.Helpers.ExternalProcessResult;

/**
 * Run this integration test as one of the first few to clean out our database
 * 
 * @author deflaux
 *
 */
public class IT010DatasetNuker {

	/**
	 * @throws Exception
	 */
	@Test
	public void testDatasetNuker() throws Exception {
		String cmd[] = {
				Helpers.getPython27Path(),
				"../tools/DatasetMetadataLoader/datasetNuker.py",
				"--serviceEndpoint", Helpers.getRepositoryServiceBaseUrl() };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertEquals("", result.getStderr());
	}
}
