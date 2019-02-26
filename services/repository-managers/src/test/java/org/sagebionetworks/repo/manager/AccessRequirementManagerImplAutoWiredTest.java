package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessRequirementManagerImplAutoWiredTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;

	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	private UserInfo adminUserInfo;
	private UserInfo testUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	
	private String entityId;
	private String entityId2;
	private String childId;
	private String fileId;

	private Team team;
	
	private AccessRequirement ar;

	@Before
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserInfo = userManager.getUserInfo(userManager.createUser(user));
		testUserInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.link);
		node.setParentId(rootId);
		entityId = nodeManager.createNewNode(node, adminUserInfo);
		
		Node fileNode = new Node();
		fileNode.setName("File");
		fileNode.setNodeType(EntityType.file);
		fileNode.setParentId(rootId);
		fileId = nodeManager.createNewNode(fileNode, adminUserInfo);

		Node childNode = new Node();
		childNode.setName("Child");
		childNode.setNodeType(EntityType.link);
		childNode.setParentId(entityId);
		childId = nodeManager.createNewNode(childNode, adminUserInfo);

		AccessControlList acl = entityPermissionsManager.getACL(rootId, adminUserInfo);
		Set<ResourceAccess> raSet = acl.getResourceAccess();
		ResourceAccess ra = new ResourceAccess();
		String testUserId = testUserInfo.getId().toString();
		ra.setPrincipalId(Long.parseLong(testUserId));
		Set<ACCESS_TYPE> atSet = new HashSet<ACCESS_TYPE>();
		atSet.add(ACCESS_TYPE.CREATE);
		ra.setAccessType(atSet);
		raSet.add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);

		rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project);
		String rootId2 = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId2); // the deletion of 'rootId' will cascade to its children
		node = new Node();
		node.setName("B");
		node.setNodeType(EntityType.link);
		node.setParentId(rootId2);
		entityId2 = nodeManager.createNewNode(node, adminUserInfo);

		team = new Team();
		team.setName("AccessRequirementManagerImplAutoWiredTest");
		team = teamManager.create(adminUserInfo, team);
	}

	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		
		if (ar!=null && ar.getId()!=null && accessRequirementManager!=null) {
			accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
		}
		userManager.deletePrincipal(adminUserInfo, testUserInfo.getId());
		if (team!=null) teamManager.delete(adminUserInfo, team.getId());
	}
	
	private static TermsOfUseAccessRequirement newEntityAccessRequirement(String entityId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}

	private static TermsOfUseAccessRequirement newTeamAccessRequirement(String teamId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(teamId);
		rod.setType(RestrictableObjectType.TEAM);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.PARTICIPATE);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	@Test
	public void testCreateEntityAccessRequirement() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test
	public void testCreateLockAccessRequirement() throws Exception {
		String jiraKey = "jiraKey";
		ar = AccessRequirementManagerImpl.newLockAccessRequirement(adminUserInfo, entityId, jiraKey);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateAccessRequirementBadParam1() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setSubjectIds(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateAccessRequirementBadParam2() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setAccessType(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testEntityCreateAccessRequirementForbidden() throws Exception {
		ar = newEntityAccessRequirement(entityId2);
		ar = accessRequirementManager.createAccessRequirement(testUserInfo, ar);
	}

	@Test(expected=UnauthorizedException.class)
	public void testTeamCreateAccessRequirementForbidden() throws Exception {
		ar = newTeamAccessRequirement(team.getId());
		// this user will not have permission to add a restriction to the evaluation
		ar = accessRequirementManager.createAccessRequirement(testUserInfo, ar);
	}
	
	@Test
	public void testUpdateAccessRequirement() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertEquals(new Long(0), ar.getVersionNumber());

		// ensure that the 'modifiedOn' date is later
		Thread.sleep(100L);
		long arModifiedOn = ar.getModifiedOn().getTime();
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>()); // change the entity id list
		AccessRequirement ar2 = accessRequirementManager.updateAccessRequirement(adminUserInfo, ar.getId().toString(), ar);
		assertTrue(ar2.getModifiedOn().getTime()-arModifiedOn>0);
		assertTrue(ar2.getSubjectIds().isEmpty());
		assertEquals(new Long(1), ar.getVersionNumber());
		assertFalse(ar.getEtag().equals(ar2.getEtag()));
		
		AccessRequirement ar3 = accessRequirementManager.updateAccessRequirement(adminUserInfo, ar2.getId().toString(), ar2);
		assertEquals(new Long(2), ar3.getVersionNumber());
	}
	
	@Test
	public void testGetAccessRequirement() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		AccessRequirement retrieved = accessRequirementManager.getAccessRequirement(ar.getId().toString());
		assertEquals(ar, retrieved);
	}
	
	@Test
	public void testGetInheritedAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(childId);
		rod.setType(RestrictableObjectType.ENTITY);
		List<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(1, ars.size());
	}
	
	@Test
	public void testGetUnmetEntityAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserInfo;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		
		List<AccessRequirement> ars = accessRequirementManager.getAllUnmetAccessRequirements(otherUserInfo, rod, ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, ars.size());
	}
	
	@Test
	public void testGetInheritedUnmetEntityAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserInfo;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(childId);
		rod.setType(RestrictableObjectType.ENTITY);
		
		List<AccessRequirement> ars = accessRequirementManager.getAllUnmetAccessRequirements(otherUserInfo, rod, ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, ars.size());
	}
	
	// entity owner does have unmet access requirements, for non-file
	@Test
	public void testGetUnmetEntityAccessRequirementsOwnerNonFile() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		
		List<AccessRequirement> ars = accessRequirementManager.getAllUnmetAccessRequirements(adminUserInfo, rod, ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, ars.size());
	}
	
	// File owner never has unmet access requirements
	@Test
	public void testGetUnmetEntityAccessRequirementsOwnerFile() throws Exception {
		ar = newEntityAccessRequirement(fileId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(fileId);
		rod.setType(RestrictableObjectType.ENTITY);
		List<AccessRequirement> ars = accessRequirementManager.getAllUnmetAccessRequirements(adminUserInfo, rod, ACCESS_TYPE.DOWNLOAD);
		assertEquals(0, ars.size());
	}

	@Test
	public void testGetRestrictionInformationInherited() {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(childId);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse info = accessRequirementManager.getRestrictionInformation(adminUserInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
	}
}
