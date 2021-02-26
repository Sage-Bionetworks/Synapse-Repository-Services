package org.sagebionetworks.repo.manager.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
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
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private List<String> evalsToDelete;
	private List<String> nodesToDelete;
	private String teamId;
	private String nodeId;
	private String evalId;
	private Submission submission;
	private Node retrievedNode;
	private EntityBundle bundle;
	
	private static final String DOCKER_DIGEST = "sha256:abcdef...";
	private static final String DOCKER_REPOSITORY_NAME = "docker.synapse.org/syn123/arepo";
	
	

	
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
		
		
		//setup for submission
		nodeId = createNode("repo", EntityType.dockerrepo, adminUserInfo).getId();
		evalId = evalsToDelete.get(0);
		
		DockerCommit commit = new DockerCommit();
		commit.setTag("foo");
		commit.setDigest(DOCKER_DIGEST);
		commit.setCreatedOn(new Date());
		dockerCommitDao.createDockerCommit(nodeId, adminUserInfo.getId(), commit);
		
		retrievedNode = nodeManager.getNode(adminUserInfo, nodeId);

		// set up a submission
		submission = new Submission();
		submission.setDockerDigest(DOCKER_DIGEST);
		submission.setEntityId(nodeId);
		submission.setEvaluationId(evalId);
		submission.setUserId(""+adminUserInfo.getId());
		submission.setVersionNumber(retrievedNode.getVersionNumber());
		bundle = new EntityBundle();
		DockerRepository entity = new DockerRepository();
		entity.setRepositoryName(DOCKER_REPOSITORY_NAME);
		bundle.setEntity(entity);
		bundle.setFileHandles(Collections.EMPTY_LIST);
		assertNotNull(retrievedNode.getETag());
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
	
	@Test
	public void testDockerRepoSubmissionCreateAndRead() throws Exception {
		// when we create the object we don't fill in the Docker Repo name field
		// but it will be filled in when we retrieve it
		assertNull(submission.getDockerRepositoryName());
		
		// create a docker repository
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
		// voila, the repository name is filled in!
		assertEquals(DOCKER_REPOSITORY_NAME, retrieved.getDockerRepositoryName());
		assertEquals(DOCKER_DIGEST, retrieved.getDockerDigest());
	}

	@Test
	public void testCreateSubmission_taggedWithCurrentEvaluationRound() throws Exception {
		EvaluationRound evaluationRound = new EvaluationRound();
		evaluationRound.setEvaluationId(evalId);
		Instant now = Instant.now();
		evaluationRound.setRoundStart(Date.from(now));
		evaluationRound.setRoundEnd(Date.from(now.plus(42, ChronoUnit.HOURS)));
		evaluationRound = evaluationManager.createEvaluationRound(adminUserInfo, evaluationRound);

		// create a docker repository
		submission = submissionManager.createSubmission(adminUserInfo, submission,
				retrievedNode.getETag(), null, bundle);

		assertEquals(evaluationRound.getId(), submission.getEvaluationRoundId());

		// retrieve the submission
		Submission retrieved = submissionManager.getSubmission(adminUserInfo, submission.getId());
		assertEquals(evaluationRound.getId(), retrieved.getEvaluationRoundId());

	}
	
	@Test
	public void testCreateSubmissionIndividualSubmissionAfterTeamSubmission() throws Exception{
		// create a submission for team
		submission.setTeamId(teamId);
		String submissionEligiblityHash = evaluationManager.getTeamSubmissionEligibility(adminUserInfo, evalId, teamId).getEligibilityStateHash().toString();
		submissionManager.createSubmission(adminUserInfo, submission, 
				retrievedNode.getETag(), submissionEligiblityHash, bundle);
		
		//create second submission as individual
		submission.setTeamId(null);
		try{
			submissionManager.createSubmission(adminUserInfo, submission, 
				retrievedNode.getETag(), null, bundle);
		}catch(UnauthorizedException e){
			//verify the error message because there are many UnauthorizedExceptions that could be thrown
			assertTrue(e.getMessage().equals("Submitter may not submit as an individual when having submitted as part of a Team."));
			return;
		}
		fail("No exception was thrown even though it was expected");
	}
}
