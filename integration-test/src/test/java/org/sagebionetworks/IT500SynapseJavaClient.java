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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;
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

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseClient synapseTwo;
	private static Long user1ToDelete;
	private static Long user2ToDelete;
	
	private static final int PREVIEW_TIMOUT = 10*1000;
	private static final int RDS_WORKER_TIMEOUT = 1000*60; // One min
	
	private List<String> toDelete;
	private List<Long> accessRequirementsToDelete;
	private List<String> handlesToDelete;
	private List<String> teamsToDelete;
	private Project project;
	private Study dataset;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		synapseOne = new SynapseClientImpl();
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		synapseTwo = new SynapseClientImpl();
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo);
		
		// Update this user's profile to contain a display name
		UserProfile profile = synapseTwo.getMyProfile();
		synapseTwo.updateMyProfile(profile);
	}
	
	@Before
	public void before() throws SynapseException {
		toDelete = new ArrayList<String>();
		accessRequirementsToDelete = new ArrayList<Long>();
		handlesToDelete = new ArrayList<String>();
		teamsToDelete = new ArrayList<String>();
		
		project = synapseOne.createEntity(new Project());
		dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = synapseOne.createEntity(dataset);
		
		toDelete.add(project.getId());
		toDelete.add(dataset.getId());
		
		
		// there shouldn't be any Teams floating around in the system
		// the Team tests assume there are no Teams to start.  So before
		// running the tests, we clean up all the teams
		long numTeams = 0L;
		do {
			PaginatedResults<Team> teams = synapseOne.getTeams(null, 10, 0);
			numTeams = teams.getTotalNumberOfResults();
			for (Team team : teams.getResults()) {
				synapseOne.deleteTeam(team.getId());
			}
		} while (numTeams>0);
	}
	
	@After
	public void after() throws Exception {
		for (String id: toDelete) {
			try {
				adminSynapse.deleteAndPurgeEntityById(id);
			} catch (SynapseNotFoundException e) {}
		}

		for (Long id : accessRequirementsToDelete) {
			try {
				adminSynapse.deleteAccessRequirement(id);
			} catch (SynapseNotFoundException e) {}
		}

		for (String id : handlesToDelete) {
			try {
				adminSynapse.deleteFileHandle(id);
			} catch (SynapseNotFoundException e) {
			} catch (SynapseServiceException e) { }
		}
		
		for (String id : teamsToDelete) {
			try {
				adminSynapse.deleteTeam(id);
			} catch (SynapseNotFoundException e) {}
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseServiceException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseServiceException e) { }
	}
	
	@Test
	public void testCheckAliasAvailable() throws SynapseException{
		AliasCheckRequest request = new AliasCheckRequest();
		// This is valid but already in use
		request.setAlias("public");
		request.setType(AliasType.TEAM_NAME);
		AliasCheckResponse response = synapseOne.checkAliasAvailable(request);
		assertNotNull(response);
		assertTrue(response.getValid());
		assertFalse("The 'public' group name should already have this alias so it cannot be available!",response.getAvailable());
	}
	
	@Test
	public void testJavaClientGetADataset() throws Exception {
		JSONObject results = synapseOne.query("select * from dataset limit 10");

		assertTrue(0 <= results.getInt("totalNumberOfResults"));

		JSONArray datasets = results.getJSONArray("results");

		if (0 < datasets.length()) {
			String datasetId = datasets.getJSONObject(0).getString("dataset.id");

			Data aStoredDataset = synapseOne.getEntity(datasetId, Data.class);
			assertNotNull(aStoredDataset.getAnnotations());

			Annotations annos = synapseOne.getAnnotations(datasetId);
			assertNotNull(annos.getStringAnnotations());
			assertNotNull(annos.getDateAnnotations());
			assertNotNull(annos.getLongAnnotations());
			assertNotNull(annos.getDoubleAnnotations());
			assertNotNull(annos.getBlobAnnotations());
		}
	}
	
	@Test
	public void testNoDeleteACLOnProject() throws Exception {
		// for entities like our project, which have 'root' as parent
		// it should not be possible to delete the ACL
		
		// Get the Users permission for this entity
		UserEntityPermissions uep = synapseOne.getUsersEntityPermissions(project.getId());
		// first, we CAN change permissions (i.e. edit the ACL)
		assertTrue(uep.getCanChangePermissions());
		// but we CAN'T just delete the ACL
		assertFalse(uep.getCanEnableInheritance());
		// and if we try, it won't work
		try {
			synapseOne.deleteACL(project.getId());
			fail("exception expected");
		} catch (SynapseForbiddenException e) {
			// as expected
		}
	}
	
	@Test(expected=SynapseBadRequestException.class)
	public void testEmptyACLAccessTypeList() throws Exception {
		// PLFM-412 said there was an error when 'PUBLIC' was given an empty array of permissions
		AccessControlList acl = synapseOne.getACL(project.getId());
		List<UserGroup> ugs = synapseOne.getGroups(0, 100).getResults();
		Long publicPrincipalId = null;
		for (UserGroup ug: ugs) {
			if (ug.getId().equals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString())) {
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
		synapseOne.updateACL(acl);
	}

	@Test
	public void testJavaClientCRUD() throws Exception {
		Study aNewDataset = new Study();
		aNewDataset.setParentId(project.getId());

		aNewDataset = synapseOne.createEntity(aNewDataset);
		
		// Get the project using just using its ID. This is useful for cases where you
		//  do not know what you are getting until it arrives.
		Project clone = (Project) synapseOne.getEntityById(project.getId());
		assertNotNull(clone);
		assertNotNull(clone.getEntityType());
		assertEquals(project.getId(), clone.getId());
		
		// Get the entity annotations
		Annotations annos = synapseOne.getAnnotations(aNewDataset.getId());
		assertNotNull(annos);
		assertEquals(aNewDataset.getId(), annos.getId());
		assertNotNull(annos.getEtag());
		// Add some values
		annos.addAnnotation("longKey", new Long(999999));
		annos.addAnnotation("blob", "This will be converted to a blob!".getBytes("UTF-8"));
		Annotations updatedAnnos = synapseOne.updateAnnotations(aNewDataset.getId(), annos);
		assertNotNull(updatedAnnos);
		assertEquals(aNewDataset.getId(), annos.getId());
		assertNotNull(updatedAnnos.getEtag());
		// The Etag should have changed
		assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));

		// Get the "zero" e-tag for specific versions. See PLFM-1420.
		Entity datasetEntity = synapseOne.getEntityByIdForVersion(aNewDataset.getId(), aNewDataset.getVersionNumber());
		assertTrue(NodeConstants.ZERO_E_TAG.equals(datasetEntity.getEtag()));

		// Get the Users permission for this entity
		UserEntityPermissions uep = synapseOne.getUsersEntityPermissions(aNewDataset.getId());
		assertNotNull(uep);
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, synapseOne.canAccess(aNewDataset.getId(), ACCESS_TYPE.UPDATE));
		assertEquals(true, synapseOne.canAccess(aNewDataset.getId(), ACCESS_TYPE.READ));
		assertTrue(uep.getCanChangePermissions());
		assertTrue(uep.getCanEnableInheritance());
		
		UserProfile profile = synapseOne.getMyProfile();
		assertNotNull(profile);
		
		assertEquals(profile.getOwnerId(), uep.getOwnerPrincipalId().toString());
		
		// should be able to download
		assertTrue(synapseOne.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// now add a ToU restriction
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();

		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(aNewDataset.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("play nice");
		ar = adminSynapse.createAccessRequirement(ar);
		accessRequirementsToDelete.add(ar.getId());
		
		UserProfile otherProfile = synapseOne.getMyProfile();
		assertNotNull(otherProfile);
		
		// should not be able to download
		assertFalse(synapseTwo.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.ENTITY);
		subjectId.setId(aNewDataset.getId());
		VariableContentPaginatedResults<AccessRequirement> vcpr = synapseTwo.getUnmetAccessRequirements(subjectId);
		assertEquals(1, vcpr.getResults().size());
		
		// now add the ToU approval
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(otherProfile.getOwnerId());
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(ar.getId());
		
		synapseTwo.createAccessApproval(aa);
		
		vcpr = synapseTwo.getUnmetAccessRequirements(subjectId);
		assertEquals(0, vcpr.getResults().size());
		
		// should be able to download
		assertTrue(synapseTwo.canAccess(aNewDataset.getId(), ACCESS_TYPE.DOWNLOAD));
		
		ar.setTermsOfUse("play nicer");
		ar = adminSynapse.updateAccessRequirement(ar);
		assertEquals("play nicer", ar.getTermsOfUse());
		
		// ACL should reflect the first User's permission
		AccessControlList acl = synapseOne.getACL(project.getId());
		Set<ResourceAccess> ras = acl.getResourceAccess();
		boolean foundit = false;
		List<Long> foundPrincipals = new ArrayList<Long>();
		for (ResourceAccess ra : ras) {
			assertNotNull(ra.getPrincipalId());
			foundPrincipals.add(ra.getPrincipalId());
			if (ra.getPrincipalId().equals(Long.parseLong(profile.getOwnerId()))) {
				foundit=true;
				Set<ACCESS_TYPE> ats = ra.getAccessType();
				assertTrue(ats.contains(ACCESS_TYPE.READ));
				assertTrue(ats.contains(ACCESS_TYPE.UPDATE));
			}
		}
		assertTrue("didn't find "+profile.getUserName()+"("+profile.getOwnerId()+") but found "+foundPrincipals, foundit);
		
		// Get the path
		EntityPath path = synapseOne.getEntityPath(aNewDataset.getId());
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
		BatchResults<EntityHeader> entityHeaders = synapseOne.getEntityTypeBatch(entityIds);
		assertNotNull(entityHeaders);
		assertEquals(3, entityHeaders.getTotalNumberOfResults());
		List<String> outputIds = new ArrayList<String>();
		for(EntityHeader entityHeader : entityHeaders.getResults()) {
			outputIds.add(entityHeader.getId());
		}
		assertEquals(entityIds.size(), outputIds.size());
		assertTrue(entityIds.containsAll(outputIds));
		
	}

	@Test
	public void testJavaClientCreateEntity() throws Exception {
		Study study = new Study();
		study.setParentId(project.getId());
		Study createdStudy = synapseOne.createEntity(study);
		assertNotNull(createdStudy);
		assertNotNull(createdStudy.getId());
		assertNotNull(createdStudy.getUri());

		String createdProjectId = createdStudy.getId();
		Study fromGet = synapseOne.getEntity(createdProjectId, Study.class);
		assertEquals(createdStudy, fromGet);

		Study fromGetById = (Study) synapseOne.getEntityById(createdProjectId);
		assertEquals(createdStudy, fromGetById);

	}

	@Test
	public void testJavaClientGetEntityBundle() throws SynapseException {		
		Annotations annos = synapseOne.getAnnotations(project.getId());
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		annos = synapseOne.updateAnnotations(project.getId(), annos);
		
		AccessControlList acl = synapseOne.getACL(project.getId());
		acl.setCreatedBy("John Doe");
		acl.setId(project.getId());
		synapseOne.updateACL(acl);
			
		int allPartsMask = EntityBundle.ENTITY |
				EntityBundle.ANNOTATIONS |
				EntityBundle.PERMISSIONS |
				EntityBundle.ENTITY_PATH |
				EntityBundle.ENTITY_REFERENCEDBY |
				EntityBundle.HAS_CHILDREN |
				EntityBundle.ACL |
				EntityBundle.ACCESS_REQUIREMENTS |
				EntityBundle.UNMET_ACCESS_REQUIREMENTS;
		
		long startTime = System.nanoTime();
		EntityBundle entityBundle = synapseOne.getEntityBundle(project.getId(), allPartsMask);
		long endTime = System.nanoTime();
		long requestTime = (endTime - startTime) / 1000000;
		System.out.println("Bundle request time was " + requestTime + " ms");
		
		
		assertEquals("Invalid fetched Entity in the EntityBundle", 
				synapseOne.getEntityById(project.getId()), entityBundle.getEntity());
		assertEquals("Invalid fetched Annotations in the EntityBundle", 
				synapseOne.getAnnotations(project.getId()), entityBundle.getAnnotations());
		assertEquals("Invalid fetched EntityPath in the EntityBundle", 
				synapseOne.getEntityPath(project.getId()), entityBundle.getPath());
		assertEquals("Invalid fetched ReferencedBy in the EntityBundle", 
				synapseOne.getEntityReferencedBy(project).getResults(), entityBundle.getReferencedBy());
		assertEquals("Invalid fetched ChildCount in the EntityBundle", 
				synapseOne.getChildCount(project.getId()) > 0, entityBundle.getHasChildren());
		assertEquals("Invalid fetched ACL in the EntityBundle", 
				synapseOne.getACL(project.getId()), entityBundle.getAccessControlList());
		assertEquals("Unexpected ARs in the EntityBundle", 
				0, entityBundle.getAccessRequirements().size());
		assertEquals("Unexpected unmet-ARs in the EntityBundle", 
				0, entityBundle.getUnmetAccessRequirements().size());
	}

	@Test
	public void testJavaClientCreateUpdateEntityBundle() throws SynapseException {
		// Create resource access for me
		UserProfile myProfile = synapseOne.getMyProfile();
		Set<ACCESS_TYPE> accessTypes = new HashSet<ACCESS_TYPE>();
		accessTypes.addAll(Arrays.asList(ACCESS_TYPE.values()));
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(myProfile.getOwnerId()));
		ra.setAccessType(accessTypes);
		
		// Create an entity		
		Study s1 = new Study();
		s1.setName("Dummy Study 1");
		s1.setEntityType(s1.getClass().getName());
		s1.setParentId(project.getId());
		
		// Create annotations for this entity
		Annotations a1 = new Annotations();		
		a1.addAnnotation("doubleAnno", new Double(45.0001));
		a1.addAnnotation("string", "A string");
		
		// Create ACL for this entity
		AccessControlList acl1 = new AccessControlList();
		Set<ResourceAccess> resourceAccesses = new HashSet<ResourceAccess>();
		resourceAccesses.add(ra);
		acl1.setResourceAccess(resourceAccesses);
		
		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		ebc.setEntity(s1);
		ebc.setAnnotations(a1);
		ebc.setAccessControlList(acl1);
				
		EntityBundle response = synapseOne.createEntityBundle(ebc);
		
		Study s2 = (Study) response.getEntity();
		toDelete.add(s2.getId());
		assertNotNull(s2);
		assertNotNull("Etag should have been generated, but was not", s2.getEtag());
		assertEquals(s1.getName(), s2.getName());
		
		Annotations a2 = response.getAnnotations();
		assertNotNull(a2);
		assertNotNull("Etag should have been generated, but was not", a2.getEtag());
		assertEquals("Retrieved Annotations in bundle do not match original ones", a1.getStringAnnotations(), a2.getStringAnnotations());
		assertEquals("Retrieved Annotations in bundle do not match original ones", a1.getDoubleAnnotations(), a2.getDoubleAnnotations());
		
		AccessControlList acl2 = response.getAccessControlList();
		assertNotNull(acl2);
		assertNotNull("Etag should have been generated, but was not", acl2.getEtag());
		assertEquals("Retrieved ACL in bundle does not match original one", acl1.getResourceAccess(), acl2.getResourceAccess());
	
		// Update the bundle, verify contents
		s2.setName("Dummy study 1 updated");
		a2.addAnnotation("string2", "Another string");
		acl2.setModifiedBy("Update user");
		
		EntityBundleCreate ebc2 = new EntityBundleCreate();
		ebc2.setEntity(s2);
		ebc2.setAnnotations(a2);
		ebc2.setAccessControlList(acl2);
				
		EntityBundle response2 = synapseOne.updateEntityBundle(s2.getId(), ebc2);
		
		Study s3 = (Study) response2.getEntity();
		assertNotNull(s3);
		assertFalse("Etag should have been updated, but was not", s2.getEtag().equals(s3.getEtag()));
		assertEquals(s2.getName(), s3.getName());
		
		Annotations a3 = response2.getAnnotations();
		assertNotNull(a3);
		assertFalse("Etag should have been updated, but was not", a2.getEtag().equals(a3.getEtag()));
		assertEquals("Retrieved Annotations in bundle do not match original ones", a2.getStringAnnotations(), a3.getStringAnnotations());
		assertEquals("Retrieved Annotations in bundle do not match original ones", a2.getDoubleAnnotations(), a3.getDoubleAnnotations());
		
		AccessControlList acl3 = response2.getAccessControlList();
		assertNotNull(acl3);
		assertFalse("Etag should have been updated, but was not", acl2.getEtag().equals(acl3.getEtag()));
		assertEquals("Retrieved ACL in bundle does not match original one", acl2.getResourceAccess(), acl3.getResourceAccess());

	}

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
		layer = synapseOne.createEntity(layer);

		layer = (Data) synapseOne.uploadLocationableToSynapse(layer,
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
	 * Create a Data entity, update it's location data to point to an external url, then download it's data and test
	 */
	@Test
	public void testJavaClientUpdateExternalLocationWithoutDownload() throws Exception {
			// Use a url that we expect to be available and whose contents we don't
		// expect to change
		String externalUrl = "http://www.sagebase.org/favicon";
		String externalUrlMD5 = "8f8e272d7fdb2fc6c19d57d00330c397";
		//int externalUrlFileSizeBytes = 1150;

		List<LocationData> locations = new ArrayList<LocationData>();
		
		Data layer = new Data();
		layer.setType(LayerTypeNames.M);
		layer.setLocations(locations);
		layer.setParentId(dataset.getId());
		layer = synapseOne.createEntity(layer);

		//update the locations
		layer = (Data)synapseOne.updateExternalLocationableToSynapse(layer, externalUrl, externalUrlMD5);
		locations = layer.getLocations();
		
		assertEquals(1, locations.size());
		LocationData location = locations.get(0);
		assertEquals(LocationTypeNames.external, location.getType());
		assertNotNull(location.getPath());
		//test location url
		assertEquals(location.getPath(), externalUrl);
		assertEquals(layer.getMd5(), externalUrlMD5);

		//also verify all is well when we don't set the md5 for external
		layer = (Data)synapseOne.updateExternalLocationableToSynapse(layer, externalUrl);
		locations = layer.getLocations();
		
		assertEquals(1, locations.size());
		location = locations.get(0);
		assertEquals(LocationTypeNames.external, location.getType());
		assertNotNull(location.getPath());
		//test location url
		assertEquals(location.getPath(), externalUrl);
		assertNull(layer.getMd5());
		
	}
	
	/**
	 * Create a Data entity, update it's location data to point to an external url, then download it's data and test
	 */
	@Test(expected=SynapseBadRequestException.class)
	public void testJavaClientUpdateMissingMd5() throws Exception {
		List<LocationData> locations = new ArrayList<LocationData>();
		
		LocationData fakeAwsLocation = new LocationData();
		fakeAwsLocation.setPath("fakeawslocation");
		fakeAwsLocation.setType(LocationTypeNames.awss3);
		locations.add(fakeAwsLocation);

		Data layer = new Data();
		layer.setType(LayerTypeNames.M);
		//md5 not set
		layer.setParentId(dataset.getId());
		layer.setLocations(locations);
		//should fail (due to missing md5)
		layer = synapseOne.createEntity(layer);
	}

	@Test
	public void testGetUsers() throws Exception {
		UserProfile myProfile = synapseOne.getMyProfile();
		assertNotNull(myProfile);
		String myPrincipalId = myProfile.getOwnerId();
		assertNotNull(myPrincipalId);

		PaginatedResults<UserProfile> users = synapseOne.getUsers(0,1000);
		assertTrue(users.getResults().size()>0);
		boolean foundSelf = false;
		List<String> allDisplayNames = new ArrayList<String>();
		for (UserProfile up : users.getResults()) {
			assertNotNull(up.getOwnerId());
			String displayName = up.getUserName();
			allDisplayNames.add(displayName);
			if (up.getOwnerId().equals(myPrincipalId)) foundSelf=true;
		}
		assertTrue("Didn't find self, only found "+allDisplayNames, foundSelf);
	}

	@Test
	public void testGetGroups() throws Exception {
		PaginatedResults<UserGroup> groups = synapseOne.getGroups(0,100);
		assertTrue(groups.getResults().size()>0);
		for (UserGroup ug : groups.getResults()) {
			assertNotNull(ug.getId());
		}
	}

	@Test
	public void testGetUserGroupHeadersById() throws Exception {
		List<String> ids = new ArrayList<String>();		
		PaginatedResults<UserProfile> users = synapseOne.getUsers(0,100);
		for (UserProfile up : users.getResults()) {	
			if (up.getUserName() != null) {
				ids.add(up.getOwnerId());
			}
		}
		UserGroupHeaderResponsePage response = synapseOne.getUserGroupHeadersByIds(ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		
		String dummyId = "This extra String should not match an ID";
		ids.add(dummyId);
		
		assertEquals(ids.size() - 1, headers.size());
		for (String id : ids)
			if (!id.equals(dummyId))
				assertTrue(headers.containsKey(id));
	}

	@Test
	public void testAccessRequirement() throws Exception {
		// create a node
		Data layer = new Data();
		layer.setType(LayerTypeNames.E);
		layer.setParentId(dataset.getId());
		layer = synapseOne.createEntity(layer);

		assertTrue(synapseOne.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// add an access requirement
		TermsOfUseAccessRequirement r = new TermsOfUseAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(layer.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		r.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));


		r.setAccessType(ACCESS_TYPE.DOWNLOAD);
		r.setTermsOfUse("I promise to be good.");
		r = adminSynapse.createAccessRequirement(r);
		accessRequirementsToDelete.add(r.getId());
		
		// check that owner can download
		assertTrue(synapseOne.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));

		UserProfile otherProfile = synapseOne.getMyProfile();
		assertNotNull(otherProfile);

		// check that another can't download
		assertFalse(synapseTwo.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// get unmet access requirements
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.ENTITY);
		subjectId.setId(layer.getId());
		PaginatedResults<AccessRequirement> ars = synapseTwo.getUnmetAccessRequirements(subjectId);
		assertEquals(1, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
		AccessRequirement clone = ars.getResults().get(0);
		assertEquals(r.getEntityType(), clone.getEntityType());
		assertTrue(clone instanceof TermsOfUseAccessRequirement);
		assertEquals(r.getTermsOfUse(), ((TermsOfUseAccessRequirement)clone).getTermsOfUse());
		
		// create approval for the requirement
		TermsOfUseAccessApproval approval = new TermsOfUseAccessApproval();
		approval.setAccessorId(otherProfile.getOwnerId());
		approval.setRequirementId(clone.getId());
		synapseTwo.createAccessApproval(approval);
		
		// get unmet requirements -- should be empty
		ars = synapseTwo.getUnmetAccessRequirements(subjectId);
		assertEquals(0, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
		
		// check that CAN download
		assertTrue(synapseTwo.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));
	}

	@Test
	public void testUserSessionData() throws Exception {
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		assertNotNull("Failed to find session token", sessionToken);
		UserProfile integrationTestUserProfile = userSessionData.getProfile();
		assertNotNull("Failed to get user profile from user session data", integrationTestUserProfile);
	}

	@Test
	public void testRetrieveSynapseTOU() throws Exception {
		String termsOfUse = synapseOne.getSynapseTermsOfUse();
		assertNotNull(termsOfUse);
		assertTrue(termsOfUse.length()>100);
	}
	
	@Test
	public void testRetrieveBridgeTOU() throws Exception {
		String synapseTermsOfUse = synapseOne.getTermsOfUse(DomainType.BRIDGE);
		assertNotNull(synapseTermsOfUse);
		assertTrue(synapseTermsOfUse.length()>100);

		String bridgeTermsOfUse = synapseOne.getTermsOfUse(DomainType.SYNAPSE);
		assertNotNull(bridgeTermsOfUse);
		assertTrue(bridgeTermsOfUse.length()>100);
		assertFalse(bridgeTermsOfUse.equals(synapseTermsOfUse));
	}
	
	/**
	 * Test that we can add an attachment to a project and then get it back.
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
			AttachmentData data = synapseOne.uploadAttachmentToSynapse(project.getId(), originalFile, finalName);
			assertEquals(finalName, data.getName());
			// Save this this attachment on the entity.
			project.setAttachments(new ArrayList<AttachmentData>());
			project.getAttachments().add(data);
			// Save this attachment to the project
			project = synapseOne.putEntity(project);
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
			synapseOne.downloadEntityAttachment(project.getId(), clone, attachmentDownload);
			assertTrue(attachmentDownload.exists());
			System.out.println(attachmentDownload.getAbsolutePath());
			assertEquals(originalFile.length(), attachmentDownload.length());
			// Now make sure we can get the preview image
			// Before we download the preview make sure it exists
			synapseOne.waitForPreviewToBeCreated(project.getId(), clone.getPreviewId(), PREVIEW_TIMOUT);
			synapseOne.downloadEntityAttachmentPreview(project.getId(), clone.getPreviewId(), previewDownload);
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
			
			UserProfile profile = synapseOne.getMyProfile();
			AttachmentData data = synapseOne.uploadUserProfileAttachmentToSynapse(profile.getOwnerId(), originalFile, finalName);
			//save this as part of the user
			profile.setPic(data);
			synapseOne.updateMyProfile(profile);
			
			//download, and check that it was updated
			profile = synapseOne.getMyProfile();
			AttachmentData clone = profile.getPic();
			assertEquals(finalName, data.getName());
			assertEquals(data.getName(), clone.getName());
			assertEquals(data.getMd5(), clone.getMd5());
			assertEquals(data.getContentType(), clone.getContentType());
			assertEquals(data.getTokenId(), clone.getTokenId());
			// the attachment should have preview
			assertNotNull(clone.getPreviewId());
			// Now make sure we can download our
			
			synapseOne.downloadUserProfileAttachment(profile.getOwnerId(), clone, attachmentDownload);
			assertTrue(attachmentDownload.exists());
			System.out.println(attachmentDownload.getAbsolutePath());
			assertEquals(originalFile.length(), attachmentDownload.length());
			// Now make sure we can get the preview image
			// Before we download the preview make sure it exists
			synapseOne.waitForUserProfilePreviewToBeCreated(profile.getOwnerId(), clone.getPreviewId(), PREVIEW_TIMOUT);
			synapseOne.downloadUserProfileAttachmentPreview(profile.getOwnerId(), clone.getPreviewId(), previewDownload);
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
		Folder child = new Folder();
		child.setName("childFolder");
		child.setParentId(project.getId());
		child = synapseOne.createEntity(child);
		assertNotNull(child);
		assertNotNull(child.getId());
		assertEquals(project.getId(), child.getParentId());
		
		// This folder should have no children
		Long count = synapseOne.getChildCount(child.getId());
		assertEquals(new Long(0), count);
		// Now add a child
		Study grandChild = new Study();
		grandChild.setName("childFolder");
		grandChild.setParentId(child.getId());
		grandChild = synapseOne.createEntity(grandChild);
		assertNotNull(grandChild);
		assertNotNull(grandChild.getId());
		assertEquals(child.getId(), grandChild.getParentId());
		// The count should now be one.
		count = synapseOne.getChildCount(child.getId());
		assertEquals(new Long(1), count);
	}
	
	/**
	 * PLFM-1166 annotations are not being returned from queries.
	 */
	@Test 
	public void testPLMF_1166() throws SynapseException, JSONException{
		// Get the project annotations
		Annotations annos = synapseOne.getAnnotations(project.getId());
		String key = "PLFM_1166";
		annos.addAnnotation(key, "one");
		synapseOne.updateAnnotations(project.getId(), annos);
		// Make sure we can query for 
		String query = "select id, "+key+" from project where id == '"+project.getId()+"'";
		JSONObject total = synapseOne.query(query);
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
	 */
	@Test 
	public void testPLFM_1212() throws SynapseException, JSONException, InterruptedException{
		// The dataset should start with no references
		PaginatedResults<EntityHeader> refs = synapseOne.getEntityReferencedBy(dataset);
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
		link = synapseOne.createEntity(link);
		// Get 
		waitForReferencesBy(dataset);
		refs = synapseOne.getEntityReferencedBy(dataset);
		assertNotNull(refs);
		assertEquals(1l, refs.getTotalNumberOfResults());
		assertNotNull(refs.getResults());
		assertEquals(1, refs.getResults().size());
		// Test that the hack for PLFM-1287 is still in place.
		assertEquals(project.getId(), refs.getResults().get(0).getId());
		
	}
	
	/**
	 * Helper to wait for references to be update.
	 */
	private void waitForReferencesBy(Entity toWatch) throws SynapseException, InterruptedException{
		// Wait for the references to appear
		PaginatedResults<EntityHeader> refs = synapseOne.getEntityReferencedBy(toWatch);
		long start = System.currentTimeMillis();
		while(refs.getTotalNumberOfResults() < 1){
			System.out.println("Waiting for refrences to be published for entity: "+toWatch.getId());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for refernces to be published for entity: "+toWatch.getId(), elapse < RDS_WORKER_TIMEOUT);
			refs = synapseOne.getEntityReferencedBy(toWatch);
		}
	}

	@Test
	public void testPLFM_1548() throws SynapseException, InterruptedException, JSONException{
		// Add a unique annotation and query for it
		String key = "keyPLFM_1548";
		String value = UUID.randomUUID().toString();
		Annotations annos = synapseOne.getAnnotations(dataset.getId());
		annos.addAnnotation(key, value);
		synapseOne.updateAnnotations(dataset.getId(), annos);
		String queryString = "select id from entity where entity."+key+" == '"+value+"'";
		// Wait for the query
		waitForQuery(queryString);
	}
	
	/**
	 * Helper 
	 */
	private void waitForQuery(String queryString) throws SynapseException, InterruptedException, JSONException{
		// Wait for the references to appear
		JSONObject results = synapseOne.query(queryString);
		assertNotNull(results);
		assertTrue(results.has("totalNumberOfResults"));
		assertNotNull(results.getLong("totalNumberOfResults"));
		long start = System.currentTimeMillis();
		while(results.getLong("totalNumberOfResults") < 1){
			System.out.println("Waiting for query: "+queryString);
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for annotations to be published for query: "+queryString, elapse < RDS_WORKER_TIMEOUT);
			results = synapseOne.query(queryString);
			System.out.println(results);
		}
	}
	
	/**
	 * Test for PLFM-1214
	 */
	@Test 
	public void testPLFM_1214() throws SynapseException, JSONException{
		// The dataset should start with no references
		dataset.setNumSamples(123l);
		dataset = synapseOne.putEntity(dataset);
		assertEquals(new Long(123), dataset.getNumSamples());
		// Now clear out the value
		dataset.setNumSamples(null);
		dataset = synapseOne.putEntity(dataset);
		assertEquals(null, dataset.getNumSamples());
	}

	@Test
	public void testPLFM_1272() throws Exception{
		// Now add a data object 
		Data data = new Data();
		data.setParentId(project.getId());
		data = synapseOne.createEntity(data);

		// Now query for the data object
		String queryString = "SELECT id, name FROM data WHERE data.parentId == \""+project.getId()+"\"";
		JSONObject results = synapseOne.query(queryString);
		assertNotNull(results);
		assertTrue(results.has("totalNumberOfResults"));
		assertEquals(1l, results.getLong("totalNumberOfResults"));
		
		queryString = "SELECT id, name FROM layer WHERE layer.parentId == \""+project.getId()+"\"";
		results = synapseOne.query(queryString);
		assertNotNull(results);
		assertTrue(results.has("totalNumberOfResults"));
		assertEquals(1l, results.getLong("totalNumberOfResults"));
	}

	@Test
	public void testGetAllUserAndGroupIds() throws SynapseException{
		HashSet<String> expected = new HashSet<String>();
		// Get all the users
		PaginatedResults<UserProfile> pr = synapseOne.getUsers(0, Integer.MAX_VALUE);
		for(UserProfile up : pr.getResults()){
			expected.add(up.getOwnerId());
		}
		PaginatedResults<UserGroup> groupPr = synapseOne.getGroups(0, Integer.MAX_VALUE);
		for(UserGroup ug : groupPr.getResults()){
			expected.add(ug.getId());
		}
		Set<String> results = synapseOne.getAllUserAndGroupIds();
		assertNotNull(results);
		assertEquals(expected.size(), results.size());
		assertEquals(expected,results);
		
	}
	@Test
	public void testRetrieveApiKey() throws SynapseException {
		String apiKey = synapseOne.retrieveApiKey();
		assertNotNull(apiKey);		
	}
	
	private String getSomeGroup(String notThisOne) throws SynapseException {
		PaginatedResults<UserGroup> groups = synapseOne.getGroups(0,100);
		String somePrincipalId = null;
		for (UserGroup ug : groups.getResults()) {
			if (ug.getId()!=notThisOne) { // don't want to use the team itself!
				somePrincipalId = ug.getId();
				break;
			}
		}
		assertNotNull(somePrincipalId);
		return somePrincipalId;
	}

	@Test
	public void testTeamAPI() throws SynapseException, IOException {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());

		UserProfile myProfile = synapseOne.getMyProfile();
		String myPrincipalId = myProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		assertNotNull(createdTeam.getId());
		assertEquals(name, createdTeam.getName());
		assertEquals(description, createdTeam.getDescription());
		assertNotNull(createdTeam.getCreatedOn());
		assertNotNull(createdTeam.getModifiedOn());
		assertEquals(myPrincipalId, createdTeam.getCreatedBy());
		assertEquals(myPrincipalId, createdTeam.getModifiedBy());
		assertNotNull(createdTeam.getEtag());
		assertNull(createdTeam.getIcon());
		// get the Team
		Team retrievedTeam = synapseOne.getTeam(createdTeam.getId());
		assertEquals(createdTeam, retrievedTeam);
		// upload an icon and get the file handle
		// before setting the icon
		try {
			synapseOne.getTeamIcon(createdTeam.getId(), false);
			fail("Expected: Not Found");
		} catch (SynapseException e) {
			// expected
		}
		
		PrintWriter pw = null;
		File file = File.createTempFile("testIcon", null);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			pw = new PrintWriter(fos);
			pw.println("test");
			pw.close();
			pw = null;
		} finally {
			if (pw!=null) pw.close();
		}
		S3FileHandle fileHandle = synapseOne.createFileHandle(file, "text/plain");
		handlesToDelete.add(fileHandle.getId());
		
		// update the Team with the icon
		createdTeam.setIcon(fileHandle.getId());
		Team updatedTeam = synapseOne.updateTeam(createdTeam);
		// get the icon url
		URL url = synapseOne.getTeamIcon(updatedTeam.getId(), false);
		assertNotNull(url);
		// query for all teams
		PaginatedResults<Team> teams = synapseOne.getTeams(null, 1, 0);
		assertEquals(1L, teams.getTotalNumberOfResults());
		assertEquals(updatedTeam, teams.getResults().get(0));
		// make sure pagination works
		teams = synapseOne.getTeams(null, 10, 1);
		assertEquals(0L, teams.getResults().size());
		
		// query for all teams, based on name fragment
		// need to update cache.  the service to trigger an update
		// requires admin privileges, so we log in as an admin:
		adminSynapse.updateTeamSearchCache();
		teams = synapseOne.getTeams(name.substring(0, 3),1, 0);
		assertEquals(1L, teams.getTotalNumberOfResults());
		assertEquals(updatedTeam, teams.getResults().get(0));
		// again, make sure pagination works
		teams = synapseOne.getTeams(name.substring(0, 3), 10, 1);
		assertEquals(0L, teams.getResults().size());
		
		// query for team members.  should get just the creator
		PaginatedResults<TeamMember> members = synapseOne.getTeamMembers(updatedTeam.getId(), null, 1, 0);
		assertEquals(1L, members.getTotalNumberOfResults());
		TeamMember tm = members.getResults().get(0);
		assertEquals(myPrincipalId, tm.getMember().getOwnerId());
		assertEquals(updatedTeam.getId(), tm.getTeamId());
		assertTrue(tm.getIsAdmin());
		
		// while we're at it, check the 'getTeamMember' service
		assertEquals(tm, synapseOne.getTeamMember(updatedTeam.getId(), myPrincipalId));
		
		TeamMembershipStatus tms = synapseOne.getTeamMembershipStatus(updatedTeam.getId(), myPrincipalId);
		assertEquals(updatedTeam.getId(), tms.getTeamId());
		assertEquals(myPrincipalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());
		
		// add a member to the team
		UserProfile otherUp = synapseTwo.getMyProfile();
		String otherDName = otherUp.getUserName();
		String otherPrincipalId = otherUp.getOwnerId();
		// the other has to ask to be added
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(createdTeam.getId());
		synapseTwo.createMembershipRequest(mrs);
		// check membership status
		tms = synapseOne.getTeamMembershipStatus(updatedTeam.getId(), otherPrincipalId);
		assertEquals(updatedTeam.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());

		// a subtle difference:  if the other user requests the status, 'canJoin' is false
		tms = synapseTwo.getTeamMembershipStatus(updatedTeam.getId(), otherPrincipalId);
		assertEquals(updatedTeam.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertFalse(tms.getCanJoin());

		// query for team members using name fragment.  should get team creator back
		String myDisplayName = /*"devuser1@sagebase.org"*/myProfile.getUserName();
		members = synapseOne.getTeamMembers(updatedTeam.getId(), myDisplayName, 1, 0);
		assertEquals(1L, members.getTotalNumberOfResults());
		assertEquals(myPrincipalId, members.getResults().get(0).getMember().getOwnerId());
		assertTrue(members.getResults().get(0).getIsAdmin());

		synapseOne.addTeamMember(updatedTeam.getId(), otherPrincipalId);
		// update the prefix cache
		adminSynapse.updateTeamSearchCache();
		
		tms = synapseTwo.getTeamMembershipStatus(updatedTeam.getId(), otherPrincipalId);
		assertEquals(updatedTeam.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertFalse(tms.getCanJoin());

		// query for team members.  should get creator as well as new member back
		members = synapseOne.getTeamMembers(updatedTeam.getId(), null, 2, 0);
		assertEquals(2L, members.getTotalNumberOfResults());
		assertEquals(2L, members.getResults().size());
		
		// query for team members using name fragment
		members = synapseOne.getTeamMembers(updatedTeam.getId(), otherDName.substring(0,otherDName.length()-4), 1, 0);
		assertEquals(1L, members.getTotalNumberOfResults());
		
		TeamMember otherMember = members.getResults().get(0);
		assertEquals(otherPrincipalId, otherMember.getMember().getOwnerId());
		assertFalse(otherMember.getIsAdmin());
		
		// make the other member an admin
		synapseOne.setTeamMemberPermissions(createdTeam.getId(), otherPrincipalId, true);
		adminSynapse.updateTeamSearchCache();
		
		members = synapseOne.getTeamMembers(createdTeam.getId(), otherDName.substring(0,otherDName.length()-4), 1, 0);
		assertEquals(1L, members.getTotalNumberOfResults());
		// now the other member is an admin
		otherMember = members.getResults().get(0);
		assertEquals(otherPrincipalId, otherMember.getMember().getOwnerId());
		assertTrue(otherMember.getIsAdmin());

		// remove admin privileges
		synapseOne.setTeamMemberPermissions(createdTeam.getId(), otherPrincipalId, false);
		adminSynapse.updateTeamSearchCache();
		
		// query for teams based on member's id
		teams = synapseOne.getTeamsForUser(otherPrincipalId, 1, 0);
		assertEquals(1L, teams.getTotalNumberOfResults());
		assertEquals(updatedTeam, teams.getResults().get(0));
		// remove the member from the team
		synapseOne.removeTeamMember(updatedTeam.getId(), otherPrincipalId);
		adminSynapse.updateTeamSearchCache();
		// query for teams based on member's id (should get nothing)
		teams = synapseOne.getTeamsForUser(otherPrincipalId, 1, 0);
		assertEquals(0L, teams.getTotalNumberOfResults());
		// delete Team
		synapseOne.deleteTeam(updatedTeam.getId());
		// delete the Team again (should be idempotent)
		synapseOne.deleteTeam(updatedTeam.getId());
		// retrieve the Team (should get a 404)
		try {
			synapseOne.getTeam(updatedTeam.getId());
			fail("Failed to delete Team "+updatedTeam.getId());
		} catch (SynapseException e) {
			// as expected
		}
	}

	@Test
	public void testTeamRestrictionRoundTrip() throws SynapseException, UnsupportedEncodingException {
		// Create Evaluation
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());

		// Create AccessRestriction
		TermsOfUseAccessRequirement tou = new TermsOfUseAccessRequirement();
		tou.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.TEAM);
		subjectId.setId(createdTeam.getId());
		tou.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		tou = adminSynapse.createAccessRequirement(tou);
		assertNotNull(tou.getId());
		accessRequirementsToDelete.add(tou.getId());
		
		// Query AccessRestriction
		VariableContentPaginatedResults<AccessRequirement> paginatedResults;
		paginatedResults = adminSynapse.getAccessRequirements(subjectId);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		// Query Unmet AccessRestriction
		paginatedResults = synapseTwo.getUnmetAccessRequirements(subjectId);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		// Create AccessApproval
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setRequirementId(tou.getId());
		synapseTwo.createAccessApproval(aa);
		
		// Query AccessRestriction
		paginatedResults = adminSynapse.getAccessRequirements(subjectId);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		// Query Unmet AccessRestriction (since the requirement is now met, the list is empty)
		paginatedResults = synapseTwo.getUnmetAccessRequirements(subjectId);
		assertEquals(0L, paginatedResults.getTotalNumberOfResults());
		assertTrue(paginatedResults.getResults().isEmpty());
	}

	@Test
	public void testMembershipInvitationAPI() throws SynapseException {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		String myPrincipalId = synapseOne.getMyProfile().getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create an invitation
		MembershipInvtnSubmission dto = new MembershipInvtnSubmission();
		String somePrincipalId = getSomeGroup(createdTeam.getId());
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		dto.setInviteeId(somePrincipalId);
		String message = "Please accept this invitation";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		MembershipInvtnSubmission created = synapseOne.createMembershipInvitation(dto);
		assertEquals(myPrincipalId, created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(expiresOn, created.getExpiresOn());
		assertNotNull(created.getId());
		assertEquals(somePrincipalId, created.getInviteeId());
		assertEquals(message, created.getMessage());
		assertEquals(createdTeam.getId(), created.getTeamId());
		// get the invitation
		MembershipInvtnSubmission retrieved = synapseOne.getMembershipInvitation(created.getId());
		assertEquals(created, retrieved);
		
		{
			// query for invitations based on user
			PaginatedResults<MembershipInvitation> invitations = synapseOne.getOpenMembershipInvitations(somePrincipalId, null, 1, 0);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			MembershipInvitation invitation = invitations.getResults().get(0);
			assertEquals(expiresOn, invitation.getExpiresOn());
			assertEquals(message, invitation.getMessage());
			assertEquals(createdTeam.getId(), invitation.getTeamId());
			assertEquals(somePrincipalId, invitation.getUserId());
			// check pagination
			invitations = synapseOne.getOpenMembershipInvitations(somePrincipalId, null, 2, 1);
			assertEquals(0L, invitations.getResults().size());
			// query for invitations based on user and team
			invitations = synapseOne.getOpenMembershipInvitations(somePrincipalId, createdTeam.getId(), 1, 0);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			MembershipInvitation invitation2 = invitations.getResults().get(0);
			assertEquals(invitation, invitation2);
			// again, check pagination
			invitations = synapseOne.getOpenMembershipInvitations(somePrincipalId, createdTeam.getId(), 2, 1);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			assertEquals(0L, invitations.getResults().size());
		}
		
		// query for invitation SUBMISSIONs based on team
		{
			PaginatedResults<MembershipInvtnSubmission> invitationSubmissions = 
					synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), null, 1, 0);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			MembershipInvtnSubmission submission = invitationSubmissions.getResults().get(0);
			assertEquals(created, submission);
			// check pagination
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), null, 2, 1);
			assertEquals(0L, invitationSubmissions.getResults().size());
			// query for SUBMISSIONs based on team and invitee
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), somePrincipalId, 1, 0);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			assertEquals(created, invitationSubmissions.getResults().get(0));
			// again, check pagination
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), somePrincipalId, 2, 1);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			assertEquals(0L, invitationSubmissions.getResults().size());
		}
		
		// delete the invitation
		synapseOne.deleteMembershipInvitation(created.getId());
		try {
			synapseOne.getMembershipInvitation(created.getId());
			fail("Failed to delete membership invitation.");
		} catch (SynapseException e) {
			// as expected
		}
	}

	@Test
	public void testMembershipRequestAPI() throws SynapseException {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		String myPrincipalId = synapseOne.getMyProfile().getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create a request
		String otherPrincipalId = synapseTwo.getMyProfile().getOwnerId();
		MembershipRqstSubmission dto = new MembershipRqstSubmission();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		String message = "Please accept this request";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		MembershipRqstSubmission created = synapseTwo.createMembershipRequest(dto);
		assertEquals(otherPrincipalId, created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(expiresOn, created.getExpiresOn());
		assertNotNull(created.getId());
		assertEquals(otherPrincipalId, created.getUserId());
		assertEquals(message, created.getMessage());
		assertEquals(createdTeam.getId(), created.getTeamId());
		// get the request
		MembershipRqstSubmission retrieved = synapseTwo.getMembershipRequest(created.getId());
		assertEquals(created, retrieved);
		// query for requests based on team
		{
			PaginatedResults<MembershipRequest> requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), null, 1, 0);
			assertEquals(1L, requests.getTotalNumberOfResults());
			MembershipRequest request = requests.getResults().get(0);
			assertEquals(expiresOn, request.getExpiresOn());
			assertEquals(message, request.getMessage());
			assertEquals(createdTeam.getId(), request.getTeamId());
			assertEquals(otherPrincipalId, request.getUserId());
			// check pagination
			requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), null, 2, 1);
			assertEquals(1L, requests.getTotalNumberOfResults());
			assertEquals(0L, requests.getResults().size());
			// query for requests based on team and member
			requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), otherPrincipalId, 1, 0);
			assertEquals(1L, requests.getTotalNumberOfResults());
			MembershipRequest request2 = requests.getResults().get(0);
			assertEquals(request, request2);
			// again, check pagination
			requests = synapseOne.getOpenMembershipRequests(createdTeam.getId(), otherPrincipalId, 2, 1);
			assertEquals(1L, requests.getTotalNumberOfResults());
			assertEquals(0L, requests.getResults().size());
		}
		
		// query for request SUBMISSIONs based on team
		{
			PaginatedResults<MembershipRqstSubmission> requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, null, 1, 0);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			MembershipRqstSubmission requestSubmission = requestSubmissions.getResults().get(0);
			assertEquals(created, requestSubmission);
			// check pagination
			requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, null, 2, 1);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			assertEquals(0L, requestSubmissions.getResults().size());
			// query for requests based on team and member
			requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, createdTeam.getId(), 1, 0);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			assertEquals(created, requestSubmissions.getResults().get(0));
			// again, check pagination
			requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, createdTeam.getId(), 2, 1);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			assertEquals(0L, requestSubmissions.getResults().size());
		}

		// delete the request
		synapseTwo.deleteMembershipRequest(created.getId());
		try {
			synapseTwo.getMembershipRequest(created.getId());
			fail("Failed to delete membership request.");
		} catch (SynapseException e) {
			// as expected
		}
	}
	
	@Test
	public void testStringUploadToS3() throws Exception {
		String content = "This is my test string.";
		String fileHandleId = synapseOne.uploadToFileHandle(content.getBytes("UTF-8"), 
			ContentType.create("text/plain", Charset.forName("UTF-8")));
		assertNotNull(fileHandleId);
		handlesToDelete.add(fileHandleId);
	}
}
