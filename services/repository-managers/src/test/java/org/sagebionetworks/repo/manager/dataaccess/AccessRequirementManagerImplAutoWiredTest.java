package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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
	private EntityAclManager entityAclManager;
	
	private UserInfo adminUserInfo;
	private UserInfo testUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	
	private String entityId;
	private String entityId2;
	private String childId;

	private Team team;
	
	private AccessRequirement ar;

	@BeforeEach
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
		nodeManager.createNewNode(fileNode, adminUserInfo);

		Node childNode = new Node();
		childNode.setName("Child");
		childNode.setNodeType(EntityType.link);
		childNode.setParentId(entityId);
		childId = nodeManager.createNewNode(childNode, adminUserInfo);

		AccessControlList acl = entityAclManager.getACL(rootId, adminUserInfo);
		Set<ResourceAccess> raSet = acl.getResourceAccess();
		ResourceAccess ra = new ResourceAccess();
		String testUserId = testUserInfo.getId().toString();
		ra.setPrincipalId(Long.parseLong(testUserId));
		Set<ACCESS_TYPE> atSet = new HashSet<ACCESS_TYPE>();
		atSet.add(ACCESS_TYPE.CREATE);
		ra.setAccessType(atSet);
		raSet.add(ra);
		entityAclManager.updateACL(acl, adminUserInfo);

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

	@AfterEach
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
	
	private static ManagedACTAccessRequirement newManagedAccessRequirement(String entityId, Long expirationPeriod, String renewalDetailsUrl) {
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setDescription("Some description");
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setExpirationPeriod(expirationPeriod);
		return ar;
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
	public void testCreateEntityAccessRequirementWithExpirationPeriod() throws Exception {
		Long expirationPeriod = 365 * 24 * 60 * 60 * 1000L;
		
		ManagedACTAccessRequirement expected = newManagedAccessRequirement(entityId, expirationPeriod, null);
		
		ManagedACTAccessRequirement result = accessRequirementManager.createAccessRequirement(adminUserInfo, expected);
		ar = result;
		
		expected.setId(result.getId());
		expected.setEtag(result.getEtag());
		expected.setModifiedOn(result.getModifiedOn());
		expected.setModifiedBy(result.getModifiedBy());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testCreateEntityAccessRequirementWithRenewalUrl() throws Exception {
		Long expirationPeriod = 365 * 24 * 60 * 60 * 1000L;
		String renewalUrl = "https://somedomain.org";
		
		ManagedACTAccessRequirement expected = newManagedAccessRequirement(entityId, expirationPeriod, renewalUrl);
		
		ManagedACTAccessRequirement result = accessRequirementManager.createAccessRequirement(adminUserInfo, expected);
		ar = result;
		
		expected.setId(result.getId());
		expected.setEtag(result.getEtag());
		expected.setModifiedOn(result.getModifiedOn());
		expected.setModifiedBy(result.getModifiedBy());
		
		assertEquals(expected, result);
		
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

	@Test
	public void testCreateAccessRequirementBadParam1() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setSubjectIds(null);
		assertThrows(IllegalArgumentException.class, ()-> {
				ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
			});
	}
	
	@Test
	public void testCreateAccessRequirementBadParam2() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setAccessType(null);
		assertThrows(IllegalArgumentException.class, ()-> {
			accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		});
	}
	
	@Test
	public void testEntityCreateAccessRequirementForbidden() throws Exception {
		ar = newEntityAccessRequirement(entityId2);
		assertThrows(UnauthorizedException.class, ()-> {
			accessRequirementManager.createAccessRequirement(testUserInfo, ar);
		});
	}

	@Test
	public void testTeamCreateAccessRequirementForbidden() throws Exception {
		ar = newTeamAccessRequirement(team.getId());
		// this user will not have permission to add a restriction to the evaluation
		assertThrows(UnauthorizedException.class, ()-> {
			 accessRequirementManager.createAccessRequirement(testUserInfo, ar);
		});
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
	public void testDeleteAccessRequirement() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		String accessRequirementId = String.valueOf(ar.getId());
		// Call under test
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, accessRequirementId);
		String expectedMessage = "An access requirement with id "+ accessRequirementId + " cannot be found.";
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			accessRequirementManager.getAccessRequirement(accessRequirementId);
		});
		assertEquals(expectedMessage, exception.getMessage());
	}

}
