package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleCreate;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
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
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
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

	@BeforeAll
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
	
	@BeforeEach
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
	
	@AfterEach
	public void after() throws Exception {
		for (String id: toDelete) {
			try {
				adminSynapse.deleteEntityById(id);
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
	
	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseException e) { }
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
		assertThrows(SynapseForbiddenException.class, () -> synapseOne.deleteACL(project.getId()));
	}
	
	@Test
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
				ra.setAccessType(new HashSet<>()); // make it an empty list
				break;
			}
		}
		if (!foundIt) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(publicPrincipalId);
			ra.setAccessType(new HashSet<>()); // make it an empty list
			acl.getResourceAccess().add(ra);
		}
		// now push it, should get a SynapseBadRequestException
		assertThrows(SynapseBadRequestException.class, () -> synapseOne.updateACL(acl));
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
		Annotations annos = synapseOne.getAnnotationsV2(file.getId());
		assertNotNull(annos);
		assertEquals(file.getId(), annos.getId());
		assertNotNull(annos.getEtag());
		// Add some values
		AnnotationsV2TestUtils.putAnnotations(annos, "longKey", "999999", AnnotationsValueType.DOUBLE);
		Annotations updatedAnnos = synapseOne.updateAnnotationsV2(file.getId(), annos);
		assertNotNull(updatedAnnos);
		assertEquals(file.getId(), annos.getId());
		assertNotNull(updatedAnnos.getEtag());
		// The Etag should have changed
		assertNotEquals(updatedAnnos.getEtag(), annos.getEtag());

		// Get the "zero" e-tag for specific versions. See PLFM-1420.
		Entity datasetEntity = synapseOne.getEntityByIdForVersion(file.getId(), file.getVersionNumber());
		assertNotEquals(NodeConstants.ZERO_E_TAG, datasetEntity.getEtag());

		// Get the Users permission for this entity
		UserEntityPermissions uep = synapseOne.getUsersEntityPermissions(file.getId());
		assertNotNull(uep);
		assertTrue(uep.getCanEdit());
		assertTrue(uep.getCanView());
		assertTrue(synapseOne.canAccess(file.getId(), ACCESS_TYPE.UPDATE));
		assertTrue(synapseOne.canAccess(file.getId(), ACCESS_TYPE.READ));
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
		readPermission.setAccessType(new HashSet<>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD)));
		acl.getResourceAccess().add(readPermission);
		synapseOne.updateACL(acl);
		
		// now add a ToU restriction
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();

		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(file.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Collections.singletonList(rod));

		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("play nice");
		ar = adminSynapse.createAccessRequirement(ar);
		accessRequirementsToDelete.add(ar.getId());
		
		UserProfile otherProfile = synapseOne.getMyProfile();
		assertNotNull(otherProfile);
		
		// should not be able to download
		assertFalse(synapseTwo.canAccess(file.getId(), ACCESS_TYPE.DOWNLOAD));
		
		// now add the ToU approval
		AccessApproval aa = new AccessApproval();
		aa.setAccessorId(otherProfile.getOwnerId());
		aa.setRequirementId(ar.getId());
		
		synapseTwo.createAccessApproval(aa);
		
		// should be able to download
		assertTrue(synapseTwo.canAccess(file.getId(), ACCESS_TYPE.DOWNLOAD));
		
		ar.setTermsOfUse("play nicer");
		ar = adminSynapse.updateAccessRequirement(ar);
		assertEquals("play nicer", ar.getTermsOfUse());
		
		// ACL should reflect the first User's permission
		acl = synapseOne.getACL(project.getId());
		Set<ResourceAccess> ras = acl.getResourceAccess();
		boolean foundit = false;
		List<Long> foundPrincipals = new ArrayList<>();
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
		assertTrue(foundit, "didn't find "+profile.getUserName()+"("+profile.getOwnerId()+") but found "+foundPrincipals);
		
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
		Annotations annos = synapseOne.getAnnotationsV2(project.getId());
		AnnotationsV2TestUtils.putAnnotations(annos, "doubleAnno", "45.0001", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos, "string", "A string", AnnotationsValueType.STRING);
		annos = synapseOne.updateAnnotationsV2(project.getId(), annos);

		AccessControlList acl = synapseOne.getACL(project.getId());
		acl.setCreatedBy("John Doe");
		acl.setId(project.getId());
		synapseOne.updateACL(acl);

		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeEntity(true);
		request.setIncludeAnnotations(true);
		request.setIncludePermissions(true);
		request.setIncludeEntityPath(true);
		request.setIncludeHasChildren(true);
		request.setIncludeAccessControlList(true);
		request.setIncludeFileName(true);
		request.setIncludeRestrictionInformation(true);
		
		long startTime = System.nanoTime();
		EntityBundle entityBundle = synapseOne.getEntityBundleV2(project.getId(), request);
		long endTime = System.nanoTime();
		long requestTime = (endTime - startTime) / 1000000;
		System.out.println("Bundle request time was " + requestTime + " ms");

		assertNotNull(entityBundle.getRestrictionInformation());
		assertEquals(synapseOne.getEntityById(project.getId()), entityBundle.getEntity(),"Invalid fetched Entity in the EntityBundle");
		assertEquals(synapseOne.getAnnotationsV2(project.getId()), entityBundle.getAnnotations(), "Invalid fetched Annotations in the EntityBundle");
		assertEquals(synapseOne.getEntityPath(project.getId()), entityBundle.getPath(), "Invalid fetched EntityPath in the EntityBundle");
		assertEquals(synapseOne.getACL(project.getId()), entityBundle.getAccessControlList(), "Invalid fetched ACL in the EntityBundle");
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
		Set<ACCESS_TYPE> accessTypes = new HashSet<>();
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
		AnnotationsV2TestUtils.putAnnotations(a1, "doubleAnno", "45.0001", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(a1, "string", "A string", AnnotationsValueType.STRING);
		
		// Create ACL for this entity
		AccessControlList acl1 = new AccessControlList();
		Set<ResourceAccess> resourceAccesses = new HashSet<>();
		resourceAccesses.add(ra);
		acl1.setResourceAccess(resourceAccesses);
		
		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		ebc.setEntity(s1);
		ebc.setAnnotations(a1);
		ebc.setAccessControlList(acl1);
				
		EntityBundle response = synapseOne.createEntityBundleV2(ebc);
		
		Folder s2 = (Folder) response.getEntity();
		toDelete.add(s2.getId());
		assertNotNull(s2);
		assertNotNull(s2.getEtag(), "Etag should have been generated, but was not");
		assertEquals(s1.getName(), s2.getName());
		
		Annotations a2 = response.getAnnotations();
		assertNotNull(a2);
		assertNotNull(a2.getEtag(), "Etag should have been generated, but was not");
		assertEquals(a1.getAnnotations(), a2.getAnnotations(), "Retrieved Annotations in bundle do not match original ones");

		AccessControlList acl2 = response.getAccessControlList();
		assertNotNull(acl2);
		assertNotNull(acl2.getEtag(), "Etag should have been generated, but was not");
		assertEquals(acl1.getResourceAccess(), acl2.getResourceAccess(), "Retrieved ACL in bundle does not match original one");
	
		// Update the bundle, verify contents
		s2.setName("Dummy study 1 updated");
		AnnotationsV2TestUtils.putAnnotations(a2, "string2", "Another string", AnnotationsValueType.STRING);
		acl2.setModifiedBy("Update user");
		
		EntityBundleCreate ebc2 = new EntityBundleCreate();
		ebc2.setEntity(s2);
		ebc2.setAnnotations(a2);
		ebc2.setAccessControlList(acl2);
				
		EntityBundle response2 = synapseOne.updateEntityBundleV2(s2.getId(), ebc2);
		
		Folder s3 = (Folder) response2.getEntity();
		assertNotNull(s3);
		assertNotEquals(s2.getEtag(), s3.getEtag(),"Etag should have been updated, but was not");
		assertEquals(s2.getName(), s3.getName());
		
		Annotations a3 = response2.getAnnotations();
		assertNotNull(a3);
		assertNotEquals(a2.getEtag(), a3.getEtag(), "Etag should have been updated, but was not");
		assertEquals(a2.getAnnotations(), a3.getAnnotations(), "Retrieved Annotations in bundle do not match original ones");

		AccessControlList acl3 = response2.getAccessControlList();
		assertNotNull(acl3);
		assertNotEquals(acl2.getEtag(), acl3.getEtag(), "Etag should have been updated, but was not");
		assertEquals(acl2.getResourceAccess(), acl3.getResourceAccess(), "Retrieved ACL in bundle does not match original one");

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
		List<String> allDisplayNames = new ArrayList<>();
		for (UserProfile up : users.getResults()) {
			assertNotNull(up.getOwnerId());
			String displayName = up.getUserName();
			allDisplayNames.add(displayName);
			if (up.getOwnerId().equals(myPrincipalId)) foundSelf=true;
		}
		assertTrue(foundSelf, "Didn't find self, only found "+allDisplayNames);
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
		List<String> ids = new ArrayList<>();
		PaginatedResults<UserProfile> users = synapseOne.getUsers(0,100);
		for (UserProfile up : users.getResults()) {	
			if (up.getUserName() != null) {
				ids.add(up.getOwnerId());
			}
		}
		UserGroupHeaderResponsePage response = synapseOne.getUserGroupHeadersByIds(ids);
		Map<String, UserGroupHeader> headers = new HashMap<>();
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
		r.setSubjectIds(Arrays.asList(rod));


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
		
		// create approval for the requirement
		AccessApproval approval = new AccessApproval();
		approval.setAccessorId(otherProfile.getOwnerId());
		approval.setRequirementId(r.getId());
		AccessApproval created = synapseTwo.createAccessApproval(approval);
		
		// make sure we can retrieve by ID
		assertEquals(created, synapseTwo.getAccessApproval(created.getId()));
		
		// check that CAN download
		assertTrue(synapseTwo.canAccess(layer.getId(), ACCESS_TYPE.DOWNLOAD));

		String accessRequirementId = r.getId().toString();

		assertThrows(SynapseBadRequestException.class, () -> adminSynapse.revokeAccessApprovals(accessRequirementId, otherProfile.getOwnerId()));
		
		adminSynapse.deleteAccessRequirement(Long.parseLong(accessRequirementId));
	}

	@Test
	public void testUserSessionData() throws Exception {
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		assertNotNull(sessionToken, "Failed to find session token");
		UserProfile integrationTestUserProfile = userSessionData.getProfile();
		assertNotNull(integrationTestUserProfile, "Failed to get user profile from user session data");
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
		assertNotNull(url, "Failed to find: "+fileName+" on the classpath");
		File originalFile = new File(url.getFile());

		// Get the profile to update.
		UserProfile profile = synapseOne.getMyProfile();
		FileHandle fileHandle = synapseOne.multipartUpload(originalFile, null, true, false);
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


	@Test
	public void testRetrieveApiKey() throws SynapseException {
		String apiKey = synapseOne.retrieveApiKey();
		assertNotNull(apiKey);
	}

	
	@Test
	public void testCertifiedUserQuiz() throws Exception {
		// before taking the test there's no passing record
		String myId = synapseOne.getMyProfile().getOwnerId();
		assertThrows(SynapseNotFoundException.class, () -> synapseOne.getCertifiedUserPassingRecord(myId));
		Quiz quiz = synapseOne.getCertifiedUserTest();
		assertNotNull(quiz);
		assertNotNull(quiz.getId());
		QuizResponse response = new QuizResponse();
		response.setQuizId(quiz.getId());
		response.setQuestionResponses(new ArrayList<>());
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
