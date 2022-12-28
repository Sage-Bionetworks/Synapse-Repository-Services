package org.sagebionetworks.worker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionEligibilityManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableSet;

public class TestHelper {
	
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
	private SubmissionEligibilityManager submissionEligibilityManager;
	
	@Autowired
	private TableIndexDAO indexDao;
	
	@Autowired
	private CertifiedUserManager certifiedUserManager;

	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;
	
	private List<String> nodes;
	private List<String> evaluations;
	private List<Long> users;
	private List<String> teams;
	
	private UserInfo adminUser;

	private Random random;
	
	public void before() {

		indexDao.truncateIndex();
		
		nodes = new ArrayList<>();
		evaluations = new ArrayList<>();
		users = new ArrayList<>();
		teams = new ArrayList<>();
		
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		random = new Random();
	}
	
	private void addNodeForCleanup(String node) {
		nodes.add(node);
	}
	
	public void addUserForCleanup(Long user) {
		users.add(user);
	}
	
	public void addTeamForCleanup(String team) {
		teams.add(team);
	}
	
	public void addEvaluationForCleanup(String evaluation) {
		evaluations.add(evaluation);
	}
	
	public void cleanup() {
		for (String evaluation : evaluations) {
			evaluationManager.deleteEvaluation(adminUser, evaluation);
		}
		Collections.reverse(nodes);
		for (String node : nodes) {
			nodeManager.delete(adminUser, node);
		}
		for (String team : teams) {
			teamManager.delete(adminUser, team);
		}
		for (Long user : users) {
			userManager.deletePrincipal(adminUser, user);
		}
		indexDao.truncateIndex();
	}
	
	public UserInfo createUser() {
		NewUser user = new NewUser();
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(user.getUserName() + "@bond.com");
		
		DBOTermsOfUseAgreement touAgreement = new DBOTermsOfUseAgreement();
		touAgreement.setAgreesToTermsOfUse(true);
		
		long userId = userManager.createOrGetTestUser(adminUser, user, null, touAgreement).getId();
		
		addUserForCleanup(userId);
		
		certifiedUserManager.setUserCertificationStatus(adminUser, userId, true);
		
		return userManager.getUserInfo(userId);
	}
	
	public Team createTeam(UserInfo user) {
		Team team = new Team();
		team.setName(UUID.randomUUID().toString());
		
		team = teamManager.create(user, team);

		addTeamForCleanup(team.getId());
		
		user.getGroups().add(Long.valueOf(team.getId()));
		
		return team;
	}

	public Project createProject(UserInfo user) {
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		
		project = createEntity(user, project);

		addNodeForCleanup(project.getId());
	
		return project;
	}
	
	public Folder createFolder(UserInfo user, Entity parent) {
		Folder folder = new Folder();
		
		folder.setParentId(parent.getId());
		folder.setName(UUID.randomUUID().toString());
		
		return createEntity(user, folder);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Entity> T createEntity(UserInfo user, T entity) {
		String id = entityManager.createEntity(user, entity, null);
		
		return (T) entityManager.getEntity(user, id);
	}

	public Evaluation createEvaluation(UserInfo user, Project project) {
		Evaluation evaluation = new Evaluation();

		evaluation.setCreatedOn(new Date());
		evaluation.setName(UUID.randomUUID().toString());
		evaluation.setOwnerId(user.getId().toString());
		evaluation.setContentSource(project.getId());
		evaluation.setEtag(UUID.randomUUID().toString());

		evaluation = evaluationManager.createEvaluation(user, evaluation);

		addEvaluationForCleanup(evaluation.getId());

		return evaluation;
	}

	public EvaluationRound createEvaluationRound(UserInfo userInfo, String evaluationId){
		EvaluationRound round = new EvaluationRound();
		Instant now = Instant.now();
		round.setRoundStart(Date.from(now));
		round.setRoundEnd(Date.from(now.plus(1, ChronoUnit.DAYS)));
		round.setEvaluationId(evaluationId);

		// cleaning up the evaluation will cascade delete the round

		return evaluationManager.createEvaluationRound(userInfo, round);
	}
	
	public SubmissionBundle createSubmission(UserInfo submitter, Evaluation evaluation, Entity entity) throws Exception {
		return createSubmission(submitter, evaluation, entity, null);
	}
	
	public SubmissionBundle createSubmission(UserInfo submitter, Evaluation evaluation, Entity entity, Team team) throws Exception {
		Submission submission = new Submission();
		submission.setEvaluationId(evaluation.getId());
		submission.setEntityId(entity.getId());
		submission.setVersionNumber(1L);
		submission.setName(UUID.randomUUID().toString());
		submission.setSubmitterAlias(UUID.randomUUID().toString());
		
		String teamEligibilityHash = null;
		
		if (team != null) {
			submission.setTeamId(team.getId());
			TeamSubmissionEligibility eligibility = submissionEligibilityManager.getTeamSubmissionEligibility(evaluation, team.getId(), new Date());
			teamEligibilityHash = String.valueOf(eligibility.getEligibilityStateHash());
		}
		
		EntityBundle entityBundle = new EntityBundle();

		entityBundle.setEntity(entity);
		entityBundle.setFileHandles(Collections.emptyList());
		
		submission = submissionManager.createSubmission(submitter, submission, entity.getEtag(), teamEligibilityHash, entityBundle);
		
		SubmissionStatus status = submissionManager.getSubmissionStatus(submitter, submission.getId());
		
		SubmissionBundle bundle = new SubmissionBundle();
		
		bundle.setSubmission(submission);
		bundle.setSubmissionStatus(status);
		
		return bundle;
	}
	

	public void setEvaluationACLForSubmission(UserInfo evaluationOwner, Evaluation evaluation, Team submitterTeam) {
		AccessControlList acl = evaluationPermissionsManager.getAcl(evaluationOwner, evaluation.getId());
		
		ResourceAccess ra = new ResourceAccess();
		
		ra.setAccessType(ImmutableSet.of(ACCESS_TYPE.PARTICIPATE, ACCESS_TYPE.SUBMIT, ACCESS_TYPE.READ));
		ra.setPrincipalId(Long.valueOf(submitterTeam.getId()));
		
		acl.getResourceAccess().add(ra);
		
		evaluationPermissionsManager.updateAcl(evaluationOwner, acl);
	}
	
	/**
	 * Create File entity with multiple versions using the annotations for each version.
	 *
	 * @return
	 */
	public FileEntity createFileWithMultipleVersions(UserInfo userInfo, String parentId, int fileNumber, String annotationKey, int numberOfVersions) {
		List<Annotations> annotations = new ArrayList<>(numberOfVersions);
		for(int i=1; i <= numberOfVersions; i++) {
			Annotations annos = new Annotations();
			AnnotationsV2TestUtils.putAnnotations(annos, annotationKey, "v-"+i, AnnotationsValueType.STRING);
			annotations.add(annos);
		}

		// create the entity
		String fileEntityId = null;		
		int version = 1;
		
		for(Annotations annos: annotations) {
						
			long fileContentSize = (long) random.nextInt(128_000);
			
			// Create a new file handle for each version
			S3FileHandle fileHandle = fileHandleDaoHelper.create((f) -> {
				f.setCreatedBy(userInfo.getId().toString());
				f.setFileName("someFile");
				f.setContentSize(fileContentSize);
			});
			
			if (fileEntityId == null) {
				fileEntityId = entityManager.createEntity(userInfo, new FileEntity()
						.setName("afile-"+fileNumber)
						.setParentId(parentId)
						.setDataFileHandleId(fileHandle.getId()),
						null);
				addNodeForCleanup(fileEntityId);
			} else {
				// create a new version for the entity
				FileEntity entity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
				entity.setVersionComment("c-"+version);
				entity.setVersionLabel("v-"+version);
				entity.setDataFileHandleId(fileHandle.getId());
				boolean newVersion = true;
				String activityId = null;
				entityManager.updateEntity(userInfo, entity, newVersion, activityId);
			}
			// get the ID and etag
			FileEntity entity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
			annos.setId(entity.getId());
			annos.setEtag(entity.getEtag());
			entityManager.updateAnnotations(userInfo, fileEntityId, annos);
			version++;
		}
		
		return entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
	}

}
