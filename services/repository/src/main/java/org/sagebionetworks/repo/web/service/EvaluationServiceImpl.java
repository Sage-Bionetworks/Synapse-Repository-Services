package org.sagebionetworks.repo.web.service;

import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.manager.ParticipantManager;
import org.sagebionetworks.evaluation.manager.SubmissionManager;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.QueryDAO;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.util.QueryTranslator;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.query.QueryStatement;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EvaluationServiceImpl implements EvaluationService {

	@Autowired
	private ServiceProvider serviceProvider;
	@Autowired
	private EvaluationManager evaluationManager;
	@Autowired
	private ParticipantManager participantManager;
	@Autowired
	private SubmissionManager submissionManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private QueryDAO queryDAO;

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation createEvaluation(Long userId, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.createEvaluation(userInfo, eval);
	}
	
	@Override
	public Evaluation getEvaluation(Long userId, String id) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.getEvaluation(userInfo, id);
	}
	
	@Override
	public PaginatedResults<Evaluation> getEvaluationByContentSource(Long userId, String id, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<Evaluation> res = evaluationManager.getEvaluationByContentSource(userInfo, id, limit, offset);
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
	@Deprecated
	public PaginatedResults<Evaluation> getEvaluationsInRange(Long userId, long limit, long offset, HttpServletRequest request) 
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<Evaluation> res = evaluationManager.getInRange(userInfo, limit, offset);
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
	
	/**
	 * Get a collection of Evaluations to which the user may SUBMIT, within a given range
	 *
	 * @param userId the userId (email address) of the user making the request
	 * @param limit
	 * @param offset
	 * @param evaluationIds
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Override
	public PaginatedResults<Evaluation> getAvailableEvaluationsInRange(
			Long userId, long limit, long offset, List<Long> evaluationIds, HttpServletRequest request) 
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<Evaluation> res = evaluationManager.getAvailableInRange(userInfo, limit, offset, evaluationIds);
		return new PaginatedResults<Evaluation>(
				request.getServletPath() + UrlHelpers.EVALUATION_AVAILABLE,
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false				
			);
	}

	@Override
	@Deprecated
	public long getEvaluationCount(Long userId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.getCount(userInfo);
	}

	@Override
	public Evaluation findEvaluation(Long userId, String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.findEvaluation(userInfo, name);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation updateEvaluation(Long userId, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException,
			InvalidModelException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.updateEvaluation(userInfo, eval);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteEvaluation(Long userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		evaluationManager.deleteEvaluation(userInfo, id);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(Long userId, String evalId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantManager.addParticipant(userInfo, evalId);
	}

	@Override
	public Participant getParticipant(Long userId, String principalId, String evalId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantManager.getParticipant(userInfo, principalId, evalId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(Long userId, String evalId,
			String idToRemove) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		participantManager.removeParticipant(userInfo, evalId, idToRemove);
	}

	@Override
	public PaginatedResults<Participant> getAllParticipants(Long userId, String evalId, long limit, long offset, HttpServletRequest request)
			throws NumberFormatException, DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<Participant> res = participantManager.getAllParticipants(userInfo, evalId, limit, offset);
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
	public long getParticipantCount(Long userId, String evalId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantManager.getNumberofParticipants(userInfo, evalId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(Long userId, Submission submission, String entityEtag, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException, JSONObjectAdapterException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		
		// fetch EntityBundle to be serialized
		int mask = ServiceConstants.DEFAULT_ENTITYBUNDLE_MASK_FOR_SUBMISSIONS;
		String entityId = submission.getEntityId();
		Long versionNumber = submission.getVersionNumber();
		EntityBundle bundle = serviceProvider.getEntityBundleService().getEntityBundle(userId, entityId, versionNumber, mask, request);
		return submissionManager.createSubmission(userInfo, submission, entityEtag, bundle);
	}

	@Override
	public Submission getSubmission(Long userId, String submissionId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.getSubmission(userInfo, submissionId);
	}

	@Override
	public SubmissionStatus getSubmissionStatus(Long userId, String submissionId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.getSubmissionStatus(userInfo, submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(Long userId,
			SubmissionStatus submissionStatus) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.updateSubmissionStatus(userInfo, submissionStatus);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public BatchUploadResponse updateSubmissionStatusBatch(Long userId, String evalId,
			SubmissionStatusBatch batch) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.updateSubmissionStatusBatch(userInfo, evalId, batch);
	}

	@Override
	@Deprecated
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(Long userId, String submissionId)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		submissionManager.deleteSubmission(userInfo, submissionId);
	}

	@Override
	public PaginatedResults<Submission> getAllSubmissions(Long userId, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
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
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(Long userId, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<SubmissionStatus> res = submissionManager.getAllSubmissionStatuses(userInfo, evalId, status, limit, offset);
		return new PaginatedResults<SubmissionStatus>(
				request.getServletPath() + makeEvalIdUrl(UrlHelpers.SUBMISSION_STATUS_WITH_EVAL_ID, evalId),
				res.getResults(),
				res.getTotalNumberOfResults(),
				offset,
				limit,
				"",
				false			
			);
	}
	
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(Long userId, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
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
	public PaginatedResults<Submission> getMyOwnSubmissionsByEvaluation(
			String evalId, Long userId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<Submission> res = submissionManager.getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset);
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
	public PaginatedResults<SubmissionBundle> getMyOwnSubmissionBundlesByEvaluation(
			String evalId, Long userId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		QueryResults<SubmissionBundle> res = submissionManager.getMyOwnSubmissionBundlesByEvaluation(userInfo, evalId, limit, offset);
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
	public String getRedirectURLForFileHandle(Long userId,
			String submissionId, String fileHandleId) 
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.getRedirectURLForFileHandle(userInfo, submissionId, fileHandleId);
	}

	@Override
	public long getSubmissionCount(Long userId, String evalId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.getSubmissionCount(userInfo, evalId);
	}

	@Override
	@Deprecated
	public <T extends Entity> boolean hasAccess(String id, Long userId,
			HttpServletRequest request, String accessType)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationPermissionsManager.hasAccess(userInfo, id, ACCESS_TYPE.valueOf(accessType)).getAuthorized();
	}

	@Override
	public AccessControlList createAcl(Long userId, AccessControlList acl)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationPermissionsManager.createAcl(userInfo, acl);
	}

	@Override
	public AccessControlList updateAcl(Long userId, AccessControlList acl)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationPermissionsManager.updateAcl(userInfo, acl);
	}

	@Override
	public void deleteAcl(Long userId, String evalId)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		evaluationPermissionsManager.deleteAcl(userInfo, evalId);
	}

	@Override
	public AccessControlList getAcl(Long userId, String evalId)
			throws NotFoundException, DatastoreException,
			ACLInheritanceException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationPermissionsManager.getAcl(userInfo, evalId);
	}

	@Override
	public UserEvaluationPermissions getUserPermissionsForEvaluation(
			Long userId, String evalId) throws NotFoundException,
			DatastoreException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationPermissionsManager.getUserPermissionsForEvaluation(userInfo, evalId);
	}
	
	@Override
	public QueryTableResults query(String userQuery, Long userId) 
			throws DatastoreException, NotFoundException, JSONObjectAdapterException,
			ParseException {
		// Parse and validate the query
		QueryStatement stmt = new QueryStatement(userQuery);
		// Convert from a query statement to a basic query
		BasicQuery basicQuery = QueryTranslator.createBasicQuery(stmt);
		UserInfo userInfo = userManager.getUserInfo(userId);
		return queryDAO.executeQuery(basicQuery, userInfo);
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
