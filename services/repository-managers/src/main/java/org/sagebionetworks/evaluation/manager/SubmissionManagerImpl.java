package org.sagebionetworks.evaluation.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionFileHandleDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	private ParticipantManager participantManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private SubmissionEligibilityManager submissionEligibilityManager;
	
	private static final int MAX_BATCH_SIZE = 500;
	
	@Override
	public Submission getSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(submissionId, "Submission ID");
		Submission sub = submissionDAO.get(submissionId);
		validateEvaluationAccess(userInfo, sub.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		return sub;
	}

	@Override
	public SubmissionStatus getSubmissionStatus(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(submissionId, "Submission ID");
		Submission sub = submissionDAO.get(submissionId);
		validateEvaluationAccess(userInfo, sub.getEvaluationId(), ACCESS_TYPE.READ);
		// only authorized users can view private Annotations 
		boolean includePrivateAnnos = evaluationPermissionsManager.hasAccess(
				userInfo, sub.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION).getAuthorized();
		return submissionToSubmissionStatus(sub, includePrivateAnnos);
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(UserInfo userInfo, Submission submission, String entityEtag, String submissionEligibilityHash, EntityBundle bundle)
			throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		EvaluationUtils.ensureNotNull(submission, "Submission");
		EvaluationUtils.ensureNotNull(bundle, "EntityBundle");
		String evalId = submission.getEvaluationId();
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getId().toString();
		
		submission.setUserId(principalId);
		
		// validate permissions
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT));
		
		// validate eTag
		String entityId = submission.getEntityId();
		Node node = nodeManager.get(userInfo, entityId);
		if (!node.getETag().equals(entityEtag)) {
			// invalid eTag; reject the Submission
			throw new IllegalArgumentException("The supplied eTag is out of date. " +
					"Please fetch Entity " + entityId + " again.");
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
		
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				checkSubmissionEligibility(userInfo, submission, submissionEligibilityHash, now));
		
		// if no name is provided, use the Entity name
		if (submission.getName() == null) {
			submission.setName(node.getName());
		}
		
		// insert EntityBundle JSON
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		bundle.writeToJSONObject(joa);
		submission.setEntityBundleJSON(joa.toJSONString());
		
		// always generate a unique ID
		submission.setId(idGenerator.generateNewId().toString());
				
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
		
		// return the Submission
		return submissionDAO.get(submissionId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo, SubmissionStatus submissionStatus) throws NotFoundException {
		EvaluationUtils.ensureNotNull(submissionStatus, "SubmissionStatus");
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
		
		return submissionStatusDAO.get(submissionStatus.getId());
	}
	
	private static void validateContent(SubmissionStatus submissionStatus, String evalId) {
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
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public BatchUploadResponse updateSubmissionStatusBatch(UserInfo userInfo, String evalId,
			SubmissionStatusBatch batch) throws NotFoundException, ConflictingUpdateException {
		
		UserInfo.validateUserInfo(userInfo);
		
		// validate content of batch
		EvaluationUtils.ensureNotNull(batch.getIsFirstBatch(), "isFirstBatch");
		EvaluationUtils.ensureNotNull(batch.getIsLastBatch(), "isLastBatch");
		EvaluationUtils.ensureNotNull(batch.getStatuses(), "statuses");
		EvaluationUtils.ensureNotEmpty(batch.getStatuses(), "statuses");
		if (batch.getStatuses().size()>MAX_BATCH_SIZE) 
			throw new IllegalArgumentException("Batch size cannot exceed "+MAX_BATCH_SIZE);
		for (SubmissionStatus submissionStatus : batch.getStatuses()) {
			EvaluationUtils.ensureNotNull(submissionStatus, "SubmissionStatus");
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
			EvaluationUtils.ensureNotNull(batchToken, "batchToken");
			if (!batchToken.equals(evalSubs.getEtag()))
				throw new ConflictingUpdateException("Your batch token is out of date.  You must restart upload from first batch.");
		}

		// update the Submissions
		submissionStatusDAO.update(batch.getStatuses());
		
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

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		
		Submission sub = submissionDAO.get(submissionId);		
		String evalId = sub.getEvaluationId();
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.DELETE_SUBMISSION);
		
		// the associated SubmissionStatus object will be deleted via cascade
		Long evalIdLong = KeyFactory.stringToKey(evalId);
		evaluationSubmissionsDAO.updateEtagForEvaluation(evalIdLong, true, ChangeType.UPDATE);
		submissionDAO.delete(submissionId);
	}

	@Override
	public QueryResults<Submission> getAllSubmissions(UserInfo userInfo, String evalId, SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		
		return getAllSubmissionsPrivate(evalId, status, limit, offset);
	}

	private QueryResults<Submission> getAllSubmissionsPrivate(String evalId,
			SubmissionStatusEnum status, long limit, long offset)
			throws DatastoreException, NotFoundException {		
		List<Submission> submissions;
		long totalNumberOfResults;
		if (status == null)	{
			submissions = submissionDAO.getAllByEvaluation(evalId, limit, offset);
			totalNumberOfResults = submissionDAO.getCountByEvaluation(evalId);
		} else {
			submissions = submissionDAO.getAllByEvaluationAndStatus(evalId, status, limit, offset);
			totalNumberOfResults = submissionDAO.getCountByEvaluationAndStatus(evalId, status);
		}
		QueryResults<Submission> res = new QueryResults<Submission>(submissions, totalNumberOfResults);
		return res;
	}
	
	@Override
	public QueryResults<SubmissionStatus> getAllSubmissionStatuses(UserInfo userInfo, String evalId, 
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ);
		// only authorized users can view private Annotations
		boolean includePrivateAnnos = evaluationPermissionsManager.hasAccess(
				userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).getAuthorized();
		QueryResults<Submission> submissions = 
				getAllSubmissionsPrivate(evalId, status, limit, offset);
		return submissionsToSubmissionStatuses(submissions, includePrivateAnnos);
	}
	
	@Override
	public QueryResults<SubmissionBundle> getAllSubmissionBundles(UserInfo userInfo, String evalId, 
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		QueryResults<Submission> submissions = getAllSubmissions(userInfo, evalId, status, limit, offset);
		return submissionsToSubmissionBundles(submissions, true);
	}
	
	@Override
	public QueryResults<Submission> getMyOwnSubmissionsByEvaluation(UserInfo userInfo,
			String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		String principalId = userInfo.getId().toString();
		List<Submission> submissions = submissionDAO.getAllByEvaluationAndUser(evalId, principalId, limit, offset);
		long totalNumberOfResults = submissionDAO.getCountByEvaluationAndUser(evalId, principalId);
		QueryResults<Submission> res = new QueryResults<Submission>(submissions, totalNumberOfResults);
		return res;	
	}
	
	@Override
	public QueryResults<SubmissionBundle> getMyOwnSubmissionBundlesByEvaluation(
			UserInfo userInfo, String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		QueryResults<Submission> submissions = getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset);
		boolean haveReadPrivateAccess = evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION).getAuthorized();
		return submissionsToSubmissionBundles(submissions, haveReadPrivateAccess);
	}
		
	@Override
	public long getSubmissionCount(UserInfo userInfo, String evalId) 
			throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(userInfo, "UserInfo");
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
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
		// generate the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
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
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(evaluationPermissionsManager.hasAccess(userInfo, evalId, accessType));
	}

	/**
	 * Get the bundled Submissions + SubmissionStatuses for a collection of Submissions.
	 * 
	 * @param submissions
	 * @param includePrivateAnnos
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	protected QueryResults<SubmissionBundle> submissionsToSubmissionBundles(
			QueryResults<Submission> submissions, boolean includePrivateAnnos)
			throws DatastoreException, NotFoundException {
		List<SubmissionBundle> bundles = new ArrayList<SubmissionBundle>(submissions.getResults().size());
		for (Submission sub : submissions.getResults()) {
			SubmissionBundle bun = new SubmissionBundle();
			bun.setSubmission(sub);
			bun.setSubmissionStatus(submissionToSubmissionStatus(sub, includePrivateAnnos));
			bundles.add(bun);
		}
		return new QueryResults<SubmissionBundle>(bundles, submissions.getTotalNumberOfResults());
	}
	
	/**
	 * Get the SubmissionStatuses for a collection of Submissions.
	 * 
	 * @param submissions
	 * @param includePrivateAnnos
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	protected QueryResults<SubmissionStatus> submissionsToSubmissionStatuses(
			QueryResults<Submission> submissions, boolean includePrivateAnnos)
			throws DatastoreException, NotFoundException {
		List<SubmissionStatus> statuses = new ArrayList<SubmissionStatus>(submissions.getResults().size());
		for (Submission sub : submissions.getResults()) {
			statuses.add(submissionToSubmissionStatus(sub, includePrivateAnnos));
		}
		return new QueryResults<SubmissionStatus>(statuses, submissions.getTotalNumberOfResults());
	}

	/**
	 * Fetch the SubmissionStatus object for a given Submission. 
	 * 
	 * @param sub
	 * @param includePrivateAnnos
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	protected SubmissionStatus submissionToSubmissionStatus(Submission sub, boolean includePrivateAnnos)
			throws DatastoreException, NotFoundException {
		SubmissionStatus status = submissionStatusDAO.get(sub.getId());
		status.setEntityId(sub.getEntityId());
		status.setVersionNumber(sub.getVersionNumber());
		if (!includePrivateAnnos) {
			Annotations annos = status.getAnnotations();
			if (annos != null) {
				status.setAnnotations(removePrivateAnnos(annos));
			}
		}
		return status;
	}

	/**
	 * Remove all Annotations with [isPrivate == true] from an Annotations object
	 * 
	 * @param annos
	 * @return
	 */
	protected static Annotations removePrivateAnnos(Annotations annos) {
		EvaluationUtils.ensureNotNull(annos, "Annotations");

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
}
