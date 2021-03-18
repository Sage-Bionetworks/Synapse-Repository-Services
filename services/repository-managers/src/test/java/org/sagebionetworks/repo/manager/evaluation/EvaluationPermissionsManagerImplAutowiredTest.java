package org.sagebionetworks.repo.manager.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
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

	private UserInfo adminUserInfo;
	private UserInfo userInfo;

	private List<String> aclsToDelete;
	private List<String> evalsToDelete;
	private List<String> nodesToDelete;

	@BeforeEach
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		userInfo.setAcceptsTermsOfUse(true);
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		aclsToDelete = new ArrayList<String>();
		evalsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
	}

	@AfterEach
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
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());

		// Update ACL -- Now give 'user' CHANGE_PERMISSIONS, SUBMIT
		ResourceAccess ra = new ResourceAccess();
		Long principalId = Long.parseLong(userInfo.getId().toString());
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessType.add(ACCESS_TYPE.SUBMIT);
		ra.setAccessType(accessType);
		Set<ResourceAccess> raSet = Collections.singleton(ra);
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
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());

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
	public void testHasAccessWithMultipleEvaluations() throws Exception {
		String projectId = createNode(UUID.randomUUID().toString(), EntityType.project, adminUserInfo);
		
		String evaluation1 = createEval(UUID.randomUUID().toString(), projectId, adminUserInfo);
		String evaluation2 = createEval(UUID.randomUUID().toString(), projectId, adminUserInfo);
	
		List<String> evaluationIds = ImmutableList.of(evaluation1, evaluation2);
		
		ACCESS_TYPE accessType = ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
		
		// Call under test
		AuthorizationStatus status = evaluationPermissionsManager.hasAccess(userInfo, accessType, evaluationIds);
	
		assertFalse(status.isAuthorized());
		assertEquals("User lacks "+accessType+" access to all the evaluations in the set.", status.getMessage());
		
		AccessControlList acl = evaluationPermissionsManager.getAcl(userInfo, evaluation1);
		
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(ImmutableSet.of(accessType));
		ra.setPrincipalId(userInfo.getId());
		
		acl.getResourceAccess().add(ra);
		
		evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		
		// Call under test
		status = evaluationPermissionsManager.hasAccess(userInfo, accessType, evaluationIds);
		
		// Still no access as evaluation2 does not have permissions
		assertFalse(status.isAuthorized());
		assertEquals("User lacks "+accessType+" access to all the evaluations in the set.", status.getMessage());
		
		// Add access to evaluation 2 as well
		acl = evaluationPermissionsManager.getAcl(userInfo, evaluation2);
		acl.getResourceAccess().add(ra);
		
		evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		
		// Call under test
		status = evaluationPermissionsManager.hasAccess(userInfo, accessType, evaluationIds);
		
		// Gained access
		assertTrue(status.isAuthorized());
		
		// Should still work with duplicates
		evaluationIds = ImmutableList.of(evaluation1, evaluation1, evaluation2);
		
		// Call under test
		status = evaluationPermissionsManager.hasAccess(userInfo, accessType, evaluationIds);
		
		assertTrue(status.isAuthorized());
		
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
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE_SUBMISSION).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE_SUBMISSION).isAuthorized());

		// Update ACL
		acl = evaluationPermissionsManager.updateAcl(userInfo, acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.CHANGE_PERMISSIONS).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE_SUBMISSION).isAuthorized());
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.DELETE_SUBMISSION).isAuthorized());
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
		assertTrue(permissions.getCanView());
		assertTrue(permissions.getCanDeleteSubmissions());
		assertTrue(permissions.getCanEditSubmissionStatuses());
		assertTrue(permissions.getCanSubmit());
		assertTrue(permissions.getCanViewPrivateSubmissionStatusAnnotations());
		assertEquals(userInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Owner
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
		assertTrue(permissions.getCanChangePermissions());
		assertTrue(permissions.getCanDelete());
		assertTrue(permissions.getCanEdit());
		assertTrue(permissions.getCanView());
		assertTrue(permissions.getCanDeleteSubmissions());
		assertTrue(permissions.getCanEditSubmissionStatuses());
		assertTrue(permissions.getCanSubmit());
		assertTrue(permissions.getCanViewPrivateSubmissionStatusAnnotations());
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
		assertTrue(permissions.getCanView());
		assertTrue(permissions.getCanDeleteSubmissions());
		assertTrue(permissions.getCanEditSubmissionStatuses());
		assertTrue(permissions.getCanSubmit());
		assertTrue(permissions.getCanViewPrivateSubmissionStatusAnnotations());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Not admin, not owner
		permissions = evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
		assertFalse(permissions.getCanChangePermissions());
		assertFalse(permissions.getCanDelete());
		assertFalse(permissions.getCanEdit());
		assertFalse(permissions.getCanView());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Update the ACL to add 'user', PARTICIPATE
		Long principalId = Long.parseLong(userInfo.getId().toString());
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.SUBMIT);
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
		// Unless we explicitly set for the anonymous user
		assertFalse(permissions.getCanPublicRead());

		// Set 'public read' for anyone
		principalId = BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId();
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
		assertTrue(permissions.getCanView());
		assertEquals(adminUserInfo.getId().toString(), permissions.getOwnerPrincipalId().toString());
		assertTrue(permissions.getCanPublicRead());
	}

	@Test
	public void testCanSubmit() throws Exception {

		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testCanSubmit";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		assertNotNull(acl);

		// Admin can SUBMIT but user cannot
		assertTrue(evaluationPermissionsManager.hasAccess(adminUserInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());

		// Update the ACL to add ('user', SUBMIT)
		Long principalId = userInfo.getId();
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.SUBMIT);
		ra.setAccessType(accessType);
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
		assertNotNull(acl);
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());
	}
	
	public static RestrictableObjectDescriptor createRestrictableObjectDescriptor(String id, RestrictableObjectType type) {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(id);
		rod.setType(type);
		return rod;
	}

	@Test
	public void testCanCheckSubmissionEligibility() throws Exception {
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.testCanCheckSubmissionEligibility";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);

		// userInfo includes this team
		String teamId = BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString();
		
		// false if user is on team but lacks submit privilege
		assertFalse(evaluationPermissionsManager.
				canCheckTeamSubmissionEligibility(userInfo, evalId, teamId).isAuthorized());
		
		// add SUBMIT privilege to ACL
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		Long principalId = userInfo.getId();
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.SUBMIT);
		ra.setAccessType(accessType);
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = evaluationPermissionsManager.updateAcl(adminUserInfo, acl);

		// true if user is on team and has submit privilege
		assertTrue(evaluationPermissionsManager.
				canCheckTeamSubmissionEligibility(userInfo, evalId, teamId).isAuthorized());
		
		// false if user is NOT on team and has submit privilege
		assertFalse(evaluationPermissionsManager.
				canCheckTeamSubmissionEligibility(userInfo, evalId, "999").isAuthorized());
		
		// true for admin
		assertTrue(evaluationPermissionsManager.
				canCheckTeamSubmissionEligibility(adminUserInfo, evalId, "999").isAuthorized());
	}

	@Test
	public void publicUserGrantAndRevokeRead() throws Exception {
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.publicUserGrantAndRevokeRead";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);

		// Ensure the user does not have access before granting read and submit
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());

		// add READ, SUBMIT privilege to ACL for public
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		ra.setAccessType(Collections.singleton(ACCESS_TYPE.READ));
		Set<ResourceAccess> raSet = Collections.singleton(ra);
		acl.setResourceAccess(raSet);
		evaluationPermissionsManager.updateAcl(adminUserInfo, acl);

		// Call under test (after granting)
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());

		// Revoke access
		acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		raSet = new HashSet<>();
		acl.setResourceAccess(raSet);
		evaluationPermissionsManager.updateAcl(adminUserInfo, acl);

		// Call under test (after revoking)
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
	}

	@Test
	public void publicUserCannotBeGrantedNonRead() throws Exception {
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.anonymousUserCanBeGrantedRead";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);

		// add READ privilege to ACL for public users
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.READ)) {
				AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
				ResourceAccess ra = new ResourceAccess();
				ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
				ra.setAccessType(Collections.singleton(type));
				Set<ResourceAccess> raSet = Collections.singleton(ra);
				acl.setResourceAccess(raSet);
				try {
					// Call under test
					evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
					fail("Expected InvalidModelException");
				} catch (InvalidModelException e) {
					// as expected
				}
			}
		}
	}

	@Test
	public void authenticatedUserGrantAndRevokeRead() throws Exception {
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.anonymousUserCanBeGrantedRead";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);

		// Ensure the user does not have access before granting read and submit
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).isAuthorized());

		// add READ, SUBMIT privilege to ACL for authenticated user
		AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		ra.setAccessType(Collections.singleton(ACCESS_TYPE.READ));
		Set<ResourceAccess> raSet = Collections.singleton(ra);
		acl.setResourceAccess(raSet);
		evaluationPermissionsManager.updateAcl(adminUserInfo, acl);

		// Call under test (after granting)
		assertTrue(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());

		// Revoke access
		acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
		raSet = new HashSet<>();
		acl.setResourceAccess(raSet);
		evaluationPermissionsManager.updateAcl(adminUserInfo, acl);

		// Call under test (after revoking)
		assertFalse(evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ).isAuthorized());
	}

	@Test
	public void authenticatedUserCannotBeGrantedNonRead() throws Exception {
		String nodeName = "EvaluationPermissionsManagerImplAutowiredTest.anonymousUserCanBeGrantedRead";
		String nodeId = createNode(nodeName, EntityType.project, adminUserInfo);
		String evalName = nodeName;
		String evalId = createEval(evalName, nodeId, adminUserInfo);

		// add READ privilege to ACL for authenticated users
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.READ)) {
				AccessControlList acl = evaluationPermissionsManager.getAcl(adminUserInfo, evalId);
				ResourceAccess ra = new ResourceAccess();
				ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
				ra.setAccessType(Collections.singleton(type));
				Set<ResourceAccess> raSet = Collections.singleton(ra);
				acl.setResourceAccess(raSet);
				try {
					// Call under test
					evaluationPermissionsManager.updateAcl(adminUserInfo, acl);
					fail("Expected InvalidModelException");
				} catch (InvalidModelException e) {
					// as expected
				}
			}
		}
	}
	
	private String createNode(String name, EntityType type, UserInfo userInfo) throws Exception {
		final long principalId = Long.parseLong(userInfo.getId().toString());
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(principalId);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(principalId);
		node.setNodeType(type);
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
