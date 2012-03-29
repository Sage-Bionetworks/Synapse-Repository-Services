package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client
 * is working
 * 
 * @author deflaux
 */
public class IT500SynapseJavaClient {

	private static Synapse synapse = null;
	private static Project project = null;
	private static Study dataset = null;

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
		dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = synapse.createEntity(dataset);
	}

	/**
	 * @throws Exception 
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
			String datasetId = datasets.getJSONObject(0).getString("dataset.id");

			JSONObject aStoredDataset = synapse.getEntity("/dataset/"
					+ datasetId);
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
		Study aNewDataset = new Study();
		aNewDataset.setStatus("created");
		aNewDataset.setParentId(project.getId());

		aNewDataset = synapse.createEntity(aNewDataset);
		assertEquals("created", aNewDataset.getStatus());
		aNewDataset.setStatus("updated");
		Study updatedDataset = synapse.putEntity(aNewDataset);
		assertEquals("updated", updatedDataset.getStatus());
		
		
		// Get the project using just using its ID. This is useful for cases where you
		//  do not know what you are getting until it arives.
		Project clone = (Project) synapse.getEntityById(project.getId());
		assertNotNull(clone);
		assertNotNull(clone.getEntityType());
		assertEquals(project.getId(), clone.getId());
		
		// Get the entity annotations
		Annotations annos = synapse.getAnnotations(aNewDataset.getId());
		assertNotNull(annos);
		assertEquals(aNewDataset.getId(), annos.getId());
		assertNotNull(annos.getEtag());
		// Add some values
		annos.addAnnotation("longKey", new Long(999999));
		annos.addAnnotation("blob", "This will be converted to a blob!".getBytes("UTF-8"));
		Annotations updatedAnnos = synapse.updateAnnotations(aNewDataset.getId(), annos);
		assertNotNull(updatedAnnos);
		assertEquals(aNewDataset.getId(), annos.getId());
		assertNotNull(updatedAnnos.getEtag());
		// The Etag should have changed
		assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));
		
		// Get the Users permission for this entity
		UserEntityPermissions uep = synapse.getUsersEntityPermissions(aNewDataset.getId());
		assertNotNull(uep);
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		
		// Get the path
		EntityPath path = synapse.getEntityPath(aNewDataset.getId());
		assertNotNull(path);
		assertNotNull(path.getPath());
		assertEquals(3, path.getPath().size());
		EntityHeader header = path.getPath().get(2);
		assertNotNull(header);
		assertEquals(aNewDataset.getId(), header.getId());
		
		// Get the entity headers
		List<String> entityIds = new ArrayList<String>();
		entityIds.add(project.getId());
		entityIds.add(dataset.getId());
		entityIds.add(aNewDataset.getId());
		BatchResults<EntityHeader> entityHeaders = synapse.getEntityTypeBatch(entityIds);
		assertNotNull(entityHeaders);
		assertEquals(3, entityHeaders.getTotalNumberOfResults());
		List<String> outputIds = new ArrayList<String>();
		for(EntityHeader entityHeader : entityHeaders.getResults()) {
			outputIds.add(entityHeader.getId());
		}
		assertEquals(entityIds.size(), outputIds.size());
		assertTrue(entityIds.containsAll(outputIds));
		
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaClientCRUDEntity() throws Exception {

		// Get the entity annotaions
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

		Project fromGetById = (Project) synapse.getEntityById(createdProjectId);
		assertEquals(createdProject, fromGetById);

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

		Data layer = new Data();
		layer.setType(LayerTypeNames.E);
		layer.setParentId(dataset.getId());
		layer = synapse.createEntity(layer);

		layer = (Data) synapse.uploadLocationableToSynapse(layer,
				dataSourceFile);
		
		// TODO!!!!!!!!!!!! test upload more than once, do we clutter LocationData?

		assertEquals("text/plain", layer.getContentType());
		assertNotNull(layer.getMd5());

		List<LocationData> locations = layer.getLocations();
		assertEquals(1, locations.size());
		LocationData location = locations.get(0);
		assertEquals(LocationTypeNames.awss3, location.getType());
		assertNotNull(location.getPath());
		assertTrue(location.getPath().startsWith("http"));
		
		File dataDestinationFile = File.createTempFile("integrationTest",
				".download");
		dataDestinationFile.deleteOnExit();
		HttpClientHelper.getContent(DefaultHttpClientSingleton.getInstance(), location.getPath(), dataDestinationFile);
		assertTrue(dataDestinationFile.isFile());
		assertTrue(dataDestinationFile.canRead());
		assertTrue(0 < dataDestinationFile.length());
		
		// TODO test auto versioning
		
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaDownloadExternalLayer() throws Exception {

		// Use a url that we expect to be available and whose contents we don't
		// expect to change
		String externalUrl = "http://www.sagebase.org/favicon";
		String externalUrlMD5 = "8f8e272d7fdb2fc6c19d57d00330c397";
		int externalUrlFileSizeBytes = 1150;

		LocationData externalLocation = new LocationData();
		externalLocation.setPath(externalUrl);
		externalLocation.setType(LocationTypeNames.external);
		List<LocationData> locations = new ArrayList<LocationData>();
		locations.add(externalLocation);

		Data layer = new Data();
		layer.setType(LayerTypeNames.M);
		layer.setMd5(externalUrlMD5);
		layer.setParentId(dataset.getId());
		layer.setLocations(locations);
		layer = synapse.createEntity(layer);

		File downloadedLayer = synapse.downloadLocationableFromSynapse(layer);
		assertEquals(externalUrlFileSizeBytes, downloadedLayer.length());

	}
	
	
	/**
	 * tests signing requests using an API key, as an alternative to logging in
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAPIKey() throws Exception {
		// get API key for integration test user
		// must be logged in to do this, so use the global client 'synapse'
		JSONObject keyJson = synapse.getSynapseEntity(StackConfiguration
				.getAuthenticationServicePrivateEndpoint(), "/secretKey");
		String apiKey = keyJson.getString("secretKey");
		assertNotNull(apiKey);
		// set user name and api key in a synapse client
		// we don't want to log-in, so use a new Synapse client instance
		Synapse synapseNoLogin = new Synapse();
		synapseNoLogin.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapseNoLogin.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapseNoLogin.setUserName(StackConfiguration.getIntegrationTestUserOneName());
		synapseNoLogin.setApiKey(apiKey);
		// now try to do a query
		JSONObject results = synapseNoLogin.query("select * from dataset limit 10");
		// should get at least one result (since 'beforeClass' makes one dataset)
		assertTrue(0 < results.getInt("totalNumberOfResults"));
	}
	
	@Test
	public void testSignTermsOfUse() throws Exception {
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword(), /*acceptTermsOfUse*/true);

	}
	
	@Test
	public void testRetrieveSynapseTOU() throws Exception {
		String termsOfUse = synapse.getSynapseTermsOfUse();
		assertNotNull(termsOfUse);
		assertTrue(termsOfUse.length()>100);
	}
	
	/**
	 * Test that we can add an attachment to a project and then get it back.
	 * 
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Test
	public void testAttachemtnsRoundTrip() throws IOException, JSONObjectAdapterException, SynapseException{
		// First create a temp file
		File temp = File.createTempFile("AttachmentTest", ".txt");
		File tempDownload = File.createTempFile("AttachmentTestDownload", ".txt");
		FileOutputStream writer = null;
		FileInputStream reader = null;
		try{
			String fileContents = "I am some text in a file";
			byte[] fileBytes = fileContents.getBytes("UTF-8");
			writer = new FileOutputStream(temp);
			writer.write(fileBytes);
			writer.close();
			// Get the md5
//			String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileBytes);
			
			// We are now ready to add this file as an attachment on the project
			AttachmentData data = synapse.uploadAttachmentToSynapse(project.getId(), temp);
			// Save this this attachment on the entity.
			project.setAttachments(new ArrayList<AttachmentData>());
			project.getAttachments().add(data);
			// Save this attachment to the project
			project = synapse.putEntity(project);
			assertNotNull(project.getAttachments());
			assertEquals(1, project.getAttachments().size());
			AttachmentData clone = project.getAttachments().get(0);
			assertEquals(data, clone);
			
			// Now make sure we can downlaod our
			synapse.downlaodEntityAttachment(project.getId(), clone, tempDownload);
			assertTrue(tempDownload.exists());
			assertTrue(tempDownload.length() == fileBytes.length);
			// Now make sure the contents are what we expect
			reader = new FileInputStream(tempDownload);
			byte[] bytes = new byte[fileBytes.length];
			reader.read(bytes);
			String downloadedText = new String(bytes, "UTF-8");
			assertEquals(fileContents, downloadedText);
		}finally{
			if(writer != null){
				writer.close();
			}
			if(reader != null){
				reader.close();
			}
			temp.delete();
			tempDownload.delete();
		}
	}
	

}
