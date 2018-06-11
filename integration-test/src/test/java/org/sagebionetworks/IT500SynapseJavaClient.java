package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import org.junit.Ignore;
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
import org.sagebionetworks.repo.model.Count;
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
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.LogEntry;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
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
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.util.SerializationUtils;

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
	
	private static final String MOCK_ACCEPT_INVITATION_ENDPOINT = "https://www.synapse.org/#invit:";
	private static final String MOCK_ACCEPT_MEMB_RQST_ENDPOINT = "https://www.synapse.org/#request:";
	private static final String MOCK_TEAM_ENDPOINT = "https://www.synapse.org/#Team:";
	private static final String MOCK_NOTIFICATION_UNSUB_ENDPOINT = "https://www.synapse.org/#unsub:";
	private static final String TEST_EMAIL = "test@test.com";

	private List<String> toDelete;
	private List<Long> accessRequirementsToDelete;
	private List<String> handlesToDelete;
	private List<String> teamsToDelete;
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
	
	private Team getTestTeamFromResults(PaginatedResults<Team> results) {
		for (Team team : results.getResults()) {
			if (!bootstrappedTeams.contains(team.getId())) {
				return team;
			}
		}
		return null;
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
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo, UUID.randomUUID().toString(), "password", TEST_EMAIL);
		
		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);

		// Update this user's profile to contain a display name
		UserProfile profile = synapseTwo.getMyProfile();
		synapseTwo.updateMyProfile(profile);
	}
	
	@Before
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<String>();
		accessRequirementsToDelete = new ArrayList<Long>();
		handlesToDelete = new ArrayList<String>();
		teamsToDelete = new ArrayList<String>();
		
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
		byte[] content = "File contents".getBytes("UTF-8");
		String fileHandleId = synapseOne.multipartUpload(new ByteArrayInputStream(content),
				(long) content.length, "content", "text/plain", null, false, false).getId();
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandleId);

		file = synapseOne.createEntity(file);
		
		// Get the project using just using its ID. This is useful for cases where you
		//  do not know what you are getting until it arrives.
		Project clone = (Project) synapseOne.getEntityById(project.getId());
		assertNotNull(clone);
		assertNotNull(clone.getEntityType());
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
		
	}

	@Test
	public void testJavaClientCreateEntity() throws Exception {
		Folder study = new Folder();
		study.setParentId(project.getId());
		Folder createdStudy = synapseOne.createEntity(study);
		assertNotNull(createdStudy);
		assertNotNull(createdStudy.getId());
		assertNotNull(createdStudy.getUri());

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

	@Test
	public void testRetrieveSynapseTOU() throws Exception {
		String termsOfUse = synapseOne.getSynapseTermsOfUse();
		assertNotNull(termsOfUse);
		assertTrue(termsOfUse.length()>100);
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
	public void testTeamAPI() throws SynapseException, IOException, InterruptedException {
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
			synapseOne.getTeamIcon(createdTeam.getId());
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
		FileHandle fileHandle = synapseOne.multipartUpload(file, null, true, false);
		handlesToDelete.add(fileHandle.getId());

		// update the Team with the icon
		createdTeam.setIcon(fileHandle.getId());
		Team updatedTeam = synapseOne.updateTeam(createdTeam);
		// get the icon url
		URL url = synapseOne.getTeamIcon(updatedTeam.getId());
		assertNotNull(url);
		// check that we can download the Team icon to a file
		File target = File.createTempFile("temp", null);
		synapseOne.downloadTeamIcon(updatedTeam.getId(), target);
		assertTrue(target.length()>0);
		// query for all teams
		PaginatedResults<Team> teams = waitForTeams(null, 50, 0);
		assertEquals(getBootstrapCountPlus(1L), teams.getTotalNumberOfResults());
		assertEquals(updatedTeam, getTestTeamFromResults(teams));
		// make sure pagination works
		teams = waitForTeams(null, 10, 1);
		assertEquals(getBootstrapCountPlus(0L), teams.getResults().size());
		
		// query for all teams, based on name fragment
		// need to update cache.  the service to trigger an update
		// requires admin privileges, so we log in as an admin:
		teams = waitForTeams(name.substring(0, 3),1, 0);
		assertEquals(2L, teams.getTotalNumberOfResults());
		assertEquals(updatedTeam, getTestTeamFromResults(teams));
		// again, make sure pagination works
		teams = waitForTeams(name.substring(0, 3), 10, 1);
		assertEquals(0L, teams.getResults().size());
		
		List<Team> teamList = synapseOne.listTeams(Collections.singletonList(Long.parseLong(updatedTeam.getId())));
		assertEquals(1L, teamList.size());
		assertEquals(updatedTeam, teamList.get(0));
		
		// query for team members.  should get just the creator
		PaginatedResults<TeamMember> members = waitForTeamMembers(updatedTeam.getId(), null, 1, 0);
		TeamMember tm = members.getResults().get(0);
		assertEquals(myPrincipalId, tm.getMember().getOwnerId());
		assertEquals(updatedTeam.getId(), tm.getTeamId());
		assertTrue(tm.getIsAdmin());
		assertEquals(1L, synapseOne.countTeamMembers(updatedTeam.getId(), null));
		
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
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(createdTeam.getId());
		synapseTwo.createMembershipRequest(mrs, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
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
		String myDisplayName = myProfile.getUserName();
		members = waitForTeamMembers(updatedTeam.getId(), myDisplayName, 1, 0);
		assertEquals(1L, synapseOne.countTeamMembers(updatedTeam.getId(), myDisplayName));
		assertEquals(myPrincipalId, members.getResults().get(0).getMember().getOwnerId());
		assertTrue(members.getResults().get(0).getIsAdmin());
		
		List<TeamMember> teamMembers = synapseOne.listTeamMembers(updatedTeam.getId(), Collections.singletonList(Long.parseLong(myPrincipalId)));
		assertEquals(members.getResults(), teamMembers);

		teamMembers = synapseOne.listTeamMembers(Collections.singletonList(Long.parseLong(updatedTeam.getId())), myPrincipalId);
		assertEquals(members.getResults(), teamMembers);

		synapseOne.addTeamMember(updatedTeam.getId(), otherPrincipalId, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		
		tms = synapseTwo.getTeamMembershipStatus(updatedTeam.getId(), otherPrincipalId);
		assertEquals(updatedTeam.getId(), tms.getTeamId());
		assertEquals(otherPrincipalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());

		// query for team members.  should get creator as well as new member back
		members = waitForTeamMembers(updatedTeam.getId(), null, 2, 0);
		assertEquals(2L, members.getResults().size());
		
		assertEquals(2L, synapseOne.countTeamMembers(updatedTeam.getId(), null));

		// query for team members using name fragment
		members = waitForTeamMembers(updatedTeam.getId(), otherDName.substring(0,otherDName.length()-4), 1, 0);
		assertEquals(1L, synapseOne.countTeamMembers(updatedTeam.getId(), otherDName.substring(0,otherDName.length()-4)));
		
		TeamMember otherMember = members.getResults().get(0);
		assertEquals(otherPrincipalId, otherMember.getMember().getOwnerId());
		assertFalse(otherMember.getIsAdmin());
		
		// make the other member an admin
		synapseOne.setTeamMemberPermissions(createdTeam.getId(), otherPrincipalId, true);
		
		members = waitForTeamMembers(createdTeam.getId(), otherDName.substring(0,otherDName.length()-4), 1, 0);
		// now the other member is an admin
		otherMember = members.getResults().get(0);
		assertEquals(otherPrincipalId, otherMember.getMember().getOwnerId());
		assertTrue(otherMember.getIsAdmin());

		// remove admin privileges
		synapseOne.setTeamMemberPermissions(createdTeam.getId(), otherPrincipalId, false);

		// now repeat the permissions change, but by accessing the ACL
		AccessControlList acl = synapseOne.getTeamACL(createdTeam.getId());

		Set<ACCESS_TYPE> otherAccessTypes = null;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getPrincipalId().equals(otherPrincipalId)) otherAccessTypes=ra.getAccessType();
		}
		// since 'other' is not an admin, he won't have his own entry in the ACL
		assertTrue(otherAccessTypes==null);
		Set<ResourceAccess> origResourceAccess = new HashSet<ResourceAccess>(acl.getResourceAccess());
		
		ResourceAccess adminRa = new ResourceAccess();
		adminRa.setPrincipalId(Long.parseLong(otherPrincipalId));
		adminRa.setAccessType(ModelConstants.TEAM_ADMIN_PERMISSIONS);
		acl.getResourceAccess().add(adminRa);
		AccessControlList updatedACL = synapseOne.updateTeamACL(acl);
		assertEquals(acl.getResourceAccess(), updatedACL.getResourceAccess());

		// finally, restore
		updatedACL.setResourceAccess(origResourceAccess);
		synapseOne.updateTeamACL(updatedACL);
		
		// query for teams based on member's id
		teams = synapseOne.getTeamsForUser(otherPrincipalId, 1, 0);
		assertEquals(2L, teams.getTotalNumberOfResults());
		assertEquals(updatedTeam, teams.getResults().get(0));
		// remove the member from the team
		synapseOne.removeTeamMember(updatedTeam.getId(), otherPrincipalId);

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
	
	private PaginatedResults<Team> waitForTeams(String prefix, int limit, int offset) throws SynapseException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			PaginatedResults<Team> teams = synapseOne.getTeams(prefix,limit, offset);
			if(teams.getTotalNumberOfResults() < 1){
				Thread.sleep(1000);
				System.out.println("Waiting for principal prefix worker");
				if(System.currentTimeMillis() - start > RDS_WORKER_TIMEOUT){
					fail("Timed out waiting for principal prefix worker.");
				}
			}else{
				return teams;
			}
		}
	}
	
	private PaginatedResults<TeamMember> waitForTeamMembers(String teamId, String prefix, int limit, int offset) throws SynapseException, InterruptedException{
		long start = System.currentTimeMillis();
		while (true){
			long count = synapseOne.countTeamMembers(teamId, prefix);
			if(count < 1L){
				System.out.println("Waiting for principal prefix worker");
				Thread.sleep(1000);
				if(System.currentTimeMillis() - start > RDS_WORKER_TIMEOUT){
					fail("Timed out waiting for principal prefix worker.");
				}
			}else{
				return synapseOne.getTeamMembers(teamId, prefix, limit, offset);
			}
		}
	}

	@Test
	public void testTeamRestrictionRoundTrip() throws SynapseException, UnsupportedEncodingException {
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
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.TEAM);
		rod.setId(createdTeam.getId());
		tou.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		tou = adminSynapse.createAccessRequirement(tou);
		assertNotNull(tou.getId());
		accessRequirementsToDelete.add(tou.getId());
		
		// Query AccessRestriction
		PaginatedResults<AccessRequirement> paginatedResults;
		paginatedResults = adminSynapse.getAccessRequirements(rod, 10L, 0L);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		paginatedResults = adminSynapse.getAccessRequirements(rod, 10L, 10L);
		assertTrue(paginatedResults.getResults().isEmpty());

		// Query Unmet AccessRestriction
		paginatedResults = synapseTwo.getUnmetAccessRequirements(rod, ACCESS_TYPE.PARTICIPATE, 10L, 0L);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		// Create AccessApproval
		AccessApproval aa = new AccessApproval();
		aa.setRequirementId(tou.getId());
		synapseTwo.createAccessApproval(aa);
		
		// Query AccessRestriction
		paginatedResults = adminSynapse.getAccessRequirements(rod, 10L, 0L);
		AccessRequirementUtil.checkTOUlist(paginatedResults, tou);
		
		// Query Unmet AccessRestriction (since the requirement is now met, the list is empty)
		paginatedResults = synapseTwo.getUnmetAccessRequirements(rod, ACCESS_TYPE.PARTICIPATE, 10L, 0L);
		assertEquals(0L, paginatedResults.getTotalNumberOfResults());
		assertTrue(paginatedResults.getResults().isEmpty());
		
		assertEquals(paginatedResults, synapseTwo.getUnmetAccessRequirements(rod, ACCESS_TYPE.PARTICIPATE, 10L, 0L));
	}

	@Test
	public void testMembershipInvitationAPI() throws Exception {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		UserProfile synapseOneProfile = synapseOne.getMyProfile();
		String myPrincipalId = synapseOneProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create an invitation
		MembershipInvitation dto = new MembershipInvitation();
		UserProfile inviteeUserProfile = synapseTwo.getMyProfile();
		List<String> inviteeEmails = inviteeUserProfile.getEmails();
		assertEquals(1, inviteeEmails.size());
		
		String inviteePrincipalId = inviteeUserProfile.getOwnerId();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		dto.setInviteeId(inviteePrincipalId);
		String message = "Please accept this invitation";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		MembershipInvitation created = synapseOne.createMembershipInvitation(dto, MOCK_ACCEPT_INVITATION_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertEquals(myPrincipalId, created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(expiresOn, created.getExpiresOn());
		assertNotNull(created.getId());
		assertEquals(inviteePrincipalId, created.getInviteeId());
		assertEquals(message, created.getMessage());
		assertEquals(createdTeam.getId(), created.getTeamId());
		
		// check that open invitation count is 1
		Count openInvitationCount = synapseTwo.getOpenMembershipInvitationCount();
		assertEquals(1L, openInvitationCount.getCount().longValue());

		// get the invitation
		MembershipInvitation retrieved = synapseOne.getMembershipInvitation(created.getId());
		assertEquals(created, retrieved);
		
		{
			// query for invitations based on user
			PaginatedResults<MembershipInvitation> invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, null, 1, 0);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			MembershipInvitation invitation = invitations.getResults().get(0);
			assertEquals(expiresOn, invitation.getExpiresOn());
			assertEquals(message, invitation.getMessage());
			assertEquals(createdTeam.getId(), invitation.getTeamId());
			assertEquals(inviteePrincipalId, invitation.getInviteeId());
			// check pagination
			invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, null, 2, 1);
			assertEquals(0L, invitations.getResults().size());
			// query for invitations based on user and team
			invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, createdTeam.getId(), 1, 0);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			MembershipInvitation invitation2 = invitations.getResults().get(0);
			assertEquals(invitation, invitation2);
			// again, check pagination
			invitations = synapseOne.getOpenMembershipInvitations(inviteePrincipalId, createdTeam.getId(), 2, 1);
			assertEquals(1L, invitations.getTotalNumberOfResults());
			assertEquals(0L, invitations.getResults().size());
		}
		
		// query for invitation SUBMISSIONs based on team
		{
			PaginatedResults<MembershipInvitation> invitationSubmissions =
					synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), null, 1, 0);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			MembershipInvitation submission = invitationSubmissions.getResults().get(0);
			assertEquals(created, submission);
			// check pagination
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), null, 2, 1);
			assertEquals(0L, invitationSubmissions.getResults().size());
			// query for SUBMISSIONs based on team and invitee
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), inviteePrincipalId, 1, 0);
			assertEquals(1L, invitationSubmissions.getTotalNumberOfResults());
			assertEquals(created, invitationSubmissions.getResults().get(0));
			// again, check pagination
			invitationSubmissions = synapseOne.getOpenMembershipInvitationSubmissions(createdTeam.getId(), inviteePrincipalId, 2, 1);
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

		// create an invitation with null inviteeId and non null inviteeEmail
		dto.setInviteeId(null);
		dto.setInviteeEmail(TEST_EMAIL);
		MembershipInvitation mis = synapseOne.createMembershipInvitation(dto, MOCK_ACCEPT_INVITATION_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		InviteeVerificationSignedToken token = synapseTwo.getInviteeVerificationSignedToken(mis.getId());
		// test if getInviteeVerificationSignedToken succeeded
		assertNotNull(token);
		String inviteeId = inviteeUserProfile.getOwnerId();
		assertEquals(inviteeId, token.getInviteeId());
		assertEquals(mis.getId(), token.getMembershipInvitationId());

		// update the inviteeIinviteeUserProfile.getOwnerId() of the invitation
		synapseTwo.updateInviteeId(mis.getId(), token);
		mis = synapseTwo.getMembershipInvitation(mis.getId());
		// test if updateInviteeId succeeded
		assertEquals(inviteeId, mis.getInviteeId());

		// delete the second invitation
		synapseOne.deleteMembershipInvitation(mis.getId());
		try {
			synapseOne.getMembershipInvitation(mis.getId());
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
		MembershipRequest dto = new MembershipRequest();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		String message = "Please accept this request";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		MembershipRequest created = synapseTwo.createMembershipRequest(dto, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertEquals(otherPrincipalId, created.getCreatedBy());
		assertNotNull(created.getCreatedOn());
		assertEquals(expiresOn, created.getExpiresOn());
		assertNotNull(created.getId());
		assertEquals(otherPrincipalId, created.getUserId());
		assertEquals(message, created.getMessage());
		assertEquals(createdTeam.getId(), created.getTeamId());
		// get the request
		MembershipRequest retrieved = synapseTwo.getMembershipRequest(created.getId());
		assertEquals(created, retrieved);

		// check that request count is 1
		Count requestCount = synapseOne.getOpenMembershipRequestCount();
		assertEquals(1L, requestCount.getCount().longValue());

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
			PaginatedResults<MembershipRequest> requestSubmissions = synapseTwo.getOpenMembershipRequestSubmissions(otherPrincipalId, null, 1, 0);
			assertEquals(1L, requestSubmissions.getTotalNumberOfResults());
			MembershipRequest requestSubmission = requestSubmissions.getResults().get(0);
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
	
	@Ignore // See PLFM-4131.
	@Test
	public void testMembershipInvitationAndAcceptanceViaNotification() throws Exception {
		// create a Team
		String name = "Test-Team-Name";
		String description = "Test-Team-Description";
		UserProfile synapseOneProfile = synapseOne.getMyProfile();
		String myPrincipalId = synapseOneProfile.getOwnerId();
		assertNotNull(myPrincipalId);
		Team team = new Team();
		team.setName(name);
		team.setDescription(description);
		Team createdTeam = synapseOne.createTeam(team);
		teamsToDelete.add(createdTeam.getId());
		
		// create an invitation
		MembershipInvitation dto = new MembershipInvitation();
		UserProfile inviteeUserProfile = synapseTwo.getMyProfile();
		List<String> inviteeEmails = inviteeUserProfile.getEmails();
		assertEquals(1, inviteeEmails.size());
		String inviteeEmail = inviteeEmails.get(0);
		String inviteeNotification = EmailValidationUtil.getBucketKeyForEmail(inviteeEmail);
		if (EmailValidationUtil.doesFileExist(inviteeNotification, 2000L))
			EmailValidationUtil.deleteFile(inviteeNotification);
		
		String inviteePrincipalId = inviteeUserProfile.getOwnerId();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		dto.setInviteeId(inviteePrincipalId);
		String message = "Please accept this invitation";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		synapseOne.createMembershipInvitation(dto, MOCK_ACCEPT_INVITATION_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		
		// check that a notification was sent to the invitee
		assertTrue(EmailValidationUtil.doesFileExist(inviteeNotification, 60000L));
		
		// make sure there's no lingering inviter notification
		String inviterNotification = EmailValidationUtil.getBucketKeyForEmail(synapseOneProfile.getEmails().get(0));
		if (EmailValidationUtil.doesFileExist(inviterNotification, 2000L))
			EmailValidationUtil.deleteFile(inviterNotification);
		
		// now get the embedded tokens
		String startString = "<a href=\""+MOCK_ACCEPT_INVITATION_ENDPOINT;
		String endString = "\"";
		String jsst = EmailValidationUtil.getTokenFromFile(inviteeNotification, startString, endString);
		JoinTeamSignedToken joinTeamSignedToken = SerializationUtils.hexDecodeAndDeserialize(jsst, JoinTeamSignedToken.class);

		startString = "<a href=\""+MOCK_NOTIFICATION_UNSUB_ENDPOINT;
		endString = "\"";
		String nsst = EmailValidationUtil.getTokenFromFile(inviteeNotification, startString, endString);
		NotificationSettingsSignedToken notificationSettingsSignedToken = 
				SerializationUtils.hexDecodeAndDeserialize(nsst, NotificationSettingsSignedToken.class);
		// delete the message
		EmailValidationUtil.deleteFile(inviteeNotification);
		
		ResponseMessage m = synapseTwo.addTeamMember(joinTeamSignedToken, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(m.getMessage());
		
		// now I should be in the team
		TeamMembershipStatus tms = synapseTwo.getTeamMembershipStatus(createdTeam.getId(), inviteePrincipalId);
		assertTrue(tms.getIsMember());
		
		m = synapseTwo.updateNotificationSettings(notificationSettingsSignedToken);
		assertNotNull(m.getMessage());
		
		// settings should now be updated
		inviteeUserProfile = synapseTwo.getMyProfile();
		assertFalse(inviteeUserProfile.getNotificationSettings().getSendEmailNotifications());
		
		// finally, the invitER should have been notified that the invitEE joined the team
		assertTrue(EmailValidationUtil.doesFileExist(inviterNotification, 60000L));
		EmailValidationUtil.deleteFile(inviterNotification);
	}

	@Ignore // See PLFM-4131
	@Test
	public void testMembershipRequestAndAcceptanceViaNotification() throws Exception {
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
		
		// clear any existing notification
		UserProfile adminUserProfile = synapseOne.getMyProfile();
		List<String> adminEmails = adminUserProfile.getEmails();
		assertEquals(1, adminEmails.size());
		String adminEmail = adminEmails.get(0);
		String adminNotification = EmailValidationUtil.getBucketKeyForEmail(adminEmail);
		if (EmailValidationUtil.doesFileExist(adminNotification, 2000L))
			EmailValidationUtil.deleteFile(adminNotification);

		// create a request
		UserProfile requesterProfile = synapseTwo.getMyProfile();
		String requesterPrincipalId = requesterProfile.getOwnerId();
		MembershipRequest dto = new MembershipRequest();
		Date expiresOn = new Date(System.currentTimeMillis()+100000L);
		dto.setExpiresOn(expiresOn);
		String message = "Please accept this request";
		dto.setMessage(message);
		dto.setTeamId(createdTeam.getId());
		synapseTwo.createMembershipRequest(dto, MOCK_ACCEPT_MEMB_RQST_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);

		// check that a notification was sent to the admin
		assertTrue(EmailValidationUtil.doesFileExist(adminNotification, 60000L));
		
		// make sure there's no lingering requester notification
		String requesterNotification = EmailValidationUtil.getBucketKeyForEmail(requesterProfile.getEmails().get(0));
		if (EmailValidationUtil.doesFileExist(requesterNotification, 2000L))
			EmailValidationUtil.deleteFile(requesterNotification);
		
		// now get the embedded tokens
		String startString = "<a href=\""+MOCK_ACCEPT_MEMB_RQST_ENDPOINT;
		String endString = "\"";
		String jsst = EmailValidationUtil.getTokenFromFile(adminNotification, startString, endString);
		JoinTeamSignedToken joinTeamSignedToken = SerializationUtils.hexDecodeAndDeserialize(jsst, JoinTeamSignedToken.class);

		startString = "<a href=\""+MOCK_NOTIFICATION_UNSUB_ENDPOINT;
		endString = "\"";
		String nsst = EmailValidationUtil.getTokenFromFile(adminNotification, startString, endString);
		NotificationSettingsSignedToken notificationSettingsSignedToken = 
				SerializationUtils.hexDecodeAndDeserialize(nsst, NotificationSettingsSignedToken.class);
		// delete the message
		EmailValidationUtil.deleteFile(adminNotification);
		
		ResponseMessage m = synapseOne.addTeamMember(joinTeamSignedToken, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(m.getMessage());
		
		// now requester should be in the team
		TeamMembershipStatus tms = synapseTwo.getTeamMembershipStatus(createdTeam.getId(), requesterPrincipalId);
		assertTrue(tms.getIsMember());
		
		m = synapseOne.updateNotificationSettings(notificationSettingsSignedToken);
		assertNotNull(m.getMessage());
		
		// admin settings should now be updated
		adminUserProfile = synapseOne.getMyProfile();
		assertFalse(adminUserProfile.getNotificationSettings().getSendEmailNotifications());
		
		// finally, the requester should have been notified that the admin added her to the team
		// TODO this fails
		//assertTrue("Can't find file "+requesterNotification, EmailValidationUtil.doesFileExist(requesterNotification));
		EmailValidationUtil.deleteFile(requesterNotification);
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
	public void testGetEntityIdByAlais() throws SynapseException{
		// Set an alias for the project
		project.setAlias(UUID.randomUUID().toString().replaceAll("-", "_"));
		synapseOne.putEntity(project);
		EntityId lookupId = synapseOne.getEntityIdByAlias(project.getAlias());
		assertNotNull(lookupId);
		assertEquals(project.getId(), lookupId.getId());
	}
	
	@Test
	public void testGetEntityChildern() throws SynapseException{
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
	public void testGetEntityChildernProjects() throws SynapseException{
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
