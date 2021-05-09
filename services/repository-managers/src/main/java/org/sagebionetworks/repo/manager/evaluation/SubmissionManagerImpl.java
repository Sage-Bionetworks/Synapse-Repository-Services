package org.sagebionetworks.repo.manager.evaluation;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_CHALLENGE_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_CHALLENGE_WEB_LINK;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_EVAL_QUEUE_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_ID;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_USER_ID;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.EvaluationSubmissionsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubmissionManagerImpl implements SubmissionManager {

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private SubmissionDAO submissionDAO;
	@Autowired
	private SubmissionStatusDAO submissionStatusDAO;
	@Autowired
	private SubmissionFileHandleDAO submissionFileHandleDAO;
	@Autowired
	private EvaluationSubmissionsDAO evaluationSubmissionsDAO;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private SubmissionEligibilityManager submissionEligibilityManager;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private UserProfileManager userProfileManager;
	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private DockerCommitDao dockerCommitDao;
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	public static final long MAX_LIMIT = 100L;

	private static final int MAX_BATCH_SIZE = 500;
	
	public static final String TEAM_SUBMISSION_NOTIFICATION_TEMPLATE = "message/teamSubmissionNotificationTemplate.html";

	private static final String TEAM_SUBMISSION_SUBJECT = "Team Challenge Submission";

	private static final String ONLY_SUBMITTER_REASON = "Only the user who submitted this submission could perform this action.";

	private static final String NON_CANCELLABLE_REASON = "This submission is currently noncancellable.";

	@Override
	public Submission getSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		ValidateArgument.required(submissionId, "Submission ID");
		Submission sub = submissionDAO.get(submissionId);
		validateEvaluationAccess(userInfo, sub.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		return sub;
	}

	@Override
	public SubmissionStatus getSubmissionStatus(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		ValidateArgument.required(submissionId, "Submission ID");
		SubmissionBundle bundle = submissionDAO.getBundle(submissionId, false);
		String evaluationId = bundle.getSubmission().getEvaluationId();
		validateEvaluationAccess(userInfo, evaluationId, ACCESS_TYPE.READ);
		// only authorized users can view private Annotations 
		SubmissionStatus result = bundle.getSubmissionStatus();
		boolean includePrivateAnnos = evaluationPermissionsManager.hasAccess(userInfo, evaluationId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).isAuthorized();
		if (!includePrivateAnnos) {
			removePrivateAnnotations(result);
		}
		return result;
	}
	
	static boolean isTeamSubmission(Submission submission, String submissionEligibilityHash) {
		return (submission.getTeamId()!=null && submissionEligibilityHash!=null);
	}
	
	static boolean isIndividualSubmission(Submission submission, String submissionEligibilityHash) {
		return submission.getTeamId()==null && submissionEligibilityHash==null && 
				submission.getContributors().size()==1 &&
				submission.getContributors().iterator().next().getPrincipalId().equals(submission.getUserId());
	}
	
	AuthorizationStatus checkSubmissionEligibility(UserInfo userInfo, Submission submission, String submissionEligibilityHash, Date now) throws DatastoreException, NotFoundException {
		String evalId = submission.getEvaluationId();
		if (isTeamSubmission(submission, submissionEligibilityHash)) {
			List<String> contributors = new ArrayList<String>();
			for (SubmissionContributor sc : submission.getContributors()) {
				contributors.add(sc.getPrincipalId());
			}
			return submissionEligibilityManager.isTeamEligible(
					evalId, submission.getTeamId(), contributors, submissionEligibilityHash, now);
		} else if (isIndividualSubmission(submission, submissionEligibilityHash)) {
			return submissionEligibilityManager.isIndividualEligible(evalId, userInfo, now);
		} else {
			throw new InvalidModelException("Submission is neither a valid Team or Individual Submission.");
		}
	}

	@Override
	@WriteTransaction
	public Submission createSubmission(UserInfo userInfo, Submission submission, String entityEtag, String submissionEligibilityHash, EntityBundle bundle)
			throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		ValidateArgument.required(submission, "Submission");
		ValidateArgument.required(bundle, "EntityBundle");
		String evalId = submission.getEvaluationId();
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getId().toString();
		
		submission.setUserId(principalId);

		if(submission.getEvaluationRoundId() != null){
			throw new IllegalArgumentException("Please do not specify any evaluationRoundId. This will be filled automatically based upon the time of creation.");
		}

		// tag the submission with the current submission round if it is present
		// if it doesn't exist it could possibly be because the user does not exist yet
		evaluationDAO.getEvaluationRoundForTimestamp(evalId, Instant.now())
			.ifPresent((EvaluationRound round) -> {
				submission.setEvaluationRoundId(round.getId());
			});

		// validate permissions
		evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT).checkAuthorizationOrElseThrow();

		// validate eTag
		String entityId = submission.getEntityId();
		Node node = nodeManager.getNode(userInfo, entityId);
		if (!node.getETag().equals(entityEtag)) {
			// invalid eTag; reject the Submission
			throw new IllegalArgumentException("The supplied eTag is out of date. " +
					"Please fetch Entity " + entityId + " again.");
		}
		
		if (node.getNodeType()==EntityType.dockerrepo) {
			String dockerRepositoryName = ((DockerRepository)bundle.getEntity()).getRepositoryName();
			ValidateArgument.required(dockerRepositoryName, "Docker Repository Name");
			submission.setDockerRepositoryName(dockerRepositoryName);
			ValidateArgument.required(submission.getDockerDigest(), "Docker Digest");
			List<DockerCommit> commits = dockerCommitDao.
					listCommitsByOwnerAndDigest(entityId, submission.getDockerDigest());
			if (commits.isEmpty()) throw new IllegalArgumentException("The given Docker Repository, "+
					entityId+", does not have digest "+submission.getDockerDigest());
		} else {
			submission.setDockerRepositoryName(null);
			submission.setDockerDigest(null);
		}
		
		// let's use a single time stamp for everything we do in this transaction
		Date now = new Date();
		
		// set created on date in contributors list and make sure creator is a contributor
		Set<SubmissionContributor> scs = new HashSet<SubmissionContributor>();
		boolean creatorIsIncluded = false;
		if (submission.getContributors()!=null) {
			for (SubmissionContributor sc : submission.getContributors()) {
				// don't want to mutate an object in a hashset, so let's make a new one
				SubmissionContributor scWithDate = new SubmissionContributor();
				scWithDate.setPrincipalId(sc.getPrincipalId());
				scWithDate.setCreatedOn(now);
				scs.add(scWithDate);
				if (scWithDate.getPrincipalId().equals(principalId)) creatorIsIncluded=true;
			}
		}
		if (!creatorIsIncluded) {
			SubmissionContributor scWithDate = new SubmissionContributor();
			scWithDate.setPrincipalId(principalId);
			scWithDate.setCreatedOn(now);
			scs.add(scWithDate);
		}
		submission.setContributors(scs);
		
		checkSubmissionEligibility(userInfo, submission, submissionEligibilityHash, now).checkAuthorizationOrElseThrow();
		
		// if no name is provided, use the Entity name
		if (submission.getName() == null) {
			submission.setName(node.getName());
		}
		
		// insert EntityBundle JSON
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		bundle.writeToJSONObject(joa);
		submission.setEntityBundleJSON(joa.toJSONString());
		
		// always generate a unique ID
		submission.setId(idGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID).toString());
				
		// set creation date
		submission.setCreatedOn(now);
		
		// create the Submission	
		String submissionId = submissionDAO.create(submission);
		
		// create an accompanying SubmissionStatus object
		SubmissionStatus status = new SubmissionStatus();
		status.setId(submissionId);
		status.setStatus(SubmissionStatusEnum.RECEIVED);
		status.setModifiedOn(now);
		
		submissionStatusDAO.create(status);
		
		// save FileHandle IDs
		for (FileHandle handle : bundle.getFileHandles()) {
			submissionFileHandleDAO.create(submissionId, handle.getId());
		}
		
		// update the EvaluationSubmissions etag
		Long evalIdLong = KeyFactory.stringToKey(submission.getEvaluationId());
		evaluationSubmissionsDAO.updateEtagForEvaluation(evalIdLong, true, ChangeType.CREATE);
		
		// Send out a message for the new submission status
		sendSubmissionMessage(userInfo, ChangeType.CREATE, submissionId);
		
		// return the Submission
		return submissionDAO.get(submissionId);
	}
	
	/**
	 * create notifications for everyone on a team submission other than the submitter
	 * @param submission
	 * @return
	 */
	public List<MessageToUserAndBody> createSubmissionNotifications(UserInfo userInfo, 
			Submission submission, String submissionEligibilityHash,
			String challengeEndpoint, String notificationUnsubscribeEndpoint) {
		
		ValidateArgument.required(challengeEndpoint, "challengeEndpoint");
		ValidateArgument.required(notificationUnsubscribeEndpoint, "notificationUnsubscribeEndpoint");
		
		if (!isTeamSubmission(submission, submissionEligibilityHash)) {
			// no contributors to notify, so just return an empty list
			return Collections.emptyList();
		}

		String submitterId = submission.getUserId();
		String teamId = submission.getTeamId();
		String evaluationId = submission.getEvaluationId();
		
		Team team = teamDAO.get(teamId);
		Evaluation evaluation = evaluationDAO.get(evaluationId);
		
		// notify all but the one who submitted.  If there is no one else on the team
		// then this list will be empty and no notification will be sent.
		Set<String> recipients = new HashSet<>();
		
		for (SubmissionContributor contributor : submission.getContributors()) {
			if (submitterId.equals(contributor.getPrincipalId())) {
				continue;
			}
			recipients.add(contributor.getPrincipalId());
		}
		
		if (recipients.isEmpty()) {
			return Collections.emptyList();
		}
		
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_TEAM_NAME, team.getName());
		fieldValues.put(TEMPLATE_KEY_TEAM_ID, teamId);
		
		String challengeEntityId = evaluation.getContentSource();
		String nodeName = null;
		try {
			nodeName = nodeManager.getNodeName(userInfo, challengeEntityId);
		} catch (UnauthorizedException e) {
			nodeName = null;
		}
		String challengeName = null;
		if (nodeName==null) {
			challengeName = challengeEntityId;
		} else {
			challengeName = nodeName;
		}
		
		fieldValues.put(TEMPLATE_KEY_CHALLENGE_NAME, challengeName);
		String challengeEntityURL = challengeEndpoint+challengeEntityId;
		EmailUtils.validateSynapsePortalHost(challengeEntityURL);
		fieldValues.put(TEMPLATE_KEY_CHALLENGE_WEB_LINK, challengeEntityURL);
			
		UserProfile userProfile = userProfileManager.getUserProfile(submitterId);
		String displayName = EmailUtils.getDisplayNameWithUsername(userProfile);
		fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, displayName);
		fieldValues.put(TEMPLATE_KEY_USER_ID, submitterId);
		fieldValues.put(TEMPLATE_KEY_EVAL_QUEUE_NAME, evaluation.getName());


		String to = EmailUtils.getEmailAddressForPrincipalName(team.getName());
		String messageContent = EmailUtils.readMailTemplate(TEAM_SUBMISSION_NOTIFICATION_TEMPLATE, fieldValues);
		
		return buildSubmissionNotificationMessages(to, recipients, messageContent, notificationUnsubscribeEndpoint);
	}
	
	protected List<MessageToUserAndBody> buildSubmissionNotificationMessages(String to, Set<String> recipients, String messageContent, String notificationUnsubscribeEndpoint) {
		List<MessageToUserAndBody> result = new ArrayList<>();
		for (String recipient : recipients) {
			MessageToUser mtu = new MessageToUser();			
			mtu.setTo(to);
			mtu.setSubject(TEAM_SUBMISSION_SUBJECT);
			mtu.setRecipients(Collections.singleton(recipient));
			mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
			result.add(new MessageToUserAndBody(mtu, messageContent, ContentType.TEXT_HTML.getMimeType()));
		}
		return result;
	}
	
	@Override
	@WriteTransaction
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo, SubmissionStatus submissionStatus) throws NotFoundException {
		ValidateArgument.required(submissionStatus, "SubmissionStatus");
		UserInfo.validateUserInfo(userInfo);
		
		// ensure Submission exists and validate access rights
		String evalId = getSubmission(userInfo, submissionStatus.getId()).getEvaluationId();		
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.UPDATE_SUBMISSION);
		
		validateContent(submissionStatus, evalId);
		
		// update and return the new Submission
		submissionStatusDAO.update(Collections.singletonList(submissionStatus));
		
		// update the EvaluationSubmissions etag
		Long evalIdLong = KeyFactory.stringToKey(evalId);
		evaluationSubmissionsDAO.updateEtagForEvaluation(evalIdLong, true, ChangeType.UPDATE);
		
		sendSubmissionMessage(userInfo, ChangeType.UPDATE, submissionStatus.getId());
		
		return submissionStatusDAO.get(submissionStatus.getId());
	}
	
	static void validateContent(SubmissionStatus submissionStatus, String evalId) {
		// validate score, if any
		Double score = submissionStatus.getScore();
		if (score != null) {
			if (score > 1.0 || score < 0.0)
				throw new IllegalArgumentException("Scores must be between 0 and 1. Received score = " + score);
		}
		
		// validate Annotations, if any
		Annotations annos = submissionStatus.getAnnotations();
		
		if (annos != null) {
			AnnotationsUtils.validateAnnotations(annos);
			// populate missing fields, specifically make sure 'isPrivate' is filled in
			AnnotationsUtils.populateMissingFields(annos);
			annos.setObjectId(submissionStatus.getId());
			annos.setScopeId(evalId);
		}
		
		// Validate the new annotations V2 if present
		if (submissionStatus.getSubmissionAnnotations() != null) {
			AnnotationsV2Utils.validateAnnotations(submissionStatus.getSubmissionAnnotations());
		}
	}
	
	@Override
	@WriteTransaction
	public BatchUploadResponse updateSubmissionStatusBatch(UserInfo userInfo, String evalId,
			SubmissionStatusBatch batch) throws NotFoundException, ConflictingUpdateException {
		
		UserInfo.validateUserInfo(userInfo);
		
		// validate content of batch
		ValidateArgument.required(batch.getIsFirstBatch(), "isFirstBatch");
		ValidateArgument.required(batch.getIsLastBatch(), "isLastBatch");
		ValidateArgument.required(batch.getStatuses(), "statuses");
		ValidateArgument.requiredNotEmpty(batch.getStatuses(), "statuses");
		if (batch.getStatuses().size()>MAX_BATCH_SIZE) 
			throw new IllegalArgumentException("Batch size cannot exceed "+MAX_BATCH_SIZE);
		for (SubmissionStatus submissionStatus : batch.getStatuses()) {
			ValidateArgument.required(submissionStatus, "SubmissionStatus");
			validateContent(submissionStatus, evalId);
		}
		
		String evalIdForBatch = submissionStatusDAO.getEvaluationIdForBatch(batch.getStatuses()).toString();
		if (!evalIdForBatch.equals(evalId)) 
			throw new IllegalArgumentException("Specified Evaluation ID does not match submissions in the batch.");
		
		// ensure Submission exists and validate access rights
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.UPDATE_SUBMISSION);
		
		Long evalIdLong = KeyFactory.stringToKey(evalId);
		EvaluationSubmissions evalSubs = evaluationSubmissionsDAO.lockAndGetForEvaluation(evalIdLong);
		// if not first batch, check batch etag
		if (!batch.getIsFirstBatch()) {
			String batchToken = batch.getBatchToken();
			ValidateArgument.required(batchToken, "batchToken");
			if (!batchToken.equals(evalSubs.getEtag()))
				throw new ConflictingUpdateException("Your batch token is out of date.  You must restart upload from first batch.");
		}

		// update the Submissions
		submissionStatusDAO.update(batch.getStatuses());
		
		batch.getStatuses().forEach( status-> {
			sendSubmissionMessage(userInfo, ChangeType.UPDATE, status.getId());
		});
		
		String newEvaluationSubmissionsEtag = 
				evaluationSubmissionsDAO.updateEtagForEvaluation(evalIdLong, batch.getIsLastBatch(), ChangeType.UPDATE);
		BatchUploadResponse response = new BatchUploadResponse();
		if (batch.getIsLastBatch()) {
			response.setNextUploadToken(null);
		} else {
			response.setNextUploadToken(newEvaluationSubmissionsEtag);
		}
		return response;
	}
	
	/**
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @param submissionContributor
	 * @return
	 */
	@Override
	@WriteTransaction
	public SubmissionContributor addSubmissionContributor(UserInfo userInfo,
			String submissionId, SubmissionContributor submissionContributor) {
		if (!userInfo.isAdmin()) throw new UnauthorizedException("This service is only available to Synapse administrators.");
		SubmissionContributor created=new SubmissionContributor();
		created.setPrincipalId(submissionContributor.getPrincipalId());
		created.setCreatedOn(new Date());
		submissionDAO.addSubmissionContributor(submissionId, created);
		return created;
	}

	@Override
	@WriteTransaction
	public void deleteSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		
		Submission sub = submissionDAO.get(submissionId);		
		String evalId = sub.getEvaluationId();
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.DELETE_SUBMISSION);
		
		// the associated SubmissionStatus object will be deleted via cascade
		Long evalIdLong = KeyFactory.stringToKey(evalId);
		evaluationSubmissionsDAO.updateEtagForEvaluation(evalIdLong, true, ChangeType.UPDATE);
		
		sendSubmissionMessage(userInfo, ChangeType.DELETE, submissionId);
		
		submissionDAO.delete(submissionId);
	}

	@Override
	public List<Submission> getAllSubmissions(UserInfo userInfo, String evalId, SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		ValidateArgument.required(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.requirement(limit >= 0 && limit <= MAX_LIMIT, "limit must be between 0 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		
		return getAllSubmissionsPrivate(evalId, status, limit, offset);
	}

	private List<Submission> getAllSubmissionsPrivate(String evalId,
			SubmissionStatusEnum status, long limit, long offset)
			throws DatastoreException, NotFoundException {		
		List<Submission> submissions;
		if (status == null)	{
			submissions = submissionDAO.getAllByEvaluation(evalId, limit, offset);
		} else {
			submissions = submissionDAO.getAllByEvaluationAndStatus(evalId, status, limit, offset);
		}
		return submissions;
	}
	
	private List<SubmissionBundle> getAllSubmissionBundlesPrivate(String evalId,
			SubmissionStatusEnum status, long limit, long offset, boolean includePrivateAnnos)
					throws DatastoreException, NotFoundException {		
		List<SubmissionBundle> bundles;
		if (status == null)	{
			bundles = submissionDAO.getAllBundlesByEvaluation(evalId, limit, offset);
		} else {
			bundles = submissionDAO.getAllBundlesByEvaluationAndStatus(evalId, status, limit, offset);
		}
		if (!includePrivateAnnos) {
			bundles.forEach(SubmissionManagerImpl::removePrivateAnnotations);
		}
		return bundles;
	}
	
	@Override
	public List<SubmissionStatus> getAllSubmissionStatuses(UserInfo userInfo, String evalId, 
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		ValidateArgument.required(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.requirement(limit >= 0 && limit <= MAX_LIMIT, "limit must be between 0 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ);
		// only authorized users can view private Annotations
		boolean includePrivateAnnos = evaluationPermissionsManager.hasAccess(
				userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).isAuthorized();
		List<SubmissionBundle> bundles = 
				getAllSubmissionBundlesPrivate(evalId, status, limit, offset, includePrivateAnnos);
		List<SubmissionStatus> result = new ArrayList<SubmissionStatus>();
		for (SubmissionBundle bundle : bundles) {
			result.add(bundle.getSubmissionStatus());
		}
		return result;
	}
	
	@Override
	public List<SubmissionBundle> getAllSubmissionBundles(UserInfo userInfo, String evalId, 
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		ValidateArgument.required(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.requirement(limit >= 0 && limit <= MAX_LIMIT, "limit must be between 0 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		
		return getAllSubmissionBundlesPrivate(evalId, status, limit, offset, true);
	}
	
	@Override
	public List<Submission> getMyOwnSubmissionsByEvaluation(UserInfo userInfo,
			String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		String principalId = userInfo.getId().toString();
		ValidateArgument.requirement(limit >= 0 && limit <= MAX_LIMIT, "limit must be between 0 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		return submissionDAO.getAllByEvaluationAndUser(evalId, principalId, limit, offset);
	}
	
	@Override
	public List<SubmissionBundle> getMyOwnSubmissionBundlesByEvaluation(
			UserInfo userInfo, String evalId, long limit, long offset)
					throws DatastoreException, NotFoundException {
		ValidateArgument.required(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.requirement(limit >= 0 && limit <= MAX_LIMIT, "limit must be between 0 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		boolean includePrivateAnnotations = evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).isAuthorized();

		String principalId = userInfo.getId().toString();
		List<SubmissionBundle> result = submissionDAO.getAllBundlesByEvaluationAndUser(evalId, principalId, limit, offset);
		
		if (!includePrivateAnnotations) {
			result.forEach(SubmissionManagerImpl::removePrivateAnnotations);
		}

		return result;
	}
		
	@Override
	public long getSubmissionCount(UserInfo userInfo, String evalId) 
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(userInfo, "UserInfo");
		ValidateArgument.required(evalId, "Evaluation ID");
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		return submissionDAO.getCountByEvaluation(evalId);
	}
	
	@Override
	public String getRedirectURLForFileHandle(UserInfo userInfo,
			String submissionId, String fileHandleId) 
			throws DatastoreException, NotFoundException {
		Submission submission = getSubmission(userInfo, submissionId);
		validateEvaluationAccess(userInfo, submission.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);

		// ensure that the requested ID is included in the Submission
		List<String> ids = submissionFileHandleDAO.getAllBySubmission(submissionId);
		if (!ids.contains(fileHandleId)) {
			throw new NotFoundException("Submission " + submissionId + " does " +
					"not contain the requested FileHandle " + fileHandleId);
		}
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.SubmissionAttachment, submissionId);
		
		// generate the URL
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}
	
	/**
	 * Check that a user has the specified permission on an Evaluation
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param accessType
	 * @throws NotFoundException
	 */
	private void validateEvaluationAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType)
			throws NotFoundException {
		evaluationPermissionsManager.hasAccess(userInfo, evalId, accessType).checkAuthorizationOrElseThrow();
	}
	
	static void removePrivateAnnotations(SubmissionBundle bundle) {
		removePrivateAnnotations(bundle.getSubmissionStatus());
	}
	
	static void removePrivateAnnotations(SubmissionStatus status) {
		Annotations annotationsV1 = status.getAnnotations();
		if (annotationsV1 != null) {
			status.setAnnotations(removePrivateAnnotations(annotationsV1));
		}
		// By default only who has private access can read the submission (V2) annotations
		status.setSubmissionAnnotations(null);
	}

	/**
	 * Remove all Annotations with [isPrivate == true] from an Annotations object
	 * 
	 * @param annos
	 * @return
	 */
	static Annotations removePrivateAnnotations(Annotations annos) {
		ValidateArgument.required(annos, "Annotations");

		List<StringAnnotation> oldStringAnnos = annos.getStringAnnos();
		if (oldStringAnnos!=null) {
			List<StringAnnotation> newStringAnnos = new ArrayList<StringAnnotation>();
			for (StringAnnotation sa : oldStringAnnos) {
				if (!sa.getIsPrivate()) {
					newStringAnnos.add(sa);
				}
			}
			annos.setStringAnnos(newStringAnnos);
		}
		
		List<DoubleAnnotation> oldDoubleAnnos = annos.getDoubleAnnos();
		if (oldDoubleAnnos!=null) {
			List<DoubleAnnotation> newDoubleAnnos = new ArrayList<DoubleAnnotation>();
			for (DoubleAnnotation da : oldDoubleAnnos) {
				if (!da.getIsPrivate()) {
					newDoubleAnnos.add(da);
				}
			}
			annos.setDoubleAnnos(newDoubleAnnos);
		}
		
		List<LongAnnotation> longAnnos = annos.getLongAnnos();
		if (longAnnos!=null) {
			List<LongAnnotation> newLongAnnos = new ArrayList<LongAnnotation>();
			for (LongAnnotation la : longAnnos) {
				if (!la.getIsPrivate()) {
					newLongAnnos.add(la);
				}
			}
			annos.setLongAnnos(newLongAnnos);
		}
		
		return annos;
	}

	@WriteTransaction
	@Override
	public void processUserCancelRequest(UserInfo userInfo, String submissionId) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(submissionId, "submissionId");
		if (!submissionDAO.getCreatedBy(submissionId).equals(userInfo.getId().toString())) {
			throw new UnauthorizedException(ONLY_SUBMITTER_REASON);
		}
		SubmissionStatus status = submissionStatusDAO.get(submissionId);
		if (status.getCanCancel() == null || !status.getCanCancel()) {
			throw new UnauthorizedException(NON_CANCELLABLE_REASON);
		}
		status.setCancelRequested(Boolean.TRUE);
		submissionStatusDAO.update(Arrays.asList(status));
		String evalId = submissionDAO.get(submissionId).getEvaluationId();
		evaluationSubmissionsDAO.updateEtagForEvaluation(KeyFactory.stringToKey(evalId), true, ChangeType.UPDATE);
	}
	
	private void sendSubmissionMessage(UserInfo userInfo, ChangeType changeType, String submissionId) {
		
		MessageToSend message = new MessageToSend()
			.withObjectType(ObjectType.SUBMISSION)
			.withObjectId(submissionId)
			.withChangeType(changeType)
			.withUserId(userInfo.getId());
		
		transactionalMessenger.sendMessageAfterCommit(message);
	}
}
