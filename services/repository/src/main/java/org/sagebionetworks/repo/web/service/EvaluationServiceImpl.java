package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.ParticipantManager;
import org.sagebionetworks.evaluation.manager.SubmissionManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EvaluationServiceImpl implements EvaluationService {
	
	@Autowired
	ServiceProvider serviceProvider;
	@Autowired
	EvaluationManager evaluationManager;
	@Autowired
	ParticipantManager participantManager;
	@Autowired
	SubmissionManager submissionManager;
	@Autowired
	UserManager userManager;
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation createEvaluation(String userName, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		return evaluationManager.createEvaluation(userInfo, eval);
	}
	
	@Override
	public Evaluation getEvaluation(String id) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		return evaluationManager.getEvaluation(id);
	}

	@Override
	public PaginatedResults<Evaluation> getEvaluationsInRange(long limit, long offset, HttpServletRequest request) 
			throws DatastoreException, NotFoundException {
		QueryResults<Evaluation> res = evaluationManager.getInRange(limit, offset);
		return new PaginatedResults<Evaluation>(
				request.getServletPath() + UrlHelpers.EVALUATION,
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false				
			);
	}

	@Override
	public long getEvaluationCount() throws DatastoreException, NotFoundException {
		return evaluationManager.getCount();
	}

	@Override
	public Evaluation findEvaluation(String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		return evaluationManager.findEvaluation(name);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation updateEvaluation(String userName, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException,
			InvalidModelException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		return evaluationManager.updateEvaluation(userInfo, eval);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteEvaluation(String userName, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		evaluationManager.deleteEvaluation(userInfo, id);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userName, String evalId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		return participantManager.addParticipant(userInfo, evalId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipantAsAdmin(String userName, String evalId,
			String idToAdd) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		return participantManager.addParticipantAsAdmin(userInfo, evalId, idToAdd);
	}

	@Override
	public Participant getParticipant(String principalId, String evalId)
			throws DatastoreException, NotFoundException {
		return participantManager.getParticipant(principalId, evalId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userName, String evalId,
			String idToRemove) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		participantManager.removeParticipant(userInfo, evalId, idToRemove);
	}

	@Override
	public PaginatedResults<Participant> getAllParticipants(String evalId, long limit, long offset, HttpServletRequest request)
			throws NumberFormatException, DatastoreException, NotFoundException {
		QueryResults<Participant> res = participantManager.getAllParticipants(evalId, limit, offset);
		return new PaginatedResults<Participant>(
				request.getServletPath() + UrlHelpers.PARTICIPANT,
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}

	@Override
	public long getParticipantCount(String evalId)
			throws DatastoreException, NotFoundException {
		return participantManager.getNumberofParticipants(evalId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(String userName, Submission submission, String entityEtag, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException, JSONObjectAdapterException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		
		// fetch EntityBundle to be serialized
		int mask = ServiceConstants.DEFAULT_ENTITYBUNDLE_MASK_FOR_SUBMISSIONS;
		String userId = userInfo.getUser().getId();
		String entityId = submission.getEntityId();
		Long versionNumber = submission.getVersionNumber();
		EntityBundle bundle = serviceProvider.getEntityBundleService().getEntityBundle(userId, entityId, versionNumber, mask, request);
		return submissionManager.createSubmission(userInfo, submission, entityEtag, bundle);
	}

	@Override
	public Submission getSubmission(String userName, String submissionId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		return submissionManager.getSubmission(userInfo, submissionId);
	}

	@Override
	public SubmissionStatus getSubmissionStatus(String submissionId)
			throws DatastoreException, NotFoundException {
		return submissionManager.getSubmissionStatus(submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(String userName,
			SubmissionStatus submissionStatus) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		return submissionManager.updateSubmissionStatus(userInfo, submissionStatus);
	}

	@Override
	@Deprecated
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(String userName, String submissionId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		submissionManager.deleteSubmission(userInfo, submissionId);
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissions(String userName, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		QueryResults<Submission> res = submissionManager.getAllSubmissions(userInfo, evalId, status, limit, offset);
		return new PaginatedResults<Submission>(
				request.getServletPath() + makeEvalIdUrl(UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN, evalId),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}
	
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(String userName, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		QueryResults<SubmissionBundle> res = submissionManager.getAllSubmissionBundles(userInfo, evalId, status, limit, offset);
		return new PaginatedResults<SubmissionBundle>(
				request.getServletPath() + makeEvalIdUrl(UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN_BUNDLE, evalId),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissionsByUser(String princpalId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		QueryResults<Submission> res = submissionManager.getAllSubmissionsByUser(princpalId, limit, offset);
		return new PaginatedResults<Submission>(
				request.getServletPath(),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}
	
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByUser(
			String princpalId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		QueryResults<SubmissionBundle> res = submissionManager.getAllSubmissionBundlesByUser(princpalId, limit, offset);
		return new PaginatedResults<SubmissionBundle>(
				request.getServletPath(),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}
	
	@Override
	public PaginatedResults<Submission> getAllSubmissionsByEvaluationAndUser(
			String evalId, String userName, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		QueryResults<Submission> res = submissionManager.getAllSubmissionsByEvaluationAndUser(userInfo, evalId, limit, offset);
		return new PaginatedResults<Submission>(
				request.getServletPath() + makeEvalIdUrl(UrlHelpers.SUBMISSION_WITH_EVAL_ID, evalId),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}
	
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByEvaluationAndUser(
			String evalId, String userName, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userName);
		QueryResults<SubmissionBundle> res = submissionManager.getAllSubmissionBundlesByEvaluationAndUser(userInfo, evalId, limit, offset);
		return new PaginatedResults<SubmissionBundle>(
				request.getServletPath() + makeEvalIdUrl(UrlHelpers.SUBMISSION_WITH_EVAL_ID_BUNDLE, evalId),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}

	@Override
	public long getSubmissionCount(String evalId) throws DatastoreException,
			NotFoundException {
		return submissionManager.getSubmissionCount(evalId);
	}
	
	/**
	 * Inserts an evaluation ID into a provided URL anywhere the
	 * EVALUATION_ID_PATH_VAR is found.
	 * 
	 * @param evalId
	 * @param url
	 * @return
	 */
	private String makeEvalIdUrl(String evalId, String url) {
		return url.replace(UrlHelpers.EVALUATION_ID_PATH_VAR, evalId);
	}
	
}
