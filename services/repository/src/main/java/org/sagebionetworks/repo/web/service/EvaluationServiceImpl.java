package org.sagebionetworks.repo.web.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.manager.SubmissionManager;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.QueryDAO;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.QueryTranslator;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.query.QueryStatement;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

public class EvaluationServiceImpl implements EvaluationService {
	@Autowired
	private ServiceProvider serviceProvider;
	@Autowired
	private EvaluationManager evaluationManager;
	@Autowired
	private SubmissionManager submissionManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private QueryDAO queryDAO;
	@Autowired
	private NotificationManager notificationManager;
	
	public EvaluationServiceImpl() {}
	
	// for testing
	public EvaluationServiceImpl(
			ServiceProvider serviceProvider,
			EvaluationManager evaluationManager,
			SubmissionManager submissionManager,
			EvaluationPermissionsManager evaluationPermissionsManager,
			UserManager userManager,
			QueryDAO queryDAO,
			NotificationManager notificationManager
			) {
		this.serviceProvider = serviceProvider;
		this.evaluationManager = evaluationManager;
		this.submissionManager = submissionManager;
		this.evaluationPermissionsManager = evaluationPermissionsManager;
		this.userManager = userManager;
		this.queryDAO = queryDAO;
		this.notificationManager = notificationManager;	
	}
	

	@Override
	@WriteTransaction
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
		List<Evaluation> res = evaluationManager.getEvaluationByContentSource(userInfo, id, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
	}

	@Override
	@Deprecated
	public PaginatedResults<Evaluation> getEvaluationsInRange(Long userId, long limit, long offset) 
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<Evaluation> res = evaluationManager.getInRange(userInfo, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
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
		List<Evaluation> res = evaluationManager.getAvailableInRange(userInfo, limit, offset, evaluationIds);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
	}

	@Override
	public Evaluation findEvaluation(Long userId, String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.findEvaluation(userInfo, name);
	}

	@Override
	@WriteTransaction
	public Evaluation updateEvaluation(Long userId, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException,
			InvalidModelException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.updateEvaluation(userInfo, eval);
	}

	@Override
	@WriteTransaction
	public void deleteEvaluation(Long userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		evaluationManager.deleteEvaluation(userInfo, id);
	}

	@Override
	@WriteTransaction
	public Submission createSubmission(Long userId, Submission submission, String entityEtag, 
			String submissionEligibilityHash, HttpServletRequest request, String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException, JSONObjectAdapterException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		
		// fetch EntityBundle to be serialized
		int mask = ServiceConstants.DEFAULT_ENTITYBUNDLE_MASK_FOR_SUBMISSIONS;
		String entityId = submission.getEntityId();
		Long versionNumber = submission.getVersionNumber();
		EntityBundle bundle = serviceProvider.getEntityBundleService().getEntityBundle(userId, entityId, versionNumber, mask, request);
		Submission created = submissionManager.createSubmission(userInfo, submission, entityEtag, submissionEligibilityHash, bundle);
		List<MessageToUserAndBody> messages = submissionManager.
				createSubmissionNotifications(userInfo,created,submissionEligibilityHash,
						challengeEndpoint, notificationUnsubscribeEndpoint);
		notificationManager.sendNotifications(userInfo, messages);
		return created;
	}
	
	/**
	 * 
	 * @param userId
	 * @param submissionId
	 * @param submissionContributor
	 * @return
	 * @throws NotFoundException 
	 */
	public SubmissionContributor addSubmissionContributor(Long userId, String submissionId, SubmissionContributor submissionContributor) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.addSubmissionContributor(userInfo, submissionId, submissionContributor);
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
	@WriteTransaction
	public SubmissionStatus updateSubmissionStatus(Long userId,
			SubmissionStatus submissionStatus) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.updateSubmissionStatus(userInfo, submissionStatus);
	}

	@Override
	@WriteTransaction
	public BatchUploadResponse updateSubmissionStatusBatch(Long userId, String evalId,
			SubmissionStatusBatch batch) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return submissionManager.updateSubmissionStatusBatch(userInfo, evalId, batch);
	}

	@Override
	@Deprecated
	@WriteTransaction
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
		List<Submission> res = submissionManager.getAllSubmissions(userInfo, evalId, status, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
	}
	
	@Override
	public PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(Long userId, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		List<SubmissionStatus> res = submissionManager.getAllSubmissionStatuses(userInfo, evalId, status, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
	}
	
	@Override
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(Long userId, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		List<SubmissionBundle> res = submissionManager.getAllSubmissionBundles(userInfo, evalId, status, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
	}
	
	@Override
	public PaginatedResults<Submission> getMyOwnSubmissionsByEvaluation(
			String evalId, Long userId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		List<Submission> res = submissionManager.getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
	}
	
	@Override
	public PaginatedResults<SubmissionBundle> getMyOwnSubmissionBundlesByEvaluation(
			String evalId, Long userId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		List<SubmissionBundle> res = submissionManager.getMyOwnSubmissionBundlesByEvaluation(userInfo, evalId, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(res, limit, offset);
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
		return evaluationPermissionsManager.hasAccess(userInfo, id, ACCESS_TYPE.valueOf(accessType)).isAuthorized();
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

	public TeamSubmissionEligibility getTeamSubmissionEligibility(Long userId, String evalId, String teamId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return evaluationManager.getTeamSubmissionEligibility(userInfo, evalId, teamId);
	}

	@Override
	public void processCancelSubmissionRequest(Long userId, String subId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		submissionManager.processUserCancelRequest(userInfo, subId);
	}

}
