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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpException;
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
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
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
	
	public static final int PREVIEW_TIMOUT = 10*1000;

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

			JSONObject aStoredDataset = synapse.getEntity("/entity/"
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
		aNewDataset.setParentId(project.getId());

		aNewDataset = synapse.createEntity(aNewDataset);
		Study updatedDataset = synapse.putEntity(aNewDataset);
		
		
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
		
		// ACL should reflect this information
		AccessControlList acl = synapse.getACL(project.getId());
		Set<ResourceAccess> ras = acl.getResourceAccess();
		boolean foundit = false;
		for (ResourceAccess ra : ras) {
			if (ra.getGroupName().equals(StackConfiguration.getIntegrationTestUserOneName())) {
				foundit=true;
				Set<ACCESS_TYPE> ats = ra.getAccessType();
				assertTrue(ats.contains(ACCESS_TYPE.READ));
				assertTrue(ats.contains(ACCESS_TYPE.UPDATE));
			}
		}
		assertTrue(foundit);
		
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
	public void testAttachemtnsImageRoundTrip() throws IOException, JSONObjectAdapterException, SynapseException{
		// First load an image from the classpath
		String fileName = "images/IMAG0019.jpg";
		URL url = IT500SynapseJavaClient.class.getClassLoader().getResource(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", url);
		File originalFile = new File(url.getFile());
		File attachmentDownload = File.createTempFile("AttachmentTestDownload", ".tmp");
		File previewDownload = File.createTempFile("AttachmentPreviewDownload", ".png");
		FileOutputStream writer = null;
		FileInputStream reader = null;
		try{
			// We are now ready to add this file as an attachment on the project
			String finalName = "iamgeFile.jpg";
			AttachmentData data = synapse.uploadAttachmentToSynapse(project.getId(), originalFile, finalName);
			assertEquals(finalName, data.getName());
			// Save this this attachment on the entity.
			project.setAttachments(new ArrayList<AttachmentData>());
			project.getAttachments().add(data);
			// Save this attachment to the project
			project = synapse.putEntity(project);
			assertNotNull(project.getAttachments());
			assertEquals(1, project.getAttachments().size());
			AttachmentData clone = project.getAttachments().get(0);
			assertEquals(data.getName(), clone.getName());
			assertEquals(data.getMd5(), clone.getMd5());
			assertEquals(data.getContentType(), clone.getContentType());
			assertEquals(data.getTokenId(), clone.getTokenId());
			// the attachment should have preview
			assertNotNull(clone.getPreviewId());
			// Now make sure we can download our
			synapse.downloadEntityAttachment(project.getId(), clone, attachmentDownload);
			assertTrue(attachmentDownload.exists());
			System.out.println(attachmentDownload.getAbsolutePath());
			assertEquals(originalFile.length(), attachmentDownload.length());
			// Now make sure we can get the preview image
			// Before we download the preview make sure it exists
			synapse.waitForPreviewToBeCreated(project.getId(), clone.getPreviewId(), PREVIEW_TIMOUT);
			synapse.downloadEntityAttachmentPreview(project.getId(), clone.getPreviewId(), previewDownload);
			assertTrue(previewDownload.exists());
			System.out.println(previewDownload.getAbsolutePath());
			assertTrue(previewDownload.length() > 0);
			assertTrue("A preview size should not exceed 100KB", previewDownload.length() < 100*1000);
		}finally{
			if(writer != null){
				writer.close();
			}
			if(reader != null){
				reader.close();
			}
			attachmentDownload.delete();
			previewDownload.delete();
		}
	}
	
	@Test	
	public void testGetChildCount() throws SynapseException{
		// Start with no children.

		// Add a child.
		Project child = new Project();
		child.setName("childFolder");
		child.setParentId(project.getId());
		child = synapse.createEntity(child);
		assertNotNull(child);
		assertNotNull(child.getId());
		assertEquals(project.getId(), child.getParentId());
		
		// This folder should have no children
		Long count = synapse.getChildCount(child.getId());
		assertEquals(new Long(0), count);
		// Now add a child
		Project grandChild = new Project();
		grandChild.setName("childFolder");
		grandChild.setParentId(child.getId());
		grandChild = synapse.createEntity(grandChild);
		assertNotNull(grandChild);
		assertNotNull(grandChild.getId());
		assertEquals(child.getId(), grandChild.getParentId());
		// The count should now be one.
		count = synapse.getChildCount(child.getId());
		assertEquals(new Long(1), count);
	}
	
	/**
	 * PLFM-1166 annotations are not being returned from queries.
	 * @throws SynapseException 
	 * @throws JSONException 
	 */
	@Test 
	public void testPLMF_1166() throws SynapseException, JSONException{
		// Get the project annotations
		Annotations annos = synapse.getAnnotations(project.getId());
		String key = "PLFM_1166";
		annos.addAnnotation(key, "one");
		synapse.updateAnnotations(project.getId(), annos);
		// Make sure we can query for 
		String query = "select id, "+key+" from project where id == '"+project.getId()+"'";
		JSONObject total = synapse.query(query);
		System.out.println(total);
		assertEquals(1l, total.getLong("totalNumberOfResults"));
		assertNotNull(total);
		assertTrue(total.has("results"));
		JSONArray array = total.getJSONArray("results");
		JSONObject row = array.getJSONObject(0);
		assertNotNull(row);
		String fullKey = "project."+key;
		assertFalse("Failed to get an annotation back with 'select annotaionName'", row.isNull(fullKey));
		JSONArray valueArray = row.getJSONArray(fullKey);
		assertNotNull(valueArray);
		assertEquals(1, valueArray.length());
		assertEquals("one", valueArray.get(0));
		System.out.println(array);
		
	}

	/**
	 * PLFM-1212 Links need to return what they reference.
	 * @throws SynapseException 
	 * @throws JSONException 
	 */
	@Test 
	public void testPLFM_1212() throws SynapseException, JSONException{
		// The dataset should start with no references
		PaginatedResults<EntityHeader> refs = synapse.getEntityReferencedBy(dataset);
		assertNotNull(refs);
		assertEquals(0l, refs.getTotalNumberOfResults());
		// Add a link to the dataset in the project
		Link link = new Link();
		Reference ref = new Reference();
		ref.setTargetId(dataset.getId());
		link.setLinksTo(ref);
		link.setLinksToClassName(Study.class.getName());
		link.setParentId(project.getId());
		// Create the link
		link = synapse.createEntity(link);
		// Get 
		refs = synapse.getEntityReferencedBy(dataset);
		assertNotNull(refs);
		assertEquals(1l, refs.getTotalNumberOfResults());
		assertNotNull(refs.getResults());
		assertEquals(1, refs.getResults().size());
		assertEquals(project.getId(), refs.getResults().get(0).getId());
		
	}
	
	/**
	 * Test for PLFM-1214
	 */
	@Test 
	public void testPLFM_1214() throws SynapseException, JSONException{
		// The dataset should start with no references
		dataset.setNumSamples(123l);
		dataset = synapse.putEntity(dataset);
		assertEquals(new Long(123), dataset.getNumSamples());
		// Now clear out the value
		dataset.setNumSamples(null);
		dataset = synapse.putEntity(dataset);
		assertEquals(null, dataset.getNumSamples());
	}
}
