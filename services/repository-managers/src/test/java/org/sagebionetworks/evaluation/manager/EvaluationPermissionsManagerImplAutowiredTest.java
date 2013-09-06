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
import java.util.Iterator;
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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
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
	private UserProvider testUserProvider;

	private UserInfo adminUser;
	private UserInfo user;

	private List<String> aclsToDelete;
	private List<String> evalsToDelete;
	private List<String> nodesToDelete;

	@Before
	public void before() {

		assertNotNull(aclDAO);
		assertNotNull(evaluationPermissionsManager);
		assertNotNull(nodeManager);
		assertNotNull(testUserProvider);

		adminUser = testUserProvider.getTestAdminUserInfo();
		assertNotNull(adminUser);
		user = testUserProvider.getTestUserInfo();
		assertNotNull(user);

		aclsToDelete = new ArrayList<String>();
		evalsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
	}

	@After
	public void after() throws Exception {
		for (String id : aclsToDelete) {
			aclDAO.delete(id);
		}
		for (String id: evalsToDelete) {
			evaluationManager.deleteEvaluation(adminUser, id);
		}
		for (String id : nodesToDelete) {
			nodeManager.delete(adminUser, id);
		}
	}

	@Test
	public void testAclRoundTrip() throws Exception {

		// Create ACL
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testAclRoundTrip";
		String nodeId = createNode(nodeName, EntityType.project, adminUser);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUser);
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUser, evalId);
		assertNotNull(acl);
		aclsToDelete.add(acl.getId());
		assertEquals(evalId, acl.getId());
		assertNotNull(acl.getEtag());
		final Date dateTime = new Date();
		assertTrue(dateTime.after(acl.getCreationDate()) || dateTime.equals(acl.getCreationDate()));

		// Has access
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.UPDATE));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.DELETE));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.READ));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));

		// Update ACL -- Now give 'user' CHANGE_PERMISSIONS, PARTICIPATE
		ResourceAccess ra = new ResourceAccess();
		Long principalId = Long.parseLong(user.getIndividualGroup().getId());
		ra.setPrincipalId(principalId);
		Iterator<UserGroup> iterator = user.getGroups().iterator();
		String groupName = iterator.next().getName();
		ra.setGroupName(groupName);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessType.add(ACCESS_TYPE.PARTICIPATE);
		ra.setAccessType(accessType);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUser, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());

		// Get ACL
		acl = evaluationPermissionsManager.getAcl(user, evalId);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());

		// Has access
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.UPDATE));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.DELETE));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.READ));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));

		// Make sure ACL is deleted when the evaluation is deleted
		evaluationManager.deleteEvaluation(adminUser, evalId);
		evalsToDelete.remove(evalId);
		try {
			evaluationPermissionsManager.getAcl(adminUser, evalId);
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
		String nodeId = createNode(nodeName, EntityType.project, user);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, user);
		AccessControlList acl = evaluationPermissionsManager.getAcl(user, evalId);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.UPDATE));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.DELETE));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.READ));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));

		// Update ACL
		acl = evaluationPermissionsManager.updateAcl(user, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.UPDATE));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.DELETE));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.READ));
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));
	}

	@Test
	public void testCreateWithExceptionsNullEvalId() throws Exception {
		// Null eval ID
		try {
			AccessControlList acl = new AccessControlList();
			evaluationPermissionsManager.createAcl(adminUser, acl);
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
			acl = evaluationPermissionsManager.createAcl(adminUser, acl);
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
			String nodeId = createNode(nodeName, EntityType.project, adminUser);
			String evalName = nodeName;
			String evalId = createEval(evalName, nodeId, adminUser);
			AccessControlList acl = createAcl(evalId, user);
			acl = evaluationPermissionsManager.createAcl(user, acl);
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
			acl = evaluationPermissionsManager.updateAcl(adminUser, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (NotFoundException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testUpdateWithExceptions() throws Exception {

		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testUpdateWithExceptions";
		String nodeId = createNode(nodeName, EntityType.project, adminUser);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUser);
		evaluationPermissionsManager.deleteAcl(adminUser, evalId);

		// ACL does not exist yet (e-tag is null)
		try {
			AccessControlList acl = new AccessControlList();
			acl.setId(evalId);
			acl = evaluationPermissionsManager.updateAcl(adminUser, acl);
			aclsToDelete.add(acl.getId());
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

		// Not authorized
		try {
			AccessControlList acl = createAcl(evalId, adminUser);
			acl = evaluationPermissionsManager.createAcl(adminUser, acl);
			aclsToDelete.add(acl.getId());
			acl = evaluationPermissionsManager.updateAcl(user, acl);
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
		String nodeId = createNode(nodeName, EntityType.project, user);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, user);
		AccessControlList acl = evaluationPermissionsManager.getAcl(user, evalId);
		assertNotNull(acl);
		aclsToDelete.add(acl.getId());
		assertEquals(evalId, acl.getId());

		// Admin
		UserEvaluationPermissions permissions =
				evaluationPermissionsManager.getUserPermissionsForEvaluation(adminUser, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanParticipate());
		assertTrue(permissions.getCanView());
		assertEquals(user.getIndividualGroup().getId(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Owner
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(user, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanView());
		assertTrue(permissions.getCanParticipate());
		assertEquals(user.getIndividualGroup().getId(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Create ACL by 'adminUser'
		nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testGetUserPermissions -- admin";
		nodeId = createNode(nodeName, EntityType.project, adminUser);
		evalName = nodeName;
		evalId = createEval(evalName, nodeId, adminUser);
		acl = evaluationPermissionsManager.getAcl(adminUser, evalId);
		assertNotNull(acl);
		aclsToDelete.add(acl.getId());
		assertEquals(evalId, acl.getId());

		// Admin
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(adminUser, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanParticipate());
		assertTrue(permissions.getCanView());
		assertEquals(adminUser.getIndividualGroup().getId(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Not admin, not owner
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(user, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertFalse(permissions.getCanParticipate());
		assertEquals(adminUser.getIndividualGroup().getId(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Update the ACL to add 'user', PARTICIPATE
		Long principalId = Long.parseLong(user.getIndividualGroup().getId());
		Iterator<UserGroup> iterator = user.getGroups().iterator();
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		while (iterator.hasNext()) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(principalId);
			String groupName = iterator.next().getName();
			ra.setGroupName(groupName);
			Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
			accessType.add(ACCESS_TYPE.PARTICIPATE);
			ra.setAccessType(accessType);
			raSet.add(ra);
		}
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUser, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());

		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(user, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertEquals(adminUser.getIndividualGroup().getId(), permissions.getOwnerPrincipalId().toString());
		assertTrue(permissions.getCanParticipate());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Set 'public read' for anonymous user
		UserInfo anonymous = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		principalId = Long.parseLong(anonymous.getIndividualGroup().getId());
		iterator = anonymous.getGroups().iterator();
		raSet = new HashSet<ResourceAccess>();
		while (iterator.hasNext()) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(principalId);
			String groupName = iterator.next().getName();
			ra.setGroupName(groupName);
			Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
			accessType.add(ACCESS_TYPE.READ);
			ra.setAccessType(accessType);
			raSet.add(ra);
		}
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUser, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());

		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(user, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertEquals(adminUser.getIndividualGroup().getId(), permissions.getOwnerPrincipalId().toString());
		assertFalse(permissions.getCanParticipate());
		assertTrue(permissions.getCanPublicRead());
	}

	@Test
	public void testCanParticipate() throws Exception {

		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testCanParticipate";
		String nodeId = createNode(nodeName, EntityType.project, adminUser);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUser);
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUser, evalId);
		assertNotNull(acl);

		// Admin can participate but user cannot
		assertTrue(evaluationPermissionsManager.hasAccess(adminUser, evalId, ACCESS_TYPE.PARTICIPATE));
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));

		// Update the ACL to add ('user', PARTICIPATE)
		Long principalId = Long.parseLong(user.getIndividualGroup().getId());
		Iterator<UserGroup> iterator = user.getGroups().iterator();
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		while (iterator.hasNext()) {
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(principalId);
			String groupName = iterator.next().getName();
			ra.setGroupName(groupName);
			Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
			accessType.add(ACCESS_TYPE.PARTICIPATE);
			ra.setAccessType(accessType);
			raSet.add(ra);
		}
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUser, acl);
		assertNotNull(acl);
		assertTrue(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));

		// Now if we have unmet requirements, adding just the ACL is not enough
		List<ACCESS_TYPE> participateAndDownload = new ArrayList<ACCESS_TYPE>();
		participateAndDownload.add(ACCESS_TYPE.DOWNLOAD);
		participateAndDownload.add(ACCESS_TYPE.PARTICIPATE);
		AccessRequirementDAO mockAccessRequirementDao = mock(AccessRequirementDAO.class);
		when(mockAccessRequirementDao.unmetAccessRequirements(
				any(RestrictableObjectDescriptor.class), any(Collection.class), eq(participateAndDownload))).
				thenReturn(Arrays.asList(new Long[]{101L}));
		AccessRequirementDAO original = (AccessRequirementDAO) ReflectionTestUtils.getField(evaluationPermissionsManager, "accessRequirementDAO");
		ReflectionTestUtils.setField(evaluationPermissionsManager, "accessRequirementDAO", mockAccessRequirementDao);
		assertFalse(evaluationPermissionsManager.hasAccess(user, evalId, ACCESS_TYPE.PARTICIPATE));
		ReflectionTestUtils.setField(evaluationPermissionsManager, "accessRequirementDAO", original);
	}

	private String createNode(String name, EntityType type, UserInfo userInfo) throws Exception {
		final long principalId = Long.parseLong(userInfo.getIndividualGroup().getId());
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
		eval.setOwnerId(userInfo.getIndividualGroup().getId());
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
		final String principalId = userInfo.getIndividualGroup().getId();
		acl.setCreatedBy(principalId);
		final Date now = new Date();
		acl.setCreationDate(now);
		acl.setModifiedBy(principalId);
		acl.setModifiedOn(now);
		return acl;
	}

	// PUBLIC: READ
	// AUTHENTICATED)USERS: READ, PARTICIPATE
	private void validatePublicReadParticipate(AccessControlList acl) {
		Set<ResourceAccess> raSet = acl.getResourceAccess();
		assertNotNull(raSet);
		String publicUserId = userManager.getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC).getId();
		String authenticatedUserId = userManager.getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS).getId();
		boolean hasPublicUser = false;
		boolean hasAuthenticatedUser = false;
		for (ResourceAccess ra: raSet) {
			String principalId = ra.getPrincipalId().toString();
			if (principalId.equals(publicUserId)) {
				hasPublicUser = true;
				assertTrue(ra.getAccessType().contains(ACCESS_TYPE.READ));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.CREATE));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.DELETE));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.PARTICIPATE));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.UPDATE));
			} else if (principalId.equals(authenticatedUserId)) {
				hasAuthenticatedUser = true;
				assertTrue(ra.getAccessType().contains(ACCESS_TYPE.READ));
				assertTrue(ra.getAccessType().contains(ACCESS_TYPE.PARTICIPATE));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.CREATE));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.DELETE));
				assertFalse(ra.getAccessType().contains(ACCESS_TYPE.UPDATE));
			}
		}
		assertTrue(hasPublicUser);
		assertTrue(hasAuthenticatedUser);
	}
}
