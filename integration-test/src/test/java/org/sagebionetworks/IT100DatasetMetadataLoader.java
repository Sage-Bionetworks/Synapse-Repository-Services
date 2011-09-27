package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.utils.ExternalProcessHelper.ExternalProcessResult;
import org.sagebionetworks.utils.ExternalProcessHelper;
import org.sagebionetworks.StackConfiguration;

/**
 * Run this integration test to load in some realistic data
 * 
 * @author deflaux
 *
 */
public class IT100DatasetMetadataLoader {

	/**
	 * @throws Exception
	 */
	@Test
	public void testDatasetMetadataLoader() throws Exception {
		String cmd[] = {
				Helpers.getPython27Path(),
				"target/non-java-dependencies/datasetCsvLoader.py",
				"--fakeLocalData",
				"--repoEndpoint",
				StackConfiguration.getRepositoryServiceEndpoint(),
				"--authEndpoint",
				StackConfiguration.getAuthenticationServicePublicEndpoint(),
				"--user",
				StackConfiguration.getIntegrationTestUserOneName(),
				"--password",
				StackConfiguration.getIntegrationTestUserOnePassword(),
				"--datasetsCsv",
				"target/non-java-dependencies/AllDatasets.csv",
				"--layersCsv",
				"target/non-java-dependencies/AllDatasetLayerLocations.csv" };
		ExternalProcessResult result = ExternalProcessHelper.runExternalProcess(cmd);
		assertEquals(0, result.getReturnCode());
		assertEquals("", result.getStderr());
	}

}
