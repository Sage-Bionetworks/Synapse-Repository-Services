package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
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
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServiceEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
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
			int datasetId = datasets.getJSONObject(0).getInt("dataset.id");

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

	@Test
	public void testJavaClientCreateAgreementIfNeeded() throws Exception {

		JSONObject datasetQueryResults = synapse
				.query("select * from dataset where name == \"MSKCC Prostate Cancer\"");
		assertEquals(1, datasetQueryResults.getJSONArray("results").length());
		JSONObject datasetQueryResult = datasetQueryResults.getJSONArray(
				"results").getJSONObject(0);

		JSONObject agreementQueryResults = synapse
				.query("select * from agreement where datasetId == "
						+ datasetQueryResult.getString("dataset.id")
						+ " and eulaId == \""
						+ datasetQueryResult.getString("dataset.eulaId")
						+ "\" and userId == \""
						+ StackConfiguration.getIntegrationTestUserOneName()
						+ "\"");

		// Agree to the eula, if needed
		// Dev Note: ReadOnlyWikiGenerator has a dependency upon this if the
		// user running the generator is not an admin or has not signed the EULA
		// for the MSKCC dataset
		if (0 == agreementQueryResults.getJSONArray("results").length()) {
			JSONObject agreement = new JSONObject();
			agreement.put("datasetId", datasetQueryResult
					.getString("dataset.id"));
			agreement.put("eulaId", datasetQueryResult
					.getString("dataset.eulaId"));
			synapse.createEntity("/agreement", agreement);
		}
	}
}
