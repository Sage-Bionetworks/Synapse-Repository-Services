package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * @author deflaux
 */
public class IT500SynapseJavaClient {
	
	private static Synapse synapse = null;
	private static Project project = null;
	private static Dataset dataset = null;

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
		
		project = synapse.createEntity(new Project());
		dataset = new Dataset();
		dataset.setParentId(project.getId());
		dataset = synapse.createEntity(dataset);
	}

	/**
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@AfterClass
	public static void afterClass() throws Exception {
		if(null != project) {
			synapse.deleteEntity(project);
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

			JSONObject aStoredDataset = synapse.getEntity("/dataset/" + datasetId);
			assertTrue(aStoredDataset.has("annotations"));

			JSONObject annotations = synapse.getEntity(aStoredDataset
					.getString("annotations"));
			assertTrue(annotations.has("stringAnnotations"));
			assertTrue(annotations.has("dateAnnotations"));
			assertTrue(annotations.has("longAnnotations"));
			assertTrue(annotations.has("doubleAnnotations"));
			assertTrue(annotations.has("blobAnnotations"));
		}
	}
	
	/**
	 * @throws Exception
	 */
	@Test 
	public void testJavaClientCRUD() throws Exception {
		JSONObject aNewDataset = synapse.createEntity("/dataset", new JSONObject("{\"name\":\"testCrud\", \"status\": \"created\", \"parentId\":\"" + project.getId() + "\"}"));
		assertEquals("created", aNewDataset.getString("status"));
		aNewDataset.put("status", "updated");
		JSONObject updatedDataset = synapse.updateEntity(aNewDataset.getString("uri"), aNewDataset);
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
	public void testJavaClientCreateEntity() throws Exception {
		Project newProject = new Project();
		newProject.setParentId(project.getId());
		Project createdProject = synapse.createEntity(newProject);		
		assertNotNull(createdProject);
		assertNotNull(createdProject.getId());
		assertNotNull(createdProject.getUri());
		
		String createdProjectId = createdProject.getId();
		Project fromGet = synapse.getEntity(createdProjectId, Project.class);
		assertEquals(createdProject, fromGet);
		
		Project fromGetById = (Project)synapse.getEntityById(createdProjectId);
		assertEquals(createdProject, fromGetById);
		
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
		
		Layer layer = new Layer();
		layer.setType(LayerTypeNames.E);
		layer.setParentId(dataset.getId());
		layer = synapse.createEntity(layer);
		
		Location location = synapse.uploadLocationableToSynapse(layer, dataSourceFile);
		assertEquals(LocationTypeNames.awss3, location.getType());
		assertEquals("text/plain", location.getContentType());

	    File dataDestinationFile = File.createTempFile("integrationTest", ".download");
	    dataDestinationFile.deleteOnExit();
		HttpClientHelper.downloadFile(DefaultHttpClientSingleton.getInstance(), location.getPath(), dataDestinationFile.getAbsolutePath());
		assertTrue(dataDestinationFile.isFile());
		assertTrue(dataDestinationFile.canRead());
		assertTrue(0 < dataDestinationFile.length());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaDownloadExternalLayer() throws Exception {

	    // Use a url that we expect to be available and whose contents we don't expect to change
	    String externalUrl = "http://www.sagebase.org/favicon";
	    String externalUrlMD5 = "8f8e272d7fdb2fc6c19d57d00330c397";
	    int externalUrlFileSizeBytes = 1150; 
	    
	    LocationData externalLocation = new LocationData();
	    externalLocation.setPath(externalUrl);
	    externalLocation.setMd5(externalUrlMD5);
	    List<LocationData> locations = new ArrayList<LocationData>();
	    locations.add(externalLocation);
	    
		Layer layer = new Layer();
		layer.setType(LayerTypeNames.M);
		layer.setParentId(dataset.getId());
		layer.setLocations(locations);
		layer = synapse.createEntity(layer);
		
		File downloadedLayer = synapse.downloadLocationableFromSynapse(layer);
		assertEquals(externalUrlFileSizeBytes, downloadedLayer.length());
		
	}
}
