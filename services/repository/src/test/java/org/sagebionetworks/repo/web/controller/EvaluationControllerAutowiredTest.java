package org.sagebionetworks.repo.web.controller;


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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EvaluationControllerAutowiredTest extends AbstractAutowiredControllerJunit5TestBase {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private EvaluationPermissionsManager evalPermissionsManager;
	
	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private SubmissionDAO submissionDAO;
	
	@Autowired
	private SubmissionStatusDAO submissionStatusDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	@Autowired
	private TeamManager teamManager;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
		
	private String adminAccessToken;
	private String testUserAccessToken;
	private Long adminUserId;
	private UserInfo adminUserInfo;
	
	private Long testUserId;
	private UserInfo testUserInfo;

	private String submittingTeamId;

	private String parentProjectId;
	private Evaluation eval1;
	private Evaluation eval2;
	private Submission sub1;
	private Submission sub2;
	
	private List<String> evaluationsToDelete;
	private List<String> submissionsToDelete;
	private List<String> nodesToDelete;
	
	@BeforeEach
	public void before() throws Exception {
		evaluationsToDelete = new ArrayList<String>();
		submissionsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		
		// get user IDs
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		adminAccessToken = oidcTokenHelper.createTotalAccessToken(adminUserId);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserId = userManager.createUser(user);
		testUserAccessToken = oidcTokenHelper.createTotalAccessToken(testUserId);
		 groupMembersDAO.addMembers(
		BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
		 Collections.singletonList(testUserId.toString()));
		testUserInfo = userManager.getUserInfo(testUserId);

		Team team = new Team();
		team.setName(UUID.randomUUID().toString() + " evaluation test team");
		submittingTeamId = teamManager.create(adminUserInfo, team).getId();
		teamManager.addMember(adminUserInfo, submittingTeamId, testUserInfo);

		groupMembersDAO.addMembers(submittingTeamId, Collections.singletonList(testUserId.toString()));

		// Initialize project to store evaluations
		parentProjectId = createNode("Some project name", testUserInfo);

		// initialize Evaluations
		eval1 = new Evaluation();
		eval1.setName("name");
		eval1.setDescription("description");
        eval1.setContentSource(parentProjectId);
        eval1.setStatus(EvaluationStatus.PLANNED);
        eval2 = new Evaluation();
		eval2.setName("name2");
		eval2.setDescription("description");
        eval2.setContentSource(parentProjectId);
        eval2.setStatus(EvaluationStatus.PLANNED);
        
        // initialize Submissions
        sub1 = new Submission();
        sub1.setName("submission1");
        sub1.setVersionNumber(1L);
        sub1.setSubmitterAlias("Team Awesome!");
        sub2 = new Submission();
        sub2.setName("submission2");
        sub2.setVersionNumber(1L);
        sub2.setSubmitterAlias("Team Even Better!");
	}
	
	@AfterEach
	public void after() throws Exception {
		// clean up submissions
		for (String id : submissionsToDelete) {
			try {
				submissionStatusDAO.delete(id);
			} catch (Exception e) {}
			try {
				submissionDAO.delete(id);
			} catch (Exception e) {}
		}
		// clean up evaluations
		for (String id : evaluationsToDelete) {
			try {
				evalPermissionsManager.deleteAcl(adminUserInfo, id);
				evaluationDAO.delete(id);
			} catch (Exception e) {}
		}
		// Delete children before parents
		Collections.reverse(nodesToDelete);
		// clean up nodes
		for (String id : nodesToDelete) {
			try {
				nodeManager.delete(adminUserInfo, id);
			} catch (Exception e) {}
		}

		teamManager.delete(adminUserInfo, submittingTeamId);
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(testUserInfo.getId().toString()));
	}
	
	@Test
	public void testEvaluationRoundTrip() throws Exception {
		long initialCount = entityServletHelper.getAvailableEvaluations(adminUserId).getTotalNumberOfResults();
		
		// Create
		eval1 = entityServletHelper.createEvaluation(eval1, adminUserId);		
		assertNotNull(eval1.getEtag());
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		
		//can read
		Boolean canRead = entityServletHelper.canAccess(adminUserId, eval1.getId(), ACCESS_TYPE.READ);
		assertTrue(canRead);
		//test user cannot read
		canRead = entityServletHelper.canAccess(testUserId, eval1.getId(), ACCESS_TYPE.READ);
		assertFalse(canRead);
		
		// Read
		Evaluation fetched = entityServletHelper.getEvaluation(adminUserId, eval1.getId());
		assertEquals(eval1, fetched);
		try {
			fetched = entityServletHelper.getEvaluation(testUserId, eval1.getId());
			fail();
		} catch (UnauthorizedException e) {
			// Expected
		}

		// Find
		fetched = entityServletHelper.findEvaluation(adminUserId, eval1.getName());
		assertEquals(eval1, fetched);
		try {
			fetched = entityServletHelper.findEvaluation(testUserId, eval1.getName());
			assertEquals(eval1, fetched);
			fail();
		} catch (NotFoundException e) {
			// Expected
		}
		
		//can update
		Boolean canUpdate = entityServletHelper.canAccess(adminUserId, eval1.getId(), ACCESS_TYPE.UPDATE);
		assertTrue(canUpdate);
		//test user can't update
		canUpdate = entityServletHelper.canAccess(testUserId, eval1.getId(), ACCESS_TYPE.UPDATE);
		assertFalse(canUpdate);
		
		// Update
		fetched.setDescription(eval1.getDescription() + " (modified)");
		Evaluation updated = entityServletHelper.updateEvaluation(fetched, adminUserId);
		assertFalse(updated.getEtag().equals(fetched.getEtag()), "eTag was not updated");
		fetched.setEtag(updated.getEtag());
		assertEquals(fetched, updated);
		assertEquals(initialCount + 1, entityServletHelper.getAvailableEvaluations(adminUserId).getTotalNumberOfResults());
		
		// Delete
		entityServletHelper.deleteEvaluation(eval1.getId(), adminUserId);
		try {
			entityServletHelper.getEvaluation(adminUserId, eval1.getId());
			fail("Delete failed");
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getAvailableEvaluations(adminUserId).getTotalNumberOfResults());
	}
	
	@Test
	public void testSubmissionRoundTrip() throws Exception {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, adminUserId);
		evaluationsToDelete.add(eval1.getId());
		
		// open the evaluation to join
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.SUBMIT);
		accessSet.add(ACCESS_TYPE.READ);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(Long.valueOf(submittingTeamId));
		AccessControlList acl = entityServletHelper.getEvaluationAcl(adminUserId, eval1.getId());
		acl.getResourceAccess().add(ra);
		acl = entityServletHelper.updateEvaluationAcl(adminUserId, acl);
		assertNotNull(acl);
		
		UserInfo userInfo = userManager.getUserInfo(testUserId);
		String nodeId = createNode("An entity", userInfo);
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);
		
		long initialCount = entityServletHelper.getSubmissionCount(adminUserId, eval1.getId());
		
		// create
		Node node = nodeManager.get(userInfo, nodeId);
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(nodeId);
		sub1 = entityServletHelper.createSubmission(sub1, testUserAccessToken, node.getETag());
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		assertEquals(initialCount + 1, entityServletHelper.getSubmissionCount(adminUserId, eval1.getId()));
		
		// read
		Submission clone = entityServletHelper.getSubmission(adminUserId, sub1.getId());
		assertEquals(sub1, clone);
		SubmissionStatus status = entityServletHelper.getSubmissionStatus(adminUserId, sub1.getId());
		assertNotNull(status);
		assertEquals(sub1.getId(), status.getId());
		assertEquals(SubmissionStatusEnum.RECEIVED, status.getStatus());
		
		// update
		Thread.sleep(1L);
		status.setScore(0.5);
		status.setStatus(SubmissionStatusEnum.SCORED);
		DoubleAnnotation da = new DoubleAnnotation();
		// make sure NaNs can make the round trip
		da.setKey("foo");
		da.setValue(Double.NaN);
		da.setIsPrivate(true);
		Annotations annots = new Annotations();
		annots.setDoubleAnnos(Collections.singletonList(da));
		annots.setObjectId(status.getId());
		annots.setScopeId(eval1.getId());
		status.setAnnotations(annots);
		status.setCanCancel(true);
		SubmissionStatus statusClone = entityServletHelper.updateSubmissionStatus(status, adminUserId);
		assertFalse(status.getModifiedOn().equals(statusClone.getModifiedOn()), "Modified date was not updated");
		status.setModifiedOn(statusClone.getModifiedOn());
		assertFalse(status.getEtag().equals(statusClone.getEtag()), "Etag was not updated");
		status.setEtag(statusClone.getEtag());
		status.setStatusVersion(statusClone.getStatusVersion());
		assertEquals(status, statusClone);
		// make sure NaNs can make the round trip
		assertTrue(Double.isNaN(statusClone.getAnnotations().getDoubleAnnos().get(0).getValue()));
		assertEquals(initialCount + 1, entityServletHelper.getSubmissionCount(adminUserId, eval1.getId()));
		
		entityServletHelper.cancelSubmission(testUserId, sub1.getId());
		status = entityServletHelper.getSubmissionStatus(adminUserId, sub1.getId());
		assertTrue(status.getCancelRequested());
		
		// delete
		entityServletHelper.deleteSubmission(sub1.getId(), adminUserId);
		try {
			entityServletHelper.deleteSubmission(sub1.getId(), adminUserId);
			fail("Failed to delete Submission " + sub1.toString());
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getSubmissionCount(adminUserId, eval1.getId()));
	}

	@Test
	public void testSubmissionUnauthorized() throws Exception {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, adminUserId);
		evaluationsToDelete.add(eval1.getId());
		UserInfo ownerInfo = userManager.getUserInfo(adminUserId);
		String nodeId = createNode("An entity", ownerInfo);
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);
		Node node = nodeManager.get(ownerInfo, nodeId);
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(nodeId);
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			entityServletHelper.createSubmission(sub1, testUserAccessToken, node.getETag());
		});
	}
	
	@Test
	public void testPaginated() throws Exception {
		// create objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, adminUserId);
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		eval2 = entityServletHelper.createEvaluation(eval2, adminUserId);
		assertNotNull(eval2.getId());
		evaluationsToDelete.add(eval2.getId());
		
		// open the evaluation to join
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.READ);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(Long.valueOf(submittingTeamId));
		AccessControlList acl = entityServletHelper.getEvaluationAcl(adminUserId, eval1.getId());
		acl.getResourceAccess().add(ra);
		acl = entityServletHelper.updateEvaluationAcl(adminUserId, acl);
		assertNotNull(acl);
		
		UserInfo userInfo = userManager.getUserInfo(testUserId);
		String node1 = createNode("entity1", userInfo);
		assertNotNull(node1);
		String etag1 = nodeManager.get(userInfo, node1).getETag();
		nodesToDelete.add(node1);
		String node2 = createNode("entity2", userInfo);
		String etag2 = nodeManager.get(userInfo, node2).getETag();
		assertNotNull(node2);
		nodesToDelete.add(node2);
		
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(node1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(adminUserId.toString());
		sub1 = entityServletHelper.createSubmission(sub1, adminAccessToken, etag1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(node2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(adminUserId.toString());
		sub2 = entityServletHelper.createSubmission(sub2, adminAccessToken, etag2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
		
		// paginated evaluations
		PaginatedResults<Evaluation> evals = entityServletHelper.getEvaluationsPaginated(adminUserId, 10, 0);
		assertEquals(2, evals.getTotalNumberOfResults());
		for (Evaluation c : evals.getResults()) {
			assertTrue(c.equals(eval1) || c.equals(eval2), "Unknown Evaluation returned: " + c.toString());
		}
		
		// paginated evaluations by content source
		evals = entityServletHelper.getEvaluationsByContentSourcePaginated(adminUserId, parentProjectId, 10, 0);
		assertEquals(2, evals.getTotalNumberOfResults());
		for (Evaluation c : evals.getResults()) {
			assertTrue(c.equals(eval1) || c.equals(eval2), "Unknown Evaluation returned: " + c.toString());
		}
		
		// paginated submissions
		PaginatedResults<Submission> subs = entityServletHelper.getAllSubmissions(adminUserId, eval1.getId(), null);
		assertEquals(2, subs.getTotalNumberOfResults());
		for (Submission s : subs.getResults()) {
			assertTrue(s.equals(sub1) || s.equals(sub2), "Unknown Submission returned: " + s.toString());
		}
		
		subs = entityServletHelper.getAllSubmissions(adminUserId, eval1.getId(), SubmissionStatusEnum.SCORED);
		assertEquals(0, subs.getTotalNumberOfResults());
		
		subs = entityServletHelper.getAllSubmissions(adminUserId, eval2.getId(), null);
		assertEquals(0, subs.getTotalNumberOfResults());
	}

	@Test
	public void testAclRoundTrip() throws Exception {

		// Create the entity first
		UserInfo userInfo = userManager.getUserInfo(testUserId);
		String nodeId = createNode("EvaluationControllerAutowiredTest.testAclRoundTrip()", userInfo);
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);

		// Create the evaluation (which should also creates the ACL)
		eval1.setContentSource(nodeId);
		eval1 = entityServletHelper.createEvaluation(eval1, testUserId);		
		assertNotNull(eval1.getEtag());
		assertNotNull(eval1.getId());
		assertEquals(nodeId, eval1.getContentSource());
		evaluationsToDelete.add(eval1.getId());

		// Get the ACL
		AccessControlList aclReturned = entityServletHelper.getEvaluationAcl(testUserId, eval1.getId());
		assertNotNull(aclReturned);
		assertEquals(eval1.getId(), aclReturned.getId());
		assertNotNull(aclReturned.getResourceAccess());

		// Update the ACL
		ResourceAccess ra = new ResourceAccess();
		Set<ACCESS_TYPE> accessType = new HashSet<ACCESS_TYPE>();
		accessType.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessType.add(ACCESS_TYPE.READ);
		ra.setAccessType(accessType);
		ra.setPrincipalId(Long.parseLong(testUserInfo.getId().toString()));
		aclReturned.getResourceAccess().add(ra);

		aclReturned = entityServletHelper.updateEvaluationAcl(testUserId, aclReturned);
		assertNotNull(aclReturned);
		assertEquals(eval1.getId(), aclReturned.getId());

		// getAcl()
		aclReturned = entityServletHelper.getEvaluationAcl(testUserId, eval1.getId());
		assertNotNull(aclReturned);
		assertEquals(eval1.getId(), aclReturned.getId());

		// getUserEvaluationPermissions()
		UserEvaluationPermissions uepReturned = entityServletHelper.getEvaluationPermissions(testUserId, eval1.getId());
		assertNotNull(uepReturned);
	}

	private String createNode(String name, UserInfo userInfo) throws Exception {
		Node toCreate = new Node();
		toCreate.setName(name);
		String ownerId = userInfo.getId().toString();
		toCreate.setCreatedByPrincipalId(Long.parseLong(ownerId));
		toCreate.setModifiedByPrincipalId(Long.parseLong(ownerId));
		toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
		toCreate.setModifiedOn(toCreate.getCreatedOn());
		toCreate.setNodeType(EntityType.project);
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("1");
    	String id = nodeManager.createNode(toCreate, userInfo).getId();
    	nodesToDelete.add(KeyFactory.stringToKey(id).toString());
    	return id;
	}
}
