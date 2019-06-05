package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LogEntry;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.entity.query.Condition;
import org.sagebionetworks.repo.model.entity.query.EntityFieldName;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.entity.query.EntityQueryUtils;
import org.sagebionetworks.repo.model.entity.query.Operator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	private static SynapseAdminClient synapseAnonymous;
	private static Long user1ToDelete;
	private static Long user2ToDelete;
	
	private static final int RDS_WORKER_TIMEOUT = 1000*60; // One min
	
	private static final String TEST_EMAIL = "test@test.com";

	private List<String> toDelete;
	private List<Long> accessRequirementsToDelete;
	private List<String> handlesToDelete;
	private Project project;
	private Folder dataset;
	
	private static Set<String> bootstrappedTeams = Sets.newHashSet();
	static {
		// note, this must match the bootstrapped teams defined in managers-spb.xml
		bootstrappedTeams.add("2"); // Administrators
		bootstrappedTeams.add("464532"); // Access and Compliance Team
		bootstrappedTeams.add("4"); // Trusted message senders
	}
	
	private long getBootstrapCountPlus(long number) {
		return bootstrappedTeams.size() + number;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		synapseTwo = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseTwo);
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo, UUID.randomUUID().toString(), "password"+UUID.randomUUID(), TEST_EMAIL);

		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);

		// Update this user's profile to contain a display name
		UserProfile profile = synapseTwo.getMyProfile();
		synapseTwo.updateMyProfile(profile);
	}
	
	@Before
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<>();
		accessRequirementsToDelete = new ArrayList<>();
		handlesToDelete = new ArrayList<>();

		project = synapseOne.createEntity(new Project());
		dataset = new Folder();
		dataset.setParentId(project.getId());
		dataset = synapseOne.createEntity(dataset);
		
		toDelete.add(project.getId());
		toDelete.add(dataset.getId());

		// The only teams we leave in the system are the bootstrap teams. The method
		// getBootstrapTeamsPlus(num) returns the right count for assertions.
		long numTeams = 0L;
		do {
			PaginatedResults<Team> teams = synapseOne.getTeams(null, 10, 0);
			numTeams = teams.getTotalNumberOfResults();
			for (Team team : teams.getResults()) {
				if (!bootstrappedTeams.contains(team.getId())) {
					adminSynapse.deleteTeam(team.getId());
				}
			}
		} while (numTeams > getBootstrapCountPlus(0));
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
			} catch (SynapseException e) { }
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseException e) { }
	}
	

	@Test
	public void testJavaClientGetADataset() throws Exception {
		JSONObject results = synapseOne.query("select * from folder limit 10");

		assertTrue(0 <= results.getInt("totalNumberOfResults"));

		JSONArray datasets = results.getJSONArray("results");

		if (0 < datasets.length()) {
			String datasetId = datasets.getJSONObject(0).getString("folder.id");

			Folder aStoredDataset = synapseOne.getEntity(datasetId, Folder.class);

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
		byte[] content = "File contents".getBytes("UTF-8");
		String fileHandleId1 = synapseOne.multipartUpload(new ByteArrayInputStream(content),
				(long) content.length, "content", "text/plain", null, false, false).getId();
		String fileHandleId2 = synapseOne.multipartUpload(new ByteArrayInputStream(content),
				(long) content.length, "content", "text/plain", null, false, false).getId();
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandleId1);

		file = synapseOne.createEntity(file);
		
		// Get the project using just using its ID. This is useful for cases where you
		//  do not know what you are getting until it arrives.
		Project clone = (Project) synapseOne.getEntityById(project.getId());
		assertNotNull(clone);
		assertEquals(project.getId(), clone.getId());
		
		// Get the entity annotations
		Annotations annos = synapseOne.getAnnotations(file.getId());
		assertNotNull(annos);
		assertEquals(file.getId(), annos.getId());
		assertNotNull(annos.getEtag());
		// Add some values
		annos.addAnnotation("longKey", new Long(999999));
		annos.addAnnotation("blob", "This will be converted to a blob!".getBytes("UTF-8"));
		Annotations updatedAnnos = synapseOne.updateAnnotations(file.getId(), annos);
		assertNotNull(updatedAnnos);
		assertEquals(file.getId(), annos.getId());
		assertNotNull(updatedAnnos.getEtag());
		// The Etag should have changed
		assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));

		// Get the "zero" e-tag for specific versions. See PLFM-1420.
		Entity datasetEntity = synapseOne.getEntityByIdForVersion(file.getId(), file.getVersionNumber());
		assertFalse(NodeConstants.ZERO_E_TAG.equals(datasetEntity.getEtag()));

		// Get the Users permission for this entity
		UserEntityPermissions uep = synapseOne.getUsersEntityPermissions(file.getId());
		assertNotNull(uep);
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, synapseOne.canAccess(file.getId(), ACCESS_TYPE.UPDATE));
		assertEquals(true, synapseOne.canAccess(file.getId(), ACCESS_TYPE.READ));
		assertTrue(uep.getCanChangePermissions());
		assertTrue(uep.getCanChangeSettings());
		assertTrue(uep.getCanEnableInheritance());
		
		UserProfile profile = synapseOne.getMyProfile();
		assertNotNull(profile);
		
		assertEquals(profile.getOwnerId(), uep.getOwnerPrincipalId().toString());
		
		// should be able to download
		assertTrue(synapseOne.canAccess(file.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// give read permission to synapseTwo
		AccessControlList acl = synapseOne.getACL(project.getId());
		ResourceAccess readPermission = new ResourceAccess();
		readPermission.setPrincipalId(Long.parseLong(synapseTwo.getMyProfile().getOwnerId()));
		readPermission.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD)));
		acl.getResourceAccess().add(readPermission);
		synapseOne.updateACL(acl);
		
		// now add a ToU restriction
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();

		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(file.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("play nice");
		ar = adminSynapse.createAccessRequirement(ar);
		accessRequirementsToDelete.add(ar.getId());
		
		UserProfile otherProfile = synapseOne.getMyProfile();
		assertNotNull(otherProfile);
		
		// should not be able to download
		assertFalse(synapseTwo.canAccess(file.getId(), ACCESS_TYPE.DOWNLOAD));
		
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.ENTITY);
		subjectId.setId(file.getId());
		PaginatedResults<AccessRequirement> vcpr = synapseTwo.getUnmetAccessRequirements(subjectId, ACCESS_TYPE.DOWNLOAD, 10L, 0L);
		assertEquals(1, vcpr.getResults().size());
		
		// now add the ToU approval
		AccessApproval aa = new AccessApproval();
		aa.setAccessorId(otherProfile.getOwnerId());
		aa.setRequirementId(ar.getId());
		
		synapseTwo.createAccessApproval(aa);
		
		vcpr = synapseTwo.getUnmetAccessRequirements(subjectId, ACCESS_TYPE.DOWNLOAD, 10L, 0L);
		assertEquals(0, vcpr.getResults().size());
		
		// should be able to download
		assertTrue(synapseTwo.canAccess(file.getId(), ACCESS_TYPE.DOWNLOAD));
		
		ar.setTermsOfUse("play nicer");
		ar = adminSynapse.updateAccessRequirement(ar);
		assertEquals("play nicer", ar.getTermsOfUse());
		
		// ACL should reflect the first User's permission
		acl = synapseOne.getACL(project.getId());
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
		EntityPath path = synapseOne.getEntityPath(file.getId());
		assertNotNull(path);
		assertNotNull(path.getPath());
		assertEquals(3, path.getPath().size());
		EntityHeader header = path.getPath().get(2);
		assertNotNull(header);
		assertEquals(file.getId(), header.getId());
		
		// Get the entity headers
		List<String> entityIds = new ArrayList<String>();
		entityIds.add(project.getId());
		entityIds.add(dataset.getId());
		entityIds.add(file.getId());
		PaginatedResults<EntityHeader> entityHeaders = synapseOne.getEntityTypeBatch(entityIds);
		assertNotNull(entityHeaders);
		assertEquals(3, entityHeaders.getTotalNumberOfResults());
		List<String> outputIds = new ArrayList<String>();
		for(EntityHeader entityHeader : entityHeaders.getResults()) {
			outputIds.add(entityHeader.getId());
		}
		assertEquals(entityIds.size(), outputIds.size());
		assertTrue(entityIds.containsAll(outputIds));

		// Update and force a new version
		file = synapseOne.getEntity(file.getId(), FileEntity.class);
		String versionComment = "A new version comment";
		file.setVersionLabel(null); // Null version label will set the label to revision number
		file.setVersionComment(versionComment);
		file.setDataFileHandleId(fileHandleId2);
		synapseOne.putEntity(file, null, true);
		file = synapseOne.getEntity(file.getId(), FileEntity.class);
		assertEquals("2", file.getVersionLabel());  // Revision number should be 2
		assertEquals(versionComment, file.getVersionComment());
		assertEquals(fileHandleId2, file.getDataFileHandleId());
	}

	@Test
	public void testJavaClientCreateEntity() throws Exception {
		Folder study = new Folder();
		study.setParentId(project.getId());
		Folder createdStudy = synapseOne.createEntity(study);
		assertNotNull(createdStudy);
		assertNotNull(createdStudy.getId());

		String createdProjectId = createdStudy.getId();
		Folder fromGet = synapseOne.getEntity(createdProjectId, Folder.class);
		assertEquals(createdStudy, fromGet);

		Folder fromGetById = (Folder) synapseOne.getEntityById(createdProjectId);
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
				EntityBundle.HAS_CHILDREN |
				EntityBundle.ACL |
				EntityBundle.ACCESS_REQUIREMENTS |
				EntityBundle.UNMET_ACCESS_REQUIREMENTS |
				EntityBundle.FILE_NAME |
				EntityBundle.RESTRICTION_INFORMATION;
		
		long startTime = System.nanoTime();
		EntityBundle entityBundle = synapseOne.getEntityBundle(project.getId(), allPartsMask);
		long endTime = System.nanoTime();
		long requestTime = (endTime - startTime) / 1000000;
		System.out.println("Bundle request time was " + requestTime + " ms");

		assertNotNull(entityBundle.getRestrictionInformation());
		assertEquals("Invalid fetched Entity in the EntityBundle", 
				synapseOne.getEntityById(project.getId()), entityBundle.getEntity());
		assertEquals("Invalid fetched Annotations in the EntityBundle", 
				synapseOne.getAnnotations(project.getId()), entityBundle.getAnnotations());
		assertEquals("Invalid fetched EntityPath in the EntityBundle", 
				synapseOne.getEntityPath(project.getId()), entityBundle.getPath());
		assertEquals("Invalid fetched ACL in the EntityBundle", 
				synapseOne.getACL(project.getId()), entityBundle.getAccessControlList());
		assertEquals("Unexpected ARs in the EntityBundle", 
				0, entityBundle.getAccessRequirements().size());
		assertEquals("Unexpected unmet-ARs in the EntityBundle", 
				0, entityBundle.getUnmetAccessRequirements().size());
		assertNull(entityBundle.getFileName());
	}
	
	@Test
	public void testSpecialCharacters() throws SynapseException {
		UserProfile myProfile = synapseOne.getMyProfile();
		String location = "Zürich"; // this string is encoded differently in UTF-8 than ISO-8859-1
		String firstName = "Sławomir"; // this string can't be encoded in ISO-8859-1
		myProfile.setLocation(location);
		myProfile.setFirstName(firstName);
		synapseOne.updateMyProfile(myProfile);
		myProfile = synapseOne.getMyProfile();
		assertEquals(location, myProfile.getLocation());
		assertEquals(firstName, myProfile.getFirstName());
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
		Folder s1 = new Folder();
		s1.setName("Dummy Study 1");
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
		
		Folder s2 = (Folder) response.getEntity();
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
		
		Folder s3 = (Folder) response2.getEntity();
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
	public void testListUserProfiles() throws Exception {
		UserProfile myProfile = synapseOne.getMyProfile();
		assertNotNull(myProfile);
		String myPrincipalId = myProfile.getOwnerId();
		assertNotNull(myPrincipalId);

		List<UserProfile> users = synapseOne.listUserProfiles(Collections.singletonList(
				Long.parseLong(myPrincipalId)));
		
		assertEquals(1, users.size());
		UserProfile up = users.get(0);
		assertEquals(myPrincipalId, up.getOwnerId());
	}
	
	@Test
	public void testGetUserGroupHeaders() throws Exception {
		UserProfile adminProfile = adminSynapse.getMyProfile();
		adminSynapse.updateMyProfile(adminProfile);
		assertNotNull(adminProfile);
		// here we are just trying to check that the URI and request parameters are 'wired up' right
		UserGroupHeaderResponsePage page = waitForUserGroupHeadersByPrefix(adminProfile.getUserName());
		assertTrue(page.getTotalNumberOfResults()>0);
		TypeFilter type = TypeFilter.USERS_ONLY;
		page = synapseOne.getUserGroupHeadersByPrefix(adminProfile.getUserName(), type, 5L, 0L);
		assertTrue(page.getTotalNumberOfResults()>0);
	}
	
	@Test
	public void testGetUserGroupHeadersByAliases() throws Exception {
		UserProfile adminProfile = adminSynapse.getMyProfile();
		adminSynapse.updateMyProfile(adminProfile);
		assertNotNull(adminProfile);

		// Get the user group header using the user's name
		List<String> aliases = Lists.newArrayList(adminProfile.getUserName());
		List<UserGroupHeader> headers = synapseOne.getUserGroupHeadersByAliases(aliases);
		assertNotNull(headers);
		assertEquals(1, headers.size());
		UserGroupHeader header = headers.get(0);
		assertEquals(adminProfile.getOwnerId(), header.getOwnerId());
		assertEquals(adminProfile.getUserName(), header.getUserName());
		assertTrue(header.getIsIndividual());
	}
	
	private UserGroupHeaderResponsePage waitForUserGroupHeadersByPrefix(String prefix) throws SynapseException, InterruptedException, UnsupportedEncodingException{
		long start = System.currentTimeMillis();
		while(true){
			UserGroupHeaderResponsePage page = synapseOne.getUserGroupHeadersByPrefix(prefix);
			if(page.getTotalNumberOfResults() < 1){
				System.out.println("Waiting for principal prefix worker");
				Thread.sleep(1000);
				if(System.currentTimeMillis() - start > RDS_WORKER_TIMEOUT){
					fail("Timed out waiting for principal prefix worker.");
				}
			}else{
				return page;
			}
		}
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
		Folder layer = new Folder();
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
		
		assertEquals(r, adminSynapse.getAccessRequirement(r.getId()));
		
		// check that owner can't download (since it's not a FileEntity)
		assertFalse(synapseOne.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));

		UserProfile otherProfile = synapseOne.getMyProfile();
		assertNotNull(otherProfile);
		
		// give read permission to synapseTwo
		AccessControlList acl = synapseOne.getACL(project.getId());
		ResourceAccess readPermission = new ResourceAccess();
		readPermission.setPrincipalId(Long.parseLong(synapseTwo.getMyProfile().getOwnerId()));
		readPermission.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD)));
		acl.getResourceAccess().add(readPermission);
		synapseOne.updateACL(acl);

		// check that another can't download
		assertFalse(synapseTwo.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// get unmet access requirements
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.ENTITY);
		subjectId.setId(layer.getId());
		PaginatedResults<AccessRequirement> ars = synapseTwo.getUnmetAccessRequirements(subjectId, ACCESS_TYPE.DOWNLOAD, 10L, 0L);
		assertEquals(1, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
		AccessRequirement clone = ars.getResults().get(0);
		assertEquals(r.getConcreteType(), clone.getConcreteType());
		assertTrue(clone instanceof TermsOfUseAccessRequirement);
		assertEquals(r.getTermsOfUse(), ((TermsOfUseAccessRequirement)clone).getTermsOfUse());
		
		// check that access type param works
		assertEquals(ars, synapseTwo.getUnmetAccessRequirements(subjectId, ACCESS_TYPE.DOWNLOAD, 10L, 0L));
		
		// create approval for the requirement
		AccessApproval approval = new AccessApproval();
		approval.setAccessorId(otherProfile.getOwnerId());
		approval.setRequirementId(clone.getId());
		AccessApproval created = synapseTwo.createAccessApproval(approval);
		
		// make sure we can retrieve by ID
		assertEquals(created, synapseTwo.getAccessApproval(created.getId()));
		
		// get unmet requirements -- should be empty
		ars = synapseTwo.getUnmetAccessRequirements(subjectId, ACCESS_TYPE.DOWNLOAD, 10L, 0L);
		assertEquals(0, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
		
		// check that CAN download
		assertTrue(synapseTwo.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));

		adminSynapse.deleteAccessApproval(created.getId());

		try {
			adminSynapse.revokeAccessApprovals(r.getId().toString(), otherProfile.getOwnerId());
			fail("Expecting IllegalArgumentException");
		} catch (SynapseBadRequestException e) {
			// The service is wired up.
			// Exception thrown for not supporting access approval deletion for TermOfUseAccessRequirement
		}
	}

	@Test
	public void testUserSessionData() throws Exception {
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		assertNotNull("Failed to find session token", sessionToken);
		UserProfile integrationTestUserProfile = userSessionData.getProfile();
		assertNotNull("Failed to get user profile from user session data", integrationTestUserProfile);
	}

	/**
	 * Test that we can add an attachment to a project and then get it back.
	 * @throws Exception 
	 */
	@Test
	public void testProfileImageRoundTrip() throws Exception{
		// First load an image from the classpath
		String fileName = "images/profile_pic.png";
		URL url = IT500SynapseJavaClient.class.getClassLoader().getResource(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", url);
		File originalFile = new File(url.getFile());

		// Get the profile to update.
		UserProfile profile = synapseOne.getMyProfile();
		S3FileHandle fileHandle = synapseOne.multipartUpload(originalFile, null, true, false);
		profile.setProfilePicureFileHandleId(fileHandle.getId());
		synapseOne.updateMyProfile(profile);
		profile = synapseOne.getMyProfile();
		// Make sure we can get a pre-signed url the image and its preview.
		URL profileURL = synapseOne.getUserProfilePictureUrl(profile.getOwnerId());
		assertNotNull(profileURL);
		URL profilePreviewURL = waitForProfilePreview(synapseOne, profile.getOwnerId());
		assertNotNull(profilePreviewURL);

	}
	
	/**
	 * Wait for a profile preview.
	 * @param client
	 * @param userId
	 * @return
	 * @throws Exception
	 */
	public URL waitForProfilePreview(SynapseClient client, String userId) throws Exception{
		long start = System.currentTimeMillis();
		while(true){
			if(System.currentTimeMillis()-start > 60000){
				fail("Timed out wait for a profile preview: "+userId);
			}
			try {
				return client.getUserProfilePicturePreviewUrl(userId);
			} catch (SynapseNotFoundException e) {
				Thread.sleep(1000);
			}
		}
	}
	
	/**
	 * PLFM-1166 annotations are not being returned from queries.
	 * @throws InterruptedException 
	 */
	@Test 
	public void testPLMF_1166() throws SynapseException, JSONException, InterruptedException{
		// Get the project annotations
		Annotations annos = synapseOne.getAnnotations(project.getId());
		String key = "PLFM_1166";
		annos.addAnnotation(key, "one");
		synapseOne.updateAnnotations(project.getId(), annos);
		// Make sure we can query for 
		String query = "select id, "+key+" from project where id == '"+project.getId()+"'";
		JSONObject total = waitForQuery(query, 1L);
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
		waitForQuery(queryString, 1L);
	}

	@Test
	public void testGetQueryWithNoOffset() throws SynapseException, InterruptedException, JSONException{
		JSONObject noOffset = waitForQuery("select id from entity", 2L);
		assertEquals(2, noOffset.get("totalNumberOfResults"));
		assertTrue(noOffset.get("results").toString().contains(project.getId()));
		assertTrue(noOffset.get("results").toString().contains(dataset.getId()));
	}

	@Test
	public void testGetQueryWithOffset1() throws SynapseException, InterruptedException, JSONException{
		JSONObject offset1 = waitForQuery("select id from entity offset 1", 2L);
		assertEquals(2, offset1.get("totalNumberOfResults"));
		assertTrue(offset1.get("results").toString().contains(project.getId()));
		assertTrue(offset1.get("results").toString().contains(dataset.getId()));
	}

	@Test (expected=SynapseBadRequestException.class)
	public void testGetQueryWithOffset0() throws SynapseException, InterruptedException, JSONException{
		String queryString = "select id from entity offset 0";
		// Wait for the query
		waitForQuery(queryString, 1L);
	}
	
	/**
	 * Helper 
	 */
	private JSONObject waitForQuery(String queryString, long expectedCount) throws SynapseException, InterruptedException, JSONException{
		// Wait for the references to appear
		JSONObject results = synapseOne.query(queryString);
		assertNotNull(results);
		assertTrue(results.has("totalNumberOfResults"));
		assertNotNull(results.getLong("totalNumberOfResults"));
		long start = System.currentTimeMillis();
		while(results.getLong("totalNumberOfResults") < expectedCount){
			System.out.println("Waiting for query: "+queryString);
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for annotations to be published for query: "+queryString, elapse < RDS_WORKER_TIMEOUT);
			results = synapseOne.query(queryString);
			System.out.println(results);
		}
		return results;
	}
	
	/**
	 * Helper to wait for the expected query results.
	 * @param query
	 * @return
	 * @throws SynapseException
	 * @throws InterruptedException
	 */
	EntityQueryResults waitForQuery(EntityQuery query) throws SynapseException, InterruptedException {
		EntityQueryResults results = synapseOne.entityQuery(query);
		long start = System.currentTimeMillis();
		while(results.getTotalEntityCount() < 1){
			System.out.println("Waiting for query...");
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for query", elapse < RDS_WORKER_TIMEOUT);
			results = synapseOne.entityQuery(query);
		}
		return results;
	}
	
	@Test
	public void testEntityNaNAnnotations() throws SynapseException, InterruptedException, JSONException{
		// Add a unique annotation and query for it
		String key = "testEntityNaNAnnotations";
		Double value = Double.NaN;
		Annotations annos = synapseOne.getAnnotations(dataset.getId());
		annos.addAnnotation(key, value);
		annos.addAnnotation("foo", "bar");
		annos.addAnnotation("baz", 10.3D);
		synapseOne.updateAnnotations(dataset.getId(), annos);
		String queryString = "select id, "+key+" from entity where entity.id == \""+dataset.getId()+"\" and entity."+key+" == \"NaN\"";
		// Wait for the query
		JSONObject result = waitForQuery(queryString, 1L);
		// result should look like:
		// {"totalNumberOfResults":1,"results":[{"entity.testEntityNaNAnnotations":["NaN"],"entity.id":"syn1681661"}]}
		assertEquals(1, result.get("totalNumberOfResults"));
		assertEquals("NaN", ((JSONObject)result.getJSONArray("results").get(0)).getJSONArray("entity.testEntityNaNAnnotations").get(0));

		queryString = "select id from entity where entity."+key+" == \"NaN\"";
		result = waitForQuery(queryString, 1L);
		assertEquals(1, result.get("totalNumberOfResults"));
		assertEquals(dataset.getId(), ((JSONObject)result.getJSONArray("results").get(0)).get("entity.id"));
	}

	@Test
	public void testRetrieveApiKey() throws SynapseException {
		String apiKey = synapseOne.retrieveApiKey();
		assertNotNull(apiKey);
	}

	
	@Test
	public void testCertifiedUserQuiz() throws Exception {
		// before taking the test there's no passing record
		String myId = synapseOne.getMyProfile().getOwnerId();
		try {
			synapseOne.getCertifiedUserPassingRecord(myId);
			fail("Expected SynapseNotFoundException");
		} catch (SynapseNotFoundException e) {
			// as expected
		}
		Quiz quiz = synapseOne.getCertifiedUserTest();
		assertNotNull(quiz);
		assertNotNull(quiz.getId());
		QuizResponse response = new QuizResponse();
		response.setQuizId(quiz.getId());
		response.setQuestionResponses(new ArrayList<QuestionResponse>());
		// this quiz will fail
		PassingRecord pr = synapseOne.submitCertifiedUserTestResponse(response);
		assertEquals(new Long(0L), pr.getScore());
		assertFalse(pr.getPassed());
		assertEquals(quiz.getId(), pr.getQuizId());
		assertNotNull(pr.getResponseId());
		PassingRecord pr2 = synapseOne.getCertifiedUserPassingRecord(myId);
		assertEquals(pr, pr2);
		
		PaginatedResults<QuizResponse> qrs = adminSynapse.getCertifiedUserTestResponses(0L, 2L, myId);
		assertEquals(1, qrs.getResults().size());
		assertEquals(pr.getResponseId(), qrs.getResults().iterator().next().getId());
		qrs = adminSynapse.getCertifiedUserTestResponses(0L, 2L, null);
		assertEquals(1, qrs.getResults().size());
		assertEquals(pr.getResponseId(), qrs.getResults().iterator().next().getId());

		PaginatedResults<PassingRecord> prs = adminSynapse.getCertifiedUserPassingRecords(0L, 2L, myId);
		assertEquals(1, prs.getResults().size());
		assertEquals(pr, prs.getResults().iterator().next());

		adminSynapse.deleteCertifiedUserTestResponse(pr.getResponseId().toString());
	}

	@Test
	public void testLogService() throws Exception {
		String label1 = UUID.randomUUID().toString();
		String label2 = UUID.randomUUID().toString();
		LogEntry logEntry = new LogEntry();
		logEntry.setLabel(label1);
		logEntry.setMessage("message 1");
		synapseOne.logError(logEntry);
		logEntry.setMessage("message 2");
		synapseOne.logError(logEntry);
		logEntry.setLabel(label2);
		logEntry.setMessage("message 2");

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		new Exception().printStackTrace(pw );
		pw.flush();
		logEntry.setStacktrace(sw.toString());
		synapseOne.logError(logEntry);
	}
	
	@Test
	public void testQueryProjectId() throws SynapseException, JSONException, InterruptedException{
		String query = "select id from entity where projectId == '"+project.getId()+"'";
		JSONObject total = waitForQuery(query, 2L);
		Long count = total.getLong("totalNumberOfResults");
		assertEquals(new Long(2), count);
	}
	
	@Test
	public void testStructuredQuery() throws Exception {
		// setup a query to find the project by ID.
		EntityQuery query = new EntityQuery();
		query.setFilterByType(EntityType.project);
		query.setConditions(new ArrayList<Condition>(1));
		query.getConditions().add(EntityQueryUtils.buildCondition(EntityFieldName.id, Operator.EQUALS, project.getId()));
		// Run the query
		EntityQueryResults results = waitForQuery(query);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(1, results.getEntities().size());
		EntityQueryResult projectResult = results.getEntities().get(0);
		assertEquals(project.getId(), projectResult.getId());
	}
	
	@Test
	public void testGetEntityIdByAlias() throws SynapseException{
		// Set an alias for the project
		project.setAlias(UUID.randomUUID().toString().replaceAll("-", "_"));
		synapseOne.putEntity(project);
		EntityId lookupId = synapseOne.getEntityIdByAlias(project.getAlias());
		assertNotNull(lookupId);
		assertEquals(project.getId(), lookupId.getId());
	}
	
	@Test
	public void testGetEntityChildren() throws SynapseException{
		EntityChildrenRequest request = new EntityChildrenRequest();
		request.setParentId(project.getId());
		request.setIncludeTypes(Lists.newArrayList(EntityType.folder));
		EntityChildrenResponse response = synapseOne.getEntityChildren(request);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertEquals(1, response.getPage().size());
		EntityHeader header = response.getPage().get(0);
		assertNotNull(header);
		assertEquals(dataset.getId(), header.getId());
	}
	
	@Test
	public void testGetEntityChildrenProjects() throws SynapseException{
		EntityChildrenRequest request = new EntityChildrenRequest();
		// null parentId should get projects.
		request.setParentId(null);
		EntityChildrenResponse response = synapseOne.getEntityChildren(request);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertTrue(response.getPage().size() > 0);
		EntityHeader header = response.getPage().get(0);
		assertNotNull(header);
		assertEquals(Project.class.getName(), header.getType());
	}

	@Test
	public void testLookupEntity() throws SynapseException {
		assertEquals(dataset.getId(), synapseOne.lookupChild(project.getId(), dataset.getName()));
	}
}
