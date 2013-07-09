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
import org.sagebionetworks.repo.manager.AuthorizationManager;
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
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
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
	SubmissionDAO submissionDAO;
	@Autowired
	SubmissionStatusDAO submissionStatusDAO;
	@Autowired
	SubmissionFileHandleDAO submissionFileHandleDAO;
	@Autowired
	EvaluationManager evaluationManager;
	@Autowired
	ParticipantManager participantManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	NodeManager nodeManager;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	FileHandleManager fileHandleManager;
	
	public SubmissionManagerImpl() {};
	
	// for testing purposes
	protected SubmissionManagerImpl(IdGenerator idGenerator, SubmissionDAO submissionDAO, 
			SubmissionStatusDAO submissionStatusDAO, SubmissionFileHandleDAO submissionFileHandleDAO,
			EvaluationManager evaluationManager, ParticipantManager participantManager,
			EntityManager entityManager, NodeManager nodeManager,
			AuthorizationManager authorizationManager, FileHandleManager fileHandleManager) {
		this.idGenerator = idGenerator;
		this.submissionDAO = submissionDAO;
		this.submissionStatusDAO = submissionStatusDAO;
		this.submissionFileHandleDAO = submissionFileHandleDAO;
		this.evaluationManager = evaluationManager;
		this.participantManager = participantManager;
		this.entityManager = entityManager;
		this.nodeManager = nodeManager;
		this.authorizationManager = authorizationManager;
		this.fileHandleManager = fileHandleManager;
	}

	@Override
	public Submission getSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(submissionId, "Submission ID");
		Submission sub = submissionDAO.get(submissionId);
		boolean isSubmissionOwner = userInfo.getIndividualGroup().getId().equals(sub.getUserId());
		boolean isEvaluationAdmin = authorizationManager.canAccess(
				userInfo, sub.getEvaluationId(), ObjectType.EVALUATION, ACCESS_TYPE.UPDATE);
		if (isSubmissionOwner || isEvaluationAdmin) {
			return sub;
		} else {
			throw new UnauthorizedException("User " + userInfo.getUser().getId() +
					" is not authorized to view Submission " + submissionId);
		}
	}

	@Override
	public SubmissionStatus getSubmissionStatus(String submissionId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(submissionId, "Submission ID");
		return submissionStatusDAO.get(submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(UserInfo userInfo, Submission submission, String entityEtag, EntityBundle bundle)
			throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		EvaluationUtils.ensureNotNull(submission, "Submission");
		EvaluationUtils.ensureNotNull(bundle, "EntityBundle");
		String evalId = submission.getEvaluationId();
		Evaluation eval = evaluationManager.getEvaluation(evalId);
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		submission.setUserId(principalId);
		
		// ensure participant exists
		try {
			participantManager.getParticipant(principalId, evalId);
		} catch (NotFoundException e) {
			throw new UnauthorizedException("User Princpal ID: " + principalId + 
					" has not joined Evaluation ID: " + evalId);
		}
		
		// 'canParticipate' is checked before someone is allowed to join,
		// but just in-case authorization changes between the time she joins
		// and the time she submits, we check authorization again:
		boolean canParticipate = authorizationManager.canAccess(userInfo, evalId, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE);
		if (!canParticipate) {
			throw new UnauthorizedException("Not allowed to participate in "+eval.getName());
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
		
		// ensure evaluation is open
		EvaluationUtils.ensureEvaluationIsOpen(eval);
		
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
		status.setStatus(SubmissionStatusEnum.OPEN);
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
		
		// ensure Submission exists and validate admin rights
		SubmissionStatus old = getSubmissionStatus(submissionStatus.getId());
		String evalId = getSubmission(userInfo, submissionStatus.getId()).getEvaluationId();
		if (!authorizationManager.canAccess(
				userInfo, evalId, ObjectType.EVALUATION, ACCESS_TYPE.UPDATE))
			throw new UnauthorizedException("Not authorized");
		
		if (!old.getEtag().equals(submissionStatus.getEtag()))
			throw new IllegalArgumentException("Your copy of SubmissionStatus " + 
					submissionStatus.getId() + " is out of date. Please fetch it again before updating.");
		
		// validate score, if any
		Double score = submissionStatus.getScore();
		if (score != null) {
			if (score > 1.0 || score < 0.0)
				throw new IllegalArgumentException("Scores must be between 0 and 1. Received score = " + score);
		}
		
		// update and return the new Submission
		submissionStatusDAO.update(submissionStatus);
		return submissionStatusDAO.get(submissionStatus.getId());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		Submission sub = submissionDAO.get(submissionId);		
		String evalId = sub.getEvaluationId();
		
		// verify access permission
		if (!authorizationManager.canAccess(userInfo, evalId, ObjectType.EVALUATION, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException("User ID: " + principalId +
					" is not authorized to modify Submission ID: " + submissionId);
		}
		
		// the associated SubmissionStatus object will be deleted via cascade
		submissionDAO.delete(submissionId);
	}

	@Override
	public QueryResults<Submission> getAllSubmissions(UserInfo userInfo, String evalId, SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		if (!authorizationManager.canAccess(
				userInfo, evalId, ObjectType.EVALUATION, ACCESS_TYPE.UPDATE))
			throw new UnauthorizedException("User Principal ID" + principalId + " is not authorized to adminster Evaluation " + evalId);
		
		return getAllSubmissions(evalId, status, limit, offset);
	}
	
	private QueryResults<Submission> getAllSubmissions(String evalId, SubmissionStatusEnum status, long limit, long offset)
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
	public QueryResults<SubmissionStatus> getAllSubmissionStatuses(String evalId, SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		// note that this request is publicly-accessible; we do not validate userInfo
		QueryResults<Submission> submissions = getAllSubmissions(evalId, status, limit, offset);
		return submissionsToSubmissionStatuses(submissions);
	}
	
	@Override
	public QueryResults<SubmissionBundle> getAllSubmissionBundles(UserInfo userInfo, String evalId, SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		QueryResults<Submission> submissions = getAllSubmissions(userInfo, evalId, status, limit, offset);
		return submissionsToSubmissionBundles(submissions);
	}
	
	@Override
	public QueryResults<Submission> getAllSubmissionsByUser(String userId, long limit, long offset) throws DatastoreException, NotFoundException {
		List<Submission> submissions = submissionDAO.getAllByUser(userId, limit, offset);
		long totalNumberOfResults = submissionDAO.getCountByUser(userId);
		QueryResults<Submission> res = new QueryResults<Submission>(submissions, totalNumberOfResults);
		return res;
	}
	
	@Override
	public QueryResults<SubmissionBundle> getAllSubmissionBundlesByUser(String userId, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		QueryResults<Submission> submissions = getAllSubmissionsByUser(userId, limit, offset);
		return submissionsToSubmissionBundles(submissions);
	}
	
	@Override
	public QueryResults<Submission> getAllSubmissionsByEvaluationAndUser(UserInfo userInfo, String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		List<Submission> submissions = submissionDAO.getAllByEvaluationAndUser(evalId, principalId, limit, offset);
		long totalNumberOfResults = submissionDAO.getCountByEvaluationAndUser(evalId, principalId);
		QueryResults<Submission> res = new QueryResults<Submission>(submissions, totalNumberOfResults);
		return res;	
	}
	
	@Override
	public QueryResults<SubmissionBundle> getAllSubmissionBundlesByEvaluationAndUser(UserInfo userInfo, String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		QueryResults<Submission> submissions = getAllSubmissionsByEvaluationAndUser(userInfo, evalId, limit, offset);
		return submissionsToSubmissionBundles(submissions);
	}
		
	@Override
	public long getSubmissionCount(String evalId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		return submissionDAO.getCountByEvaluation(evalId);
	}
	
	@Override
	public URL getRedirectURLForFileHandle(UserInfo userInfo, 
			String submissionId, String fileHandleId) 
			throws DatastoreException, NotFoundException {
		Submission submission = getSubmission(userInfo, submissionId);
		
		// validate permissions
		boolean isEvaluationAdmin = authorizationManager.canAccess(userInfo,
				submission.getEvaluationId(), ObjectType.EVALUATION, ACCESS_TYPE.UPDATE); // TODO: change to READ_PRIVATE_SUBMISSION
		if (!isEvaluationAdmin) {
			throw new UnauthorizedException("Insufficient priveliges to " +
					"download files from this Evaluation");
		}
		
		// ensure that the requested ID is included in the Submission
		List<String> ids = submissionFileHandleDAO.getAllBySubmission(submissionId);
		if (!ids.contains(fileHandleId)) {
			throw new NotFoundException("Submission " + submissionId + " does " +
					"not contain the requested FileHandle " + fileHandleId);
		}
		
		// generate the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}
	
	protected QueryResults<SubmissionBundle> submissionsToSubmissionBundles(QueryResults<Submission> submissions) throws DatastoreException, NotFoundException {
		List<SubmissionBundle> bundles = new ArrayList<SubmissionBundle>(submissions.getResults().size());
		for (Submission sub : submissions.getResults()) {
			SubmissionBundle bun = new SubmissionBundle();
			bun.setSubmission(sub);
			bun.setSubmissionStatus(getSubmissionStatus(sub.getId()));
			bundles.add(bun);
		}
		return new QueryResults<SubmissionBundle>(bundles, submissions.getTotalNumberOfResults());
	}
	
	protected QueryResults<SubmissionStatus> submissionsToSubmissionStatuses(QueryResults<Submission> submissions) throws DatastoreException, NotFoundException {
		List<SubmissionStatus> statuses = new ArrayList<SubmissionStatus>(submissions.getResults().size());
		for (Submission sub : submissions.getResults()) {
			statuses.add(getSubmissionStatus(sub.getId()));
		}
		return new QueryResults<SubmissionStatus>(statuses, submissions.getTotalNumberOfResults());
	}

}
