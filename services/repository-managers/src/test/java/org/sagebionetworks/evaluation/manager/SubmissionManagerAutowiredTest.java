package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;


@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SubmissionManagerAutowiredTest {

	@Autowired
	private EvaluationManager evaluationManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private SubmissionManager submissionManager;
	
	@Autowired
	private DockerCommitDao dockerCommitDao;
	
	@Autowired
	private TeamManager teamManager;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private List<String> evalsToDelete;
	private List<String> nodesToDelete;
	private String teamId;
	
	private Node createNode(String name, EntityType type, UserInfo userInfo) throws Exception {
		final long principalId = Long.parseLong(userInfo.getId().toString());
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(principalId);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(principalId);
		node.setNodeType(type);
		node = nodeManager.createNode(node, userInfo);
		nodesToDelete.add(node.getId());
		
		return node;
	}



	@Before
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		evalsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		
		// create a project
		Node project = createNode("project", EntityType.project, adminUserInfo);
		// create an evaluation
		Evaluation evaluation = new Evaluation();
		evaluation.setContentSource(project.getId());
		evaluation.setName("evaluation");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = evaluationManager.createEvaluation(adminUserInfo, evaluation);
		assertNotNull(evaluation.getId());
		evalsToDelete.add(evaluation.getId());
		
		Team team = new Team();
		team.setName("teamName");		
		teamId = teamManager.create(adminUserInfo, team).getId();
	}

	@After
	public void after() throws Exception {
		for (String id: evalsToDelete) {
			evaluationManager.deleteEvaluation(adminUserInfo, id);
		}
		for (String id : nodesToDelete) {
			nodeManager.delete(adminUserInfo, id);
		}
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		teamManager.delete(adminUserInfo, teamId);
	}
	
	private static final String DOCKER_DIGEST = "sha256:abcdef...";
	
	@Test
	public void testDockerRepoSubmissionCreateAndRead() throws Exception {
		// create a docker repository
		String nodeId = createNode("repo", EntityType.dockerrepo, adminUserInfo).getId();
		String evalId = evalsToDelete.get(0);
		
		DockerCommit commit = new DockerCommit();
		commit.setTag("foo");
		commit.setDigest(DOCKER_DIGEST);
		commit.setCreatedOn(new Date());
		dockerCommitDao.createDockerCommit(nodeId, adminUserInfo.getId(), commit);
		
		Node retrievedNode = nodeManager.get(adminUserInfo, nodeId);

		// create a submission
		Submission submission = new Submission();
		submission.setDockerDigest(DOCKER_DIGEST);
		submission.setEntityId(nodeId);
		submission.setEvaluationId(evalId);
		submission.setUserId(""+adminUserInfo.getId());
		submission.setVersionNumber(retrievedNode.getVersionNumber());
		EntityBundle bundle = new EntityBundle();
		bundle.setFileHandles(Collections.EMPTY_LIST);
		assertNotNull(retrievedNode.getETag());
		submission = submissionManager.createSubmission(adminUserInfo, submission, 
				retrievedNode.getETag(), null, bundle);
		
		assertNotNull(submission.getId());
		
		// retrieve the submission
		Submission retrieved = submissionManager.getSubmission(adminUserInfo, submission.getId());
		assertEquals(1, retrieved.getContributors().size());
		assertEquals(""+adminUserInfo.getId(), retrieved.getContributors().iterator().next().getPrincipalId());
		
		assertNotNull(retrieved.getCreatedOn());
		assertNotNull(retrieved.getEntityBundleJSON());
		assertEquals(nodeId, retrieved.getEntityId());
		assertEquals(evalId, retrieved.getEvaluationId());
		assertEquals(submission.getId(), retrieved.getId());
		assertNull(retrieved.getTeamId());
		assertEquals(retrievedNode.getVersionNumber(), retrieved.getVersionNumber());
		assertEquals(DOCKER_DIGEST, retrieved.getDockerDigest());
	}
	
	@Test //(expected = UnauthorizedException.class)
	public void testCreateSubmissionIndividualSubmissionAfterTeamSubmission() throws Exception{
		//this is similar to the (expect=SomeException.class) annotation, but also allows the verification of the exception message
		expectedException.expect(UnauthorizedException.class);
		expectedException.expectMessage("Submitter may not submit as an individual when having submitted as part of a Team");
		
		// create a docker repository
		String nodeId = createNode("repo", EntityType.dockerrepo, adminUserInfo).getId();
		String evalId = evalsToDelete.get(0);
		
		DockerCommit commit = new DockerCommit();
		commit.setTag("foo");
		commit.setDigest(DOCKER_DIGEST);
		commit.setCreatedOn(new Date());
		dockerCommitDao.createDockerCommit(nodeId, adminUserInfo.getId(), commit);
		
		Node retrievedNode = nodeManager.get(adminUserInfo, nodeId);		

		// create a submission
		Submission submission = new Submission();
		submission.setDockerDigest(DOCKER_DIGEST);
		submission.setEntityId(nodeId);
		submission.setEvaluationId(evalId);
		submission.setUserId(""+adminUserInfo.getId());
		submission.setVersionNumber(retrievedNode.getVersionNumber());
		submission.setTeamId(teamId);
		EntityBundle bundle = new EntityBundle();
		bundle.setFileHandles(Collections.EMPTY_LIST);
		assertNotNull(retrievedNode.getETag());
		String submissionEligiblityHash = "" +SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(evaluationManager.getTeamSubmissionEligibility(adminUserInfo, evalId, teamId));
		submissionManager.createSubmission(adminUserInfo, submission, 
				retrievedNode.getETag(), submissionEligiblityHash, bundle);
		
		submission.setTeamId(null);
		submissionManager.createSubmission(adminUserInfo, submission, 
				retrievedNode.getETag(), null, bundle);
		
	}
}
