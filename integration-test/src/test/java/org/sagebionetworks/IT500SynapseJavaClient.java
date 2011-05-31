package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

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
	 * @throws MalformedURLException 
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws MalformedURLException {

		synapse = new Synapse();
		synapse.setRepositoryEndpoint(Helpers.getRepositoryServiceBaseUrl());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaClientGetADataset() throws Exception {
		JSONObject results = synapse.query("select * from dataset");

		assertTrue(0 < results.getInt("totalNumberOfResults"));

		int datasetId = results.getJSONArray("results").getJSONObject(0)
				.getInt("dataset.id");

		JSONObject dataset = synapse.getEntity("/dataset/" + datasetId);
		assertTrue(dataset.has("annotations"));
		
		JSONObject annotations = synapse.getEntity(dataset.getString("annotations"));
		assertTrue(annotations.has("stringAnnotations"));
		assertTrue(annotations.has("dateAnnotations"));
		assertTrue(annotations.has("longAnnotations"));
		assertTrue(annotations.has("doubleAnnotations"));
		assertTrue(annotations.has("blobAnnotations"));
	}

}
