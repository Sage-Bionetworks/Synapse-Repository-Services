package org.sagebionetworks.evaluation.manager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.file.FileHandle;
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
	private EvaluationManager evaluationManager;
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
		// only authorized users can view private Annotations
		boolean includePrivateAnnos = evaluationPermissionsManager.hasAccess(
				userInfo, sub.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		return submissionToSubmissionStatus(sub, includePrivateAnnos);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(UserInfo userInfo, Submission submission, String entityEtag, EntityBundle bundle)
			throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		EvaluationUtils.ensureNotNull(submission, "Submission");
		EvaluationUtils.ensureNotNull(bundle, "EntityBundle");
		String evalId = submission.getEvaluationId();
		Evaluation eval = evaluationManager.getEvaluation(userInfo, evalId);
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getId().toString();
		
		submission.setUserId(principalId);
		
		// validate permissions
		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.SUBMIT)) {
			throw new UnauthorizedException("Not allowed to submit to " + eval.getName());
		}
		
		// validate eTag
		String entityId = submission.getEntityId();
		Node node = nodeManager.get(userInfo, entityId);
		if (!node.getETag().equals(entityEtag)) {
			// invalid eTag; reject the Submission
			throw new IllegalArgumentException("The supplied eTag is out of date. " +
					"Please fetch Entity " + entityId + " again.");
		} 
		
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
		submission.setCreatedOn(new Date());
		
		// create the Submission	
		String submissionId = submissionDAO.create(submission);
		
		// create an accompanying SubmissionStatus object
		SubmissionStatus status = new SubmissionStatus();
		status.setId(submissionId);
		status.setStatus(SubmissionStatusEnum.RECEIVED);
		status.setModifiedOn(new Date());
		submissionStatusDAO.create(status);
		
		// save FileHandle IDs
		for (FileHandle handle : bundle.getFileHandles()) {
			submissionFileHandleDAO.create(submissionId, handle.getId());
		}
		
		// return the Submission
		return submissionDAO.get(submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo, SubmissionStatus submissionStatus) throws NotFoundException {
		EvaluationUtils.ensureNotNull(submissionStatus, "SubmissionStatus");
		UserInfo.validateUserInfo(userInfo);
		
		// ensure Submission exists and validate access rights
		SubmissionStatus old = getSubmissionStatus(userInfo, submissionStatus.getId());
		String evalId = getSubmission(userInfo, submissionStatus.getId()).getEvaluationId();
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.UPDATE_SUBMISSION);
		
		if (!old.getEtag().equals(submissionStatus.getEtag()))
			throw new IllegalArgumentException("Your copy of SubmissionStatus " + 
					submissionStatus.getId() + " is out of date. Please fetch it again before updating.");
		
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
			annos.setObjectId(submissionStatus.getId());
			annos.setScopeId(evalId);
		}
		
		// update and return the new Submission
		submissionStatusDAO.update(submissionStatus);
		return submissionStatusDAO.get(submissionStatus.getId());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		
		Submission sub = submissionDAO.get(submissionId);		
		String evalId = sub.getEvaluationId();
		validateEvaluationAccess(userInfo, evalId, ACCESS_TYPE.DELETE_SUBMISSION);
		
		// the associated SubmissionStatus object will be deleted via cascade
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
		// only authorized users can view private Annotations
		boolean includePrivateAnnos = evaluationPermissionsManager.hasAccess(
				userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		QueryResults<Submission> submissions = getAllSubmissions(userInfo, evalId, status, limit, offset);
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
		boolean haveReadPrivateAccess = evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
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
	public URL getRedirectURLForFileHandle(UserInfo userInfo, 
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
		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, accessType)) {
			throw new UnauthorizedException("You lack " + accessType + " rights for Evaluation " + evalId);
		}
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
	protected Annotations removePrivateAnnos(Annotations annos) {
		EvaluationUtils.ensureNotNull(annos, "Annotations");

		List<StringAnnotation> newStringAnnos = new ArrayList<StringAnnotation>();
		List<StringAnnotation> oldStringAnnos = annos.getStringAnnos();
		for (StringAnnotation sa : oldStringAnnos) {
			if (!sa.getIsPrivate()) {
				newStringAnnos.add(sa);
			}
		}
		annos.setStringAnnos(newStringAnnos);
		
		List<DoubleAnnotation> newDoubleAnnos = new ArrayList<DoubleAnnotation>();
		List<DoubleAnnotation> oldDoubleAnnos = annos.getDoubleAnnos();
		for (DoubleAnnotation da : oldDoubleAnnos) {
			if (!da.getIsPrivate()) {
				newDoubleAnnos.add(da);
			}
		}
		annos.setDoubleAnnos(newDoubleAnnos);
		
		List<LongAnnotation> newLongAnnos = new ArrayList<LongAnnotation>();
		List<LongAnnotation> longAnnos = annos.getLongAnnos();
		for (LongAnnotation la : longAnnos) {
			if (!la.getIsPrivate()) {
				newLongAnnos.add(la);
			}
		}
		annos.setLongAnnos(newLongAnnos);
		
		return annos;
	}

}
