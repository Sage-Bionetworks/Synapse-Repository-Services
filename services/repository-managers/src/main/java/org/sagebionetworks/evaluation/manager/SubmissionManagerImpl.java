package org.sagebionetworks.evaluation.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.dao.SubmissionDAO;
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
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityWithAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
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
	EvaluationManager evaluationManager;
	@Autowired
	ParticipantManager participantManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	NodeManager nodeManager;
	
	public SubmissionManagerImpl() {};
	
	// for testing purposes
	protected SubmissionManagerImpl(IdGenerator idGenerator, SubmissionDAO submissionDAO, 
			SubmissionStatusDAO submissionStatusDAO, EvaluationManager evaluationManager,
			ParticipantManager participantManager, EntityManager entityManager,
			NodeManager nodeManager) {
		this.idGenerator = idGenerator;
		this.submissionDAO = submissionDAO;
		this.submissionStatusDAO = submissionStatusDAO;
		this.evaluationManager = evaluationManager;
		this.participantManager = participantManager;
		this.entityManager = entityManager;
		this.nodeManager = nodeManager;
	}

	@Override
	public Submission getSubmission(String submissionId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(submissionId, "Submission ID");
		return submissionDAO.get(submissionId);
	}

	@Override
	public SubmissionStatus getSubmissionStatus(String submissionId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(submissionId, "Submission ID");
		return submissionStatusDAO.get(submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(UserInfo userInfo, Submission submission) throws NotFoundException {
		EvaluationUtils.ensureNotNull(submission, "Submission");
		String evalId = submission.getEvaluationId();
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		submission.setUserId(principalId);
		
		// ensure participant exists
		try {
			participantManager.getParticipant(principalId, evalId);
		} catch (NotFoundException e) {
			throw new ForbiddenException("User Princpal ID: " + principalId + 
					" has not joined Evaluation ID: " + evalId);
		}
		
		// fetch entity and annotations
		Node node = nodeManager.getNodeForVersionNumber(
				userInfo, submission.getEntityId(), submission.getVersionNumber());	
		Class<? extends Entity> clazz = EntityType.valueOf(node.getNodeType()).getClassForType();
		EntityWithAnnotations<? extends Entity> ewa = entityManager.getEntityWithAnnotations(
				userInfo, submission.getEntityId(), clazz);
		submission.setFileHandleId(node.getFileHandleId());
		
		// if no name is provided, use the Entity name
		if (submission.getName() == null) {
			submission.setName(node.getName());
		}
		
		// ensure evaluation is open
		Evaluation eval = evaluationManager.getEvaluation(evalId);
		EvaluationUtils.ensureEvaluationIsOpen(eval);
		
		// always generate a unique ID
		submission.setId(idGenerator.generateNewId().toString());
				
		// Set creation date
		submission.setCreatedOn(new Date());
		
		// create the Submission	
		String id = submissionDAO.create(submission, ewa);
		
		// create an accompanying SubmissionStatus object
		SubmissionStatus status = new SubmissionStatus();
		status.setId(id);
		status.setStatus(SubmissionStatusEnum.OPEN);
		status.setModifiedOn(new Date());
		submissionStatusDAO.create(status);
		
		// return the Submission
		return submissionDAO.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo, SubmissionStatus submissionStatus) throws NotFoundException {
		EvaluationUtils.ensureNotNull(submissionStatus, "SubmissionStatus");
		UserInfo.validateUserInfo(userInfo);
		
		// ensure Submission exists and validate admin rights
		SubmissionStatus old = getSubmissionStatus(submissionStatus.getId());
		String evalId = getSubmission(submissionStatus.getId()).getEvaluationId();
		if (!evaluationManager.isEvalAdmin(userInfo, evalId))
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
		if (!evaluationManager.isEvalAdmin(userInfo, evalId)) {
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
		
		if (!evaluationManager.isEvalAdmin(userInfo, evalId))
			throw new UnauthorizedException("User Principal ID" + principalId + " is not authorized to adminster Evaluation " + evalId);
		
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

}
