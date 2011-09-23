package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.SynapseServiceException;
import org.sagebionetworks.client.SynapseUserException;
import org.sagebionetworks.utils.HttpClientHelper;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * @author deflaux
 */
public class IT500SynapseJavaClient {
	
	private static Synapse synapse = null;
	private static JSONObject project = null;

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		
		project = synapse.createEntity("/project", new JSONObject("{\"name\":\"Java Client Test\"}"));
	}

	/**
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@AfterClass
	public static void afterClass() throws HttpException, IOException, JSONException, SynapseUserException, SynapseServiceException {
		if(null != project) {
			synapse.deleteEntity(project.getString("uri"));
		}
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
	public void testJavaClientCRUD() throws Exception {
		JSONObject dataset = synapse.createEntity("/dataset", new JSONObject("{\"name\":\"testCrud\", \"status\": \"created\", \"parentId\":\"" + project.getString("id") + "\"}"));
		assertEquals("created", dataset.getString("status"));
		dataset.put("status", "updated");
		JSONObject updatedDataset = synapse.updateEntity(dataset.getString("uri"), dataset);
		assertEquals("updated", updatedDataset.getString("status"));	

		JSONArray annotationValue = new JSONArray();
		annotationValue.put("created");
		JSONObject stringAnnotations = new JSONObject();
		stringAnnotations.put("annotStatus", annotationValue);
		JSONObject annotations = new JSONObject();
		annotations.put("stringAnnotations", stringAnnotations);

		JSONObject createdAnnotations = synapse.updateEntity(updatedDataset.getString("annotations"), annotations);
		assertEquals("created", createdAnnotations.getJSONObject("stringAnnotations").getJSONArray("annotStatus").getString(0));

		annotationValue = new JSONArray();
		annotationValue.put("updated");
		stringAnnotations = new JSONObject();
		stringAnnotations.put("annotStatus", annotationValue);
		annotations = new JSONObject();
		annotations.put("stringAnnotations", stringAnnotations);

		JSONObject updatedAnnotations = synapse.updateEntity(updatedDataset.getString("annotations"), annotations);
		assertEquals("updated", updatedAnnotations.getJSONObject("stringAnnotations").getJSONArray("annotStatus").getString(0));
	}

	/**
	 * @throws Exception
	 */
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
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaClientUploadDownloadLayerFromS3() throws Exception {
		
	    File dataSourceFile = File.createTempFile("integrationTest", ".txt");
	    dataSourceFile.deleteOnExit();
	    FileWriter writer = new FileWriter(dataSourceFile);
	    writer.write("Hello world!");
	    writer.close();
		
		JSONObject dataset = synapse.createEntity("/dataset", new JSONObject("{\"name\":\"testS3\", \"parentId\":\"" + project.getString("id") + "\"}"));
		JSONObject layer = synapse.createEntity("/layer", new JSONObject("{\"name\":\"testS3\", \"type\":\"M\", \"parentId\":\"" + dataset.getString("id") + "\"}"));
		JSONObject location = synapse.uploadLayerToSynapse(layer, dataSourceFile);
		assertEquals("awss3", location.getString("type"));
		assertEquals("text/plain", location.getString("contentType"));

	    File dataDestinationFile = File.createTempFile("integrationTest", ".download");
	    dataDestinationFile.deleteOnExit();
		HttpClientHelper.downloadFile(location.getString("path"), dataDestinationFile.getAbsolutePath());
		assertTrue(dataDestinationFile.isFile());
		assertTrue(dataDestinationFile.canRead());
		assertTrue(0 < dataDestinationFile.length());
	}
}
