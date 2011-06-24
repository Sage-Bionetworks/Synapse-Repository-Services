package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * TODO write more tests!
 * 
 * @author deflaux
 * 
 */
public class IT500SynapseJavaClient {
	private static Synapse synapse = null;

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setRepositoryEndpoint(StackConfiguration.getAuthenticationServiceEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
		synapse.login(Helpers.getIntegrationTestUser(), Helpers
				.getIntegrationTestUser());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaClientGetADataset() throws Exception {
		JSONObject results = synapse.query("select * from dataset limit 10");

		assertTrue(0 <= results.getInt("totalNumberOfResults"));
		
		JSONArray datasets = results.getJSONArray("results");

		if (0 < datasets.length()) {
			int datasetId = datasets.getJSONObject(0)
					.getInt("dataset.id");

			JSONObject dataset = synapse.getEntity("/dataset/" + datasetId);
			assertTrue(dataset.has("annotations"));

			JSONObject annotations = synapse.getEntity(dataset
					.getString("annotations"));
			assertTrue(annotations.has("stringAnnotations"));
			assertTrue(annotations.has("dateAnnotations"));
			assertTrue(annotations.has("longAnnotations"));
			assertTrue(annotations.has("doubleAnnotations"));
			assertTrue(annotations.has("blobAnnotations"));
		}
	}

}
