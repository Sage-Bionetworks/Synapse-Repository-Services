package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.Helpers.ExternalProcessResult;

/**
 * Run this integration test to load in some realistic data
 * 
 * @author deflaux
 *
 */
public class IT100DatasetMetadataLoaderNoBamboo {

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
				StackConfiguration.getAuthenticationServiceEndpoint(),
				"--user",
				Helpers.getIntegrationTestUser(),
				"--password",
				Helpers.getIntegrationTestUser(),
				"--datasetsCsv",
				"target/non-java-dependencies/AllDatasets.csv",
				"--layersCsv",
				"target/non-java-dependencies/AllDatasetLayerLocations.csv" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertEquals("", result.getStderr());
	}

}
