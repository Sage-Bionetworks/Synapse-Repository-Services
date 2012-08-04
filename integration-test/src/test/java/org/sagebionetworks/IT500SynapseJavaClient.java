package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.EntityBundle;
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
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
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
	}
	
	@Before
	public void before() throws SynapseException {
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
	@After
	public void after() throws Exception {
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
	
	// for entities like our project, which have 'root' as parent
	// it should not be possible to delete the ACL
	@Test
	public void testNoDeleteACLOnProject() throws Exception {
		// Get the Users permission for this entity
		UserEntityPermissions uep = synapse.getUsersEntityPermissions(project.getId());
		// first, we CAN change permissions (i.e. edit the ACL)
		assertTrue(uep.getCanChangePermissions());
		// but we CAN'T just delete the ACL
		assertFalse(uep.getCanEnableInheritance());
		// and if we try, it won't work
		try {
			synapse.deleteACL(project.getId());
			fail("exception expected");
		} catch (SynapseForbiddenException e) {
			// as expected
		}
	}
	
	// PLFM-412 said there was an error when 'PUBLIC' was given an empty array of permissions
	@Test(expected=SynapseBadRequestException.class)
	public void testEmptyACLAccessTypeList() throws Exception {
		AccessControlList acl = synapse.getACL(project.getId());
		List<UserGroup> ugs = synapse.getGroups(0, 100).getResults();
		Long publicPrincipalId = null;
		for (UserGroup ug: ugs) {
			if (ug.getName().equals("PUBLIC")) {
				publicPrincipalId = Long.parseLong(ug.getId());
				break;
			}
		}
		assertTrue(publicPrincipalId!=null);
		boolean foundIt = false;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getPrincipalId().equals(publicPrincipalId)) {
				foundIt = true;
				ra.setAccessType(new HashSet<ACCESS_TYPE>()); // make it an empty list
				break;
			}
		}
		if (!foundIt) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(publicPrincipalId);
			ra.setAccessType(new HashSet<ACCESS_TYPE>()); // make it an empty list
			acl.getResourceAccess().add(ra);
		}
		// now push it, should get a SynapseBadRequestException
		synapse.updateACL(acl);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testJavaClientCRUD() throws Exception {
		Study aNewDataset = new Study();
		aNewDataset.setParentId(project.getId());

		aNewDataset = synapse.createEntity(aNewDataset);
		
		// Get the project using just using its ID. This is useful for cases where you
		//  do not know what you are getting until it arrives.
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
		assertEquals(true, synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.UPDATE));
		assertEquals(true, synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.READ));
		assertTrue(uep.getCanChangePermissions());
		assertTrue(uep.getCanEnableInheritance());
		
		UserProfile profile = synapse.getMyProfile();
		assertNotNull(profile);
		
		assertEquals(profile.getOwnerId(), uep.getOwnerPrincipalId().toString());
		
		// should be able to download
		assertTrue(synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// now add a ToU restriction
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setEntityIds(Arrays.asList(new String[]{aNewDataset.getId()}));
		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("play nice");
		ar = synapse.createAccessRequirement(ar);
		
		// should not be able to download
		assertFalse(synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		
		VariableContentPaginatedResults<AccessRequirement> vcpr = synapse.getUnmetAccessReqAccessRequirements(aNewDataset.getId());
		assertEquals(1, vcpr.getResults().size());
		
		// now add the ToU approval
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(profile.getOwnerId());
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(ar.getId());
		
		synapse.createAccessApproval(aa);
		
		vcpr = synapse.getUnmetAccessReqAccessRequirements(aNewDataset.getId());
		assertEquals(0, vcpr.getResults().size());
		
		// should be able to download
		assertTrue(synapse.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// ACL should reflect this information
		AccessControlList acl = synapse.getACL(project.getId());
		Set<ResourceAccess> ras = acl.getResourceAccess();
		boolean foundit = false;
		List<Long> foundPrincipals = new ArrayList<Long>();
		for (ResourceAccess ra : ras) {
			assertNotNull(ra.getPrincipalId());
			assertNull(ra.getGroupName()); // deprecated, so should be null
			foundPrincipals.add(ra.getPrincipalId());
			if (ra.getPrincipalId().equals(Long.parseLong(profile.getOwnerId()))) {
				foundit=true;
				Set<ACCESS_TYPE> ats = ra.getAccessType();
				assertTrue(ats.contains(ACCESS_TYPE.READ));
				assertTrue(ats.contains(ACCESS_TYPE.UPDATE));
			}
		}
		assertTrue("didn't find "+profile.getDisplayName()+"("+profile.getOwnerId()+") but found "+foundPrincipals, foundit);
		
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
	
	@Test
	public void testJavaClientGetEntityBundle() throws SynapseException {
		int offset = Integer.parseInt(ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW);
		int limit = Integer.parseInt(ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM);
		
		Annotations annos = synapse.getAnnotations(project.getId());
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		annos = synapse.updateAnnotations(project.getId(), annos);
		
		AccessControlList acl = synapse.getACL(project.getId());
		acl.setCreatedBy("John Doe");
		acl.setId(project.getId());
		synapse.updateACL(acl);
			
		int allPartsMask = EntityBundle.ENTITY |
				EntityBundle.ANNOTATIONS |
				EntityBundle.PERMISSIONS |
				EntityBundle.ENTITY_PATH |
				EntityBundle.ENTITY_REFERENCEDBY |
				EntityBundle.CHILD_COUNT |
				EntityBundle.ACL |
				EntityBundle.USERS |
				EntityBundle.GROUPS;
		
		long startTime = System.nanoTime();
		EntityBundle entityBundle = synapse.getEntityBundle(project.getId(), allPartsMask);
		long endTime = System.nanoTime();
		long requestTime = (endTime - startTime) / 1000000;
		System.out.println("Bundle request time was " + requestTime + " ms");
		
		
		assertEquals("Invalid fetched Entity in the EntityBundle", 
				synapse.getEntityById(project.getId()), entityBundle.getEntity());
		assertEquals("Invalid fetched Annotations in the EntityBundle", 
				synapse.getAnnotations(project.getId()), entityBundle.getAnnotations());
		assertEquals("Invalid fetched EntityPath in the EntityBundle", 
				synapse.getEntityPath(project.getId()), entityBundle.getPath());
		assertEquals("Invalid fetched ReferencedBy in the EntityBundle", 
				synapse.getEntityReferencedBy(project), entityBundle.getReferencedBy());
		assertEquals("Invalid fetched ChildCount in the EntityBundle", 
				synapse.getChildCount(project.getId()), entityBundle.getChildCount());
		assertEquals("Invalid fetched ACL in the EntityBundle", 
				synapse.getACL(project.getId()), entityBundle.getAccessControlList());
		assertEquals("Invalid fetched Users in the EntityBundle", 
				synapse.getUsers(offset, limit), entityBundle.getUsers());
		assertEquals("Invalid fetched Groups in the EntityBundle", 
				synapse.getGroups(offset, limit), entityBundle.getGroups());
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
	@Ignore // This test is not stable.  It has caused two builds to fail with: java.net.SocketTimeoutException: Read timed out
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
		
	@Test
	public void testGetUsers() throws Exception {
		UserProfile myProfile = synapse.getMyProfile();
		assertNotNull(myProfile);
		String myPrincipalId = myProfile.getOwnerId();
		assertNotNull(myPrincipalId);

		PaginatedResults<UserProfile> users = synapse.getUsers(0,1000);
		assertTrue(users.getResults().size()>0);
		boolean foundSelf = false;
		List<String> allDisplayNames = new ArrayList<String>();
		for (UserProfile up : users.getResults()) {
			assertNotNull(up.getOwnerId());
			String displayName = up.getDisplayName();
			allDisplayNames.add(displayName);
			if (up.getOwnerId().equals(myPrincipalId)) foundSelf=true;
		}
		assertTrue("Didn't find self, only found "+allDisplayNames, foundSelf);
	}
	
	@Test
	public void testGetGroups() throws Exception {
		PaginatedResults<UserGroup> groups = synapse.getGroups(0,100);
		assertTrue(groups.getResults().size()>0);
		for (UserGroup ug : groups.getResults()) {
			assertNotNull(ug.getId());
			assertNotNull(ug.getName());
		}
	}
	
	@Test
	public void testAccessRequirement() throws Exception {
		// create a node
		Data layer = new Data();
		layer.setType(LayerTypeNames.E);
		layer.setParentId(dataset.getId());
		layer = synapse.createEntity(layer);

		assertTrue(synapse.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// add an access requirement
		TermsOfUseAccessRequirement r = new TermsOfUseAccessRequirement();
		r.setEntityIds(Arrays.asList(new String[]{layer.getId()}));
		r.setAccessType(ACCESS_TYPE.DOWNLOAD);
		r.setTermsOfUse("I promise to be good.");
		synapse.createAccessRequirement(r);
		
		// check that can't download
		assertFalse(synapse.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));

		// get unmet access requirements
		PaginatedResults<AccessRequirement> ars = synapse.getUnmetAccessReqAccessRequirements(layer.getId());
		assertEquals(1, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
		AccessRequirement clone = ars.getResults().get(0);
		assertEquals(r.getEntityType(), clone.getEntityType());
		assertTrue(clone instanceof TermsOfUseAccessRequirement);
		assertEquals(r.getTermsOfUse(), ((TermsOfUseAccessRequirement)clone).getTermsOfUse());
		
		// create approval for the requirement
		TermsOfUseAccessApproval approval = new TermsOfUseAccessApproval();
		UserProfile profile = synapse.getMyProfile();
		assertNotNull(profile);
		assertNotNull(profile.getOwnerId());
		approval.setAccessorId(profile.getOwnerId());
		approval.setRequirementId(clone.getId());
		synapse.createAccessApproval(approval);
		
		// get unmet requirements -- should be empty
		ars = synapse.getUnmetAccessReqAccessRequirements(layer.getId());
		assertEquals(0, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
		
		// check that CAN download
		assertTrue(synapse.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));
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
	public void testAttachmentsImageRoundTrip() throws IOException, JSONObjectAdapterException, SynapseException{
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
			assertTrue("A preview size should not exceed 100KB.  This one is "+previewDownload.length(), previewDownload.length() < 100*1000);
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

	/**
	 * Test that we can add an attachment to a project and then get it back.
	 * 
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	@Test
	public void testProfileImageRoundTrip() throws IOException, JSONObjectAdapterException, SynapseException{
		// First load an image from the classpath
		String fileName = "images/profile_pic.png";
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
			
			UserProfile profile = synapse.getMyProfile();
			AttachmentData data = synapse.uploadUserProfileAttachmentToSynapse(profile.getOwnerId(), originalFile, finalName);
			//save this as part of the user
			profile.setPic(data);
			synapse.updateMyProfile(profile);
			
			//download, and check that it was updated
			profile = synapse.getMyProfile();
			AttachmentData clone = profile.getPic();
			assertEquals(finalName, data.getName());
			assertEquals(data.getName(), clone.getName());
			assertEquals(data.getMd5(), clone.getMd5());
			assertEquals(data.getContentType(), clone.getContentType());
			assertEquals(data.getTokenId(), clone.getTokenId());
			// the attachment should have preview
			assertNotNull(clone.getPreviewId());
			// Now make sure we can download our
			
			synapse.downloadUserProfileAttachment(profile.getOwnerId(), clone, attachmentDownload);
			assertTrue(attachmentDownload.exists());
			System.out.println(attachmentDownload.getAbsolutePath());
			assertEquals(originalFile.length(), attachmentDownload.length());
			// Now make sure we can get the preview image
			// Before we download the preview make sure it exists
			synapse.waitForUserProfilePreviewToBeCreated(profile.getOwnerId(), clone.getPreviewId(), PREVIEW_TIMOUT);
			synapse.downloadUserProfileAttachmentPreview(profile.getOwnerId(), clone.getPreviewId(), previewDownload);
			assertTrue(previewDownload.exists());
			System.out.println(previewDownload.getAbsolutePath());
			assertTrue(previewDownload.length() > 0);
			assertTrue("A preview size should not exceed 100KB.  This one is "+previewDownload.length(), previewDownload.length() < 100*1000);
		}
		finally{
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
		// Test that the hack for PLFM-1287 is still in place.
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
	
	@Test
	public void testPLFM_1272() throws Exception{
		// Now add a data object 
		Data data = new Data();
		data.setParentId(project.getId());
		data = synapse.createEntity(data);

		// Now query for the data object
		String queryString = "SELECT id, name FROM data WHERE data.parentId == \""+project.getId()+"\"";
		JSONObject results = synapse.query(queryString);
		assertNotNull(results);
		assertTrue(results.has("totalNumberOfResults"));
		assertEquals(1l, results.getLong("totalNumberOfResults"));
		
		queryString = "SELECT id, name FROM layer WHERE layer.parentId == \""+project.getId()+"\"";
		results = synapse.query(queryString);
		assertNotNull(results);
		assertTrue(results.has("totalNumberOfResults"));
		assertEquals(1l, results.getLong("totalNumberOfResults"));
	}
	
	@Test
	public void testGetAllUserAndGroupIds() throws SynapseException{
		HashSet<String> expected = new HashSet<String>();
		// Get all the users
		PaginatedResults<UserProfile> pr = synapse.getUsers(0, Integer.MAX_VALUE);
		for(UserProfile up : pr.getResults()){
			expected.add(up.getOwnerId());
		}
		PaginatedResults<UserGroup> groupPr = synapse.getGroups(0, Integer.MAX_VALUE);
		for(UserGroup ug : groupPr.getResults()){
			expected.add(ug.getId());
		}
		Set<String> results = synapse.getAllUserAndGroupIds();
		assertNotNull(results);
		assertEquals(expected.size(), results.size());
		assertEquals(expected,results);
		
	}
}
