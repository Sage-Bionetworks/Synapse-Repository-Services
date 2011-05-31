package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.Helpers.ExternalProcessResult;

/**
 * Run this integration test to load in some data
 * 
 * @author deflaux
 *
 */
public class IT015DatasetMetadataLoader {

	/**
	 * @throws Exception
	 */
	@Test
	public void testDatasetMetadataLoader() throws Exception {
		String cmd[] = {
				Helpers.getPython27Path(),
				"../tools/DatasetMetadataLoader/datasetCsvLoader.py",
				"--fakeLocalData",
				"--serviceEndpoint",
				Helpers.getRepositoryServiceBaseUrl(),
				"--datasetsCsv",
				"../tools/DatasetMetadataLoader/AllDatasets.csv",
				"--layersCsv",
				"../tools/DatasetMetadataLoader/AllDatasetLayerLocations.csv" };
		ExternalProcessResult result = Helpers.runExternalProcess(cmd);
		assertEquals("", result.getStderr());
	}

}
