package org.sagebionetworks.replication.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.evaluation.dao.SubmissionField;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.manager.SubmissionManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@ActiveProfiles("test-replication-workers")
public class SubmissionReplicationIntegrationTest {
	
	private static final int MAX_WAIT = 2 * 60 * 1000;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TeamManager teamManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private EvaluationManager evaluationManager;
	
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;

	@Autowired
	private SubmissionManager submissionManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	private TableIndexDAO indexDao;
	
	private UserInfo adminUser;
	
	private UserInfo evaluationOwner;

	private UserInfo submitter;
	
	private Team submitterTeam;

	private Project evaluationProject;
	private Project submitterProject;
	
	private Evaluation evaluation;
	
	private List<String> nodes;
	private List<String> evaluations;
	private List<Long> users;
	private List<String> teams;

	@BeforeEach
	public void before() throws Exception {
		indexDao.truncateIndex();
		
		nodes = new ArrayList<>();
		evaluations = new ArrayList<>();
		users = new ArrayList<>();
		teams = new ArrayList<>();
		
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		evaluationOwner = createUser();
		submitter = createUser();
		submitterTeam = createTeam(submitter);
		submitterProject = createProject(submitter);
		
		
		evaluationProject = createProject(evaluationOwner);
		evaluation = createEvaluation(evaluationOwner, evaluationProject);

		setEvaluationACL(evaluationOwner, evaluation, submitterTeam);
	}

	@AfterEach
	public void after() {
		for (String evaluation : evaluations) {
			evaluationManager.deleteEvaluation(adminUser, evaluation);
		}
		for (String node : nodes) {
			nodeManager.delete(adminUser, node);
		}
		for (String team : teams) {
			teamManager.delete(adminUser, team);
		}
		for (Long user : users) {
			userManager.deletePrincipal(adminUser, user);
		}
		//indexDao.truncateIndex();
	}

	@Test
	public void testSubmissionReplication() throws Exception {
		Entity entity = createFolder(submitter, submitterProject);
		SubmissionBundle submission = createSubmission(submitter, evaluation, entity);
		
		Long submissionId = Long.valueOf(submission.getSubmissionStatus().getId());
		String etag = submission.getSubmissionStatus().getEtag();
		
		ObjectDataDTO data = asyncHelper.waitForObjectReplication(ViewObjectType.SUBMISSION, submissionId, etag, MAX_WAIT);

		assertEquals(KeyFactory.stringToKey(evaluationProject.getId()), data.getProjectId());
		assertEquals(KeyFactory.stringToKey(evaluation.getId()), data.getParentId());
		assertEquals(KeyFactory.stringToKey(evaluation.getId()), data.getBenefactorId());
		assertEquals(submitter.getId(), data.getCreatedBy());
		
		Map<String, ObjectAnnotationDTO> annotations = data.getAnnotations()
				.stream()
				.collect(Collectors.toMap(ObjectAnnotationDTO::getKey, Function.identity()));
		
		assertFalse(annotations.isEmpty());
		
		assertValue(submitter.getId().toString(), annotations.get(SubmissionField.submitterid.getColumnName()));
		assertValue(submission.getSubmissionStatus().getStatus().name(), annotations.get(SubmissionField.status.getColumnName()));

		// Updates the status
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission.getSubmission().getId());
		status.setStatus(SubmissionStatusEnum.EVALUATION_IN_PROGRESS);
		
		// And add some annotations
		Annotations submissionAnnotations = AnnotationsV2Utils.emptyAnnotations();
		AnnotationsV2TestUtils.putAnnotations(submissionAnnotations, "foo", "bar", AnnotationsValueType.STRING);
		status.setSubmissionAnnotations(submissionAnnotations);
		
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		etag = status.getEtag();
		
		data = asyncHelper.waitForObjectReplication(ViewObjectType.SUBMISSION, submissionId, etag, MAX_WAIT);
		
		annotations = data.getAnnotations()
				.stream()
				.collect(Collectors.toMap(ObjectAnnotationDTO::getKey, Function.identity()));
		
		// Makes sure the status was updated
		assertValue(status.getStatus().name(), annotations.get(SubmissionField.status.getColumnName()));
		// And that the annotation was added
		assertValue("bar", annotations.get("foo"));
		
	}
	
	private void assertValue(String expectedValue, ObjectAnnotationDTO annotation) {
		assertEquals(expectedValue, annotation.getValue().iterator().next());
	}

	private UserInfo createUser() {
		NewUser user = new NewUser();
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(user.getUserName() + "@bond.com");
		long userId = userManager.createUser(user);
		
		users.add(userId);
		
		UserInfo userInfo = userManager.getUserInfo(userId);
		
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		return userInfo;
	}
	
	private Team createTeam(UserInfo user) {
		Team team = new Team();
		team.setName(UUID.randomUUID().toString());
		
		team = teamManager.create(user, team);
		
		teams.add(team.getId());
		
		user.getGroups().add(Long.valueOf(team.getId()));
		
		return team;
	}

	private Project createProject(UserInfo user) throws Exception {
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		
		project = createEntity(user, project);
	
		nodes.add(project.getId());
		
		return project;
	}
	
	private Folder createFolder(UserInfo user, Entity parent) {
		Folder folder = new Folder();
		
		folder.setParentId(parent.getId());
		folder.setName(UUID.randomUUID().toString());
		
		return createEntity(user, folder);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Entity> T createEntity(UserInfo user, T entity) {
		String id = entityManager.createEntity(user, entity, null);
		
		return (T) entityManager.getEntity(user, id);
	}

	private Evaluation createEvaluation(UserInfo user, Project project) {
		Evaluation evaluation = new Evaluation();

		evaluation.setCreatedOn(new Date());
		evaluation.setName(UUID.randomUUID().toString());
		evaluation.setOwnerId(user.getId().toString());
		evaluation.setContentSource(project.getId());
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation.setEtag(UUID.randomUUID().toString());

		evaluation = evaluationManager.createEvaluation(user, evaluation);
		
		evaluations.add(evaluation.getId());

		return evaluation;
	}
	
	private SubmissionBundle createSubmission(UserInfo submitter, Evaluation evaluation, Entity entity) throws Exception {
		Submission submission = new Submission();
		submission.setEvaluationId(evaluation.getId());
		submission.setEntityId(entity.getId());
		submission.setVersionNumber(1L);
		submission.setName(UUID.randomUUID().toString());
		submission.setSubmitterAlias(UUID.randomUUID().toString());
		
		EntityBundle entityBundle = new EntityBundle();

		entityBundle.setEntity(entity);
		entityBundle.setFileHandles(Collections.emptyList());
		
		submission = submissionManager.createSubmission(submitter, submission, entity.getEtag(), null, entityBundle);
		
		SubmissionStatus status = submissionManager.getSubmissionStatus(submitter, submission.getId());
		
		SubmissionBundle bundle = new SubmissionBundle();
		
		bundle.setSubmission(submission);
		bundle.setSubmissionStatus(status);
		
		return bundle;
	}
	
	private void setEvaluationACL(UserInfo evaluationOwner, Evaluation evaluation, Team submitterTeam) {
		AccessControlList acl = evaluationPermissionsManager.getAcl(evaluationOwner, evaluation.getId());
		
		ResourceAccess ra = new ResourceAccess();
		
		ra.setAccessType(ImmutableSet.of(ACCESS_TYPE.PARTICIPATE, ACCESS_TYPE.SUBMIT, ACCESS_TYPE.READ));
		ra.setPrincipalId(Long.valueOf(submitterTeam.getId()));
		
		acl.getResourceAccess().add(ra);
		
		evaluationPermissionsManager.updateAcl(evaluationOwner, acl);
	}

}
