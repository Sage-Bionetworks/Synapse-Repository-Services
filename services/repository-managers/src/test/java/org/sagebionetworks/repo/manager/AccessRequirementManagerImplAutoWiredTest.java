package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
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
	private EvaluationManager evaluationManager;
	
	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	
	private UserInfo adminUserInfo;
	private UserInfo testUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	
	private String entityId;
	private String entityId2;
	private String childId;
	
	private Evaluation evaluation;
	private Evaluation evaluation2;
	private Evaluation adminEvaluation;
	
	
	private Team team;
	
	private AccessRequirement ar;

	@Before
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserInfo = userManager.getUserInfo(userManager.createUser(user));
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project.name());
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.layer.name());
		node.setParentId(rootId);
		entityId = nodeManager.createNewNode(node, adminUserInfo);
		
		Node childNode = new Node();
		childNode.setName("Child");
		childNode.setNodeType(EntityType.layer.name());
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

		evaluation = newEvaluation("test-name", testUserInfo, rootId);
		adminEvaluation = newEvaluation("admin-name", adminUserInfo, rootId);

		rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project.name());
		String rootId2 = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId2); // the deletion of 'rootId' will cascade to its children
		node = new Node();
		node.setName("B");
		node.setNodeType(EntityType.layer.name());
		node.setParentId(rootId2);
		entityId2 = nodeManager.createNewNode(node, adminUserInfo);

		evaluation2 = newEvaluation("test-name2", adminUserInfo, rootId2);
		
		team = new Team();
		team.setName("AccessRequirementManagerImplAutoWiredTest");
		team = teamManager.create(adminUserInfo, team);
	}
	
	private Evaluation newEvaluation(String name, UserInfo userInfo, String contentSource) throws NotFoundException {
		Evaluation evaluation = new Evaluation();
		evaluation.setName(name);
		evaluation.setCreatedOn(new Date());
		evaluation.setContentSource(contentSource);
		evaluation.setDescription("description");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = evaluationManager.createEvaluation(userInfo, evaluation);
		return evaluation;
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
		
		if (evaluation!=null) {
			try {
				evaluationManager.deleteEvaluation(adminUserInfo, evaluation.getId());
				evaluation=null;
			} catch (Exception e) {}
		}
		if (evaluation2!=null) {
			try {
				evaluationManager.deleteEvaluation(adminUserInfo, evaluation2.getId());
				evaluation2=null;
			} catch (Exception e) {}
		}
		if (adminEvaluation!=null) {
			try {
				evaluationManager.deleteEvaluation(adminUserInfo, adminEvaluation.getId());
				adminEvaluation=null;
			} catch (Exception e) {}
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
		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	private static TermsOfUseAccessRequirement newEvaluationAccessRequirement(String evaluationId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(evaluationId);
		rod.setType(RestrictableObjectType.EVALUATION);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.PARTICIPATE);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	private static TermsOfUseAccessRequirement newTeamAccessRequirement(String teamId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(teamId);
		rod.setType(RestrictableObjectType.TEAM);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setEntityType(ar.getClass().getName());
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
		ar = AccessRequirementManagerImpl.newLockAccessRequirement(adminUserInfo, entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test
	public void testCreateEvaluationAccessRequirement() throws Exception {
		ar = newEvaluationAccessRequirement(evaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam1() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setSubjectIds(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam2() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setEntityType(ACTAccessRequirement.class.getName());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam3() throws Exception {
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
	public void testEvaluationCreateAccessRequirementForbidden() throws Exception {
		ar = newEvaluationAccessRequirement(adminEvaluation.getId());
		// this user will not have permission to add a restriction to the evaluation
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

		// ensure that the 'modifiedOn' date is later
		Thread.sleep(100L);
		long arModifiedOn = ar.getModifiedOn().getTime();
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>()); // change the entity id list
		AccessRequirement ar2 = accessRequirementManager.updateAccessRequirement(adminUserInfo, ar.getId().toString(), ar);
		assertTrue(ar2.getModifiedOn().getTime()-arModifiedOn>0);
		assertTrue(ar2.getSubjectIds().isEmpty());
	}
	
	@Test
	public void testGetAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetInheritedAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(childId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetUnmetEntityAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserInfo;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(otherUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetInheritedUnmetEntityAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserInfo;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(childId);
		rod.setType(RestrictableObjectType.ENTITY);
		
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(otherUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetUnmetEvaluationAccessRequirements() throws Exception {
		ar = newEvaluationAccessRequirement(adminEvaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserInfo;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(adminEvaluation.getId());
		rod.setType(RestrictableObjectType.EVALUATION);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(otherUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	// entity owner never has unmet access requirements
	@Test
	public void testGetUnmetEntityAccessRequirementsOwner() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(adminUserInfo, rod);
		assertEquals(0L, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
	}
	
	// evaluation owner gets no special treatment
	@Test
	public void testGetUnmetEvaluationAccessRequirementsOwner() throws Exception {
		ar = newEvaluationAccessRequirement(evaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(evaluation.getId());
		rod.setType(RestrictableObjectType.EVALUATION);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(testUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testDeleteAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
		ar=null;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(0L, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteEvaluationAccessRequirementForbidden() throws Exception {
		ar = newEvaluationAccessRequirement(adminEvaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		accessRequirementManager.deleteAccessRequirement(testUserInfo, ar.getId().toString());
	}
	

}
