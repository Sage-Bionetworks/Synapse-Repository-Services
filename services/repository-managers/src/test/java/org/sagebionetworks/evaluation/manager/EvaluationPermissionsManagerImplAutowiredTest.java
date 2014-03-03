package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EvaluationPermissionsManagerImplAutowiredTest {

	@Autowired
	private AccessControlListDAO aclDAO;

	@Autowired
	private EvaluationManager evaluationManager;

	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;

	private List<String> aclsToDelete;
	private List<String> evalsToDelete;
	private List<String> nodesToDelete;
	private List<String> accessRestrictionsToDelete;

	@Before
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		aclsToDelete = new ArrayList<String>();
		evalsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		accessRestrictionsToDelete = new ArrayList<String>();
	}

	@After
	public void after() throws Exception {
		for (String id : aclsToDelete) {
			aclDAO.delete(id, ObjectType.EVALUATION);
		}
		for (String id: evalsToDelete) {
			evaluationManager.deleteEvaluation(adminUserInfo, id);
		}
		for (String id : nodesToDelete) {
			nodeManager.delete(adminUserInfo, id);
		}
		for (String id : accessRestrictionsToDelete) {
			accessRequirementDAO.delete(id);
		}
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}

	@Test
	public void testAclRoundTrip() throws Exception {

		// Create ACL
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testAclRoundTrip";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		assertNotNull(acl);
		aclsToDelete.add(acl.getId());
		assertEquals(evalId, acl.getId());
		assertNotNull(acl.getEtag());
		final Date dateTime = new Date();
		assertTrue(dateTime.after(acl.getCreationDate()) || dateTime.equals(acl.getCreationDate()));

		// Has access
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT));

		// Update ACL -- Now give 'user' CHANGE_PERMISSIONS, PARTICIPATE
		ResourceAccess ra = new ResourceAccess();
		Long principalId = Long.parseLong(userInfo.getId().toString());
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessType.add(ACCESS_TYPE.PARTICIPATE);
		accessType.add(ACCESS_TYPE.SUBMIT);
		ra.setAccessType(accessType);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());

		// Get ACL
		acl = evaluationPermissionsManager.getAcl(userInfo, evalId);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());

		// Has access
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT));
		
		// now add unmet access requirements
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(evalId);
		rod.setType(RestrictableObjectType.EVALUATION);
		List<RestrictableObjectDescriptor> subjectIds = Arrays.asList(new RestrictableObjectDescriptor[]{rod});
		ar.setSubjectIds(subjectIds);
		ar.setCreatedBy(userInfo.getId().toString());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(userInfo.getId().toString());
		ar.setModifiedOn(new Date());
		ar.setEntityType(TermsOfUseAccessRequirement.class.getName());
		ar.setTermsOfUse("foo");
		ar = accessRequirementDAO.create(ar);
		accessRestrictionsToDelete.add(ar.getId().toString());
		
		// now user will lack PARTICIPATE and SUBMIT access
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT));

		// Make sure ACL is deleted when the evaluation is deleted
		evaluationManager.deleteEvaluation(adminUserInfo, evalId);
		evalsToDelete.remove(evalId);
		try {
			evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
			aclsToDelete.remove(evalId);
			fail();
		} catch (NotFoundException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testEvalOwner() throws Exception {

		// Create ACL
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testEvalOwner";
		String nodeId = createNode(nodeName, EntityType.project, userInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, userInfo);
		AccessControlList acl = evaluationPermissionsManager.getAcl(userInfo, evalId);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));

		// Update ACL
		acl = evaluationPermissionsManager.updateAcl(userInfo, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ));
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));
	}

	@Test
	public void testCreateWithExceptionsNullEvalId() throws Exception {
		// Null eval ID
		try {
			AccessControlList acl = new AccessControlList();
			evaluationPermissionsManager.createAcl(adminUserInfo, acl);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testCreateWithExceptionsNullEvalNotFound() throws Exception {
		// Eval not found
		try {
			AccessControlList acl = new AccessControlList();
			acl.setId("123");
			acl = evaluationPermissionsManager.createAcl(adminUserInfo, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (NotFoundException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testCreateWithExceptionsNotAuthorized() throws Exception {
		// Not authorized
		try {
			String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testCreateWithExceptions";
			String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
			String evalName = nodeName;
			String evalId = createEval(evalName, nodeId, adminUserInfo);
			AccessControlList acl = createAcl(evalId, userInfo);
			acl = evaluationPermissionsManager.createAcl(userInfo, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (UnauthorizedException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testUpdateWithExceptionsEvalNotFound() throws Exception {
		// Eval not found
		try {
			AccessControlList acl = new AccessControlList();
			acl.setId("123");
			acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (NotFoundException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testUpdateWithExceptions() throws Exception {

		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testUpdateWithExceptions";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);
		evaluationPermissionsManager.deleteAcl(adminUserInfo, evalId);

		// ACL does not exist yet (e-tag is null)
		try {
			AccessControlList acl = new AccessControlList();
			acl.setId(evalId);
			acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

		// Not authorized
		try {
			AccessControlList acl = createAcl(evalId, adminUserInfo);
			acl = evaluationPermissionsManager.createAcl(adminUserInfo, acl);
			aclsToDelete.add(acl.getId());
			acl = evaluationPermissionsManager.updateAcl(userInfo, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (UnauthorizedException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testGetUserPermissions() throws Exception {

		// Create ACL by 'user'
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testGetUserPermissions";
		String nodeId = createNode(nodeName, EntityType.project, userInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, userInfo);
		AccessControlList acl = evaluationPermissionsManager.getAcl(userInfo, evalId);
		assertNotNull(acl);
		aclsToDelete.add(acl.getId());
		assertEquals(evalId, acl.getId());

		// Admin
		UserEvaluationPermissions permissions =
				evaluationPermissionsManager.getUserPermissionsForEvaluation(adminUserInfo, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanParticipate());
		assertTrue(permissions.getCanView());
		assertEquals(userInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Owner
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanView());
		assertTrue(permissions.getCanParticipate());
		assertEquals(userInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Create ACL by 'adminUserInfo'
		nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testGetUserPermissions -- admin";
		nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		evalName = nodeName;
		evalId = createEval(evalName, nodeId, adminUserInfo);
		acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		assertNotNull(acl);
		aclsToDelete.add(acl.getId());
		assertEquals(evalId, acl.getId());

		// Admin
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(adminUserInfo, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanParticipate());
		assertTrue(permissions.getCanView());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Not admin, not owner
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertFalse(permissions.getCanParticipate());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Update the ACL to add 'user', PARTICIPATE
		Long principalId = Long.parseLong(userInfo.getId().toString());
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.PARTICIPATE);
		ra.setAccessType(accessType);
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());

		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		assertTrue(permissions.getCanParticipate());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Set 'public read' for anonymous user
		UserInfo anonymous = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		principalId = anonymous.getId();
		raSet = new HashSet<ResourceAccess>();
		ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.READ);
		ra.setAccessType(accessType);
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());

		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		assertFalse(permissions.getCanParticipate());
		assertTrue(permissions.getCanPublicRead());
	}

	@Test
	public void testCanParticipate() throws Exception {

		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testCanParticipate";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		assertNotNull(acl);

		// Admin can participate but user cannot
		assertTrue(evaluationPermissionsManager.hasAccess(adminUserInfo, evalId, ACCESS_TYPE.PARTICIPATE));
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));

		// Update the ACL to add ('user', PARTICIPATE)
		Long principalId = userInfo.getId();
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.PARTICIPATE);
		ra.setAccessType(accessType);
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		assertNotNull(acl);
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));

		// Now if we have unmet requirements, adding just the ACL is not enough
		List<ACCESS_TYPE> participateAndDownload = new ArrayList<ACCESS_TYPE>();
		participateAndDownload.add(ACCESS_TYPE.DOWNLOAD);
		participateAndDownload.add(ACCESS_TYPE.PARTICIPATE);
		AccessRequirementDAO mockAccessRequirementDao = mock(AccessRequirementDAO.class);
		when(mockAccessRequirementDao.unmetAccessRequirements(
				any(List.class), any(RestrictableObjectType.class), any(Collection.class), eq(participateAndDownload))).
				thenReturn(Arrays.asList(new Long[]{101L}));
		AccessRequirementDAO original = (AccessRequirementDAO) ReflectionTestUtils.getField(evaluationPermissionsManager, "accessRequirementDAO");
		ReflectionTestUtils.setField(evaluationPermissionsManager, "accessRequirementDAO", mockAccessRequirementDao);
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.PARTICIPATE));
		ReflectionTestUtils.setField(evaluationPermissionsManager, "accessRequirementDAO", original);
	}

	private String createNode(String name, EntityType type, UserInfo userInfo) throws Exception {
		final long principalId = Long.parseLong(userInfo.getId().toString());
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(principalId);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(principalId);
		node.setNodeType(type.name());
		String id = nodeManager.createNewNode(node, userInfo);
		nodesToDelete.add(id);
		return id;
	}

	private String createEval(String name, String contentSource, UserInfo userInfo) throws Exception {
		Evaluation eval = new Evaluation();
		eval.setCreatedOn(new Date());
		eval.setName(name);
		eval.setOwnerId(userInfo.getId().toString());
        eval.setContentSource(contentSource);
        eval.setStatus(EvaluationStatus.PLANNED);
        eval.setEtag(UUID.randomUUID().toString());
        eval = evaluationManager.createEvaluation(userInfo, eval);
		evalsToDelete.add(eval.getId());
		return eval.getId();
	}

	private AccessControlList createAcl(String evalId, UserInfo userInfo) throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId(evalId);
		final String principalId = userInfo.getId().toString();
		acl.setCreatedBy(principalId);
		final Date now = new Date();
		acl.setCreationDate(now);
		acl.setModifiedBy(principalId);
		acl.setModifiedOn(now);
		return acl;
	}
}
