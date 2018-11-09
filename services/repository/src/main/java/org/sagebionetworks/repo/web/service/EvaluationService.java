package org.sagebionetworks.repo.web.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface EvaluationService {

	////// Methods for managing evaluations //////

	/**
	 * Create a new Synapse Evaluation
	 * 
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public Evaluation createEvaluation(Long userId, Evaluation eval)
			throws DatastoreException, InvalidModelException, NotFoundException;
	
	/**
	 * Get a Synapse Evaluation by its id
	 */
	public Evaluation getEvaluation(Long userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Gets all Synapse Evaluations tied to the given Project
	 */
	public PaginatedResults<Evaluation> getEvaluationByContentSource(Long userId, String id, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException;

	/**
	 * Get a collection of Evaluations, within a given range
	 *
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Evaluation> getEvaluationsInRange(Long userId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get a collection of Evaluations in which the user has SUBMIT permission, within a given range
	 *
	 * @param userId the userId (email address) of the user making the request
	 * @param limit
	 * @param offset
	 * @param evaluationIds
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Evaluation> getAvailableEvaluationsInRange(
			Long userId, long limit, long offset, List<Long> evaluationIds, HttpServletRequest request) throws DatastoreException, NotFoundException;

	/**
	 * Find a Evaluation, by name
	 * 
	 * @param name
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Evaluation findEvaluation(Long userId, String name) throws DatastoreException,
			NotFoundException, UnauthorizedException;

	/**
	 * Update a Synapse Evaluation.
	 * 
	 * @param userId
	 * @param eval
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 */
	public Evaluation updateEvaluation(Long userId, Evaluation eval)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, InvalidModelException,
			ConflictingUpdateException;

	/**
	 * Delete a Synapse Evaluation.
	 * 
	 * @param userId
	 * @param evalId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public void deleteEvaluation(Long userId, String evalId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	////// Methods for managing submissions //////

	/**
	 * Create a Submission.
	 * 
	 * @param userId
	 * @param submission
	 * @param entityEtag
	 * @param submissionEligibilityHash
	 * @param challengeEndpoint
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 * @throws JSONObjectAdapterException
	 */
	public Submission createSubmission(Long userId, Submission submission, String entityEtag, 
			String submissionEligibilityHash, HttpServletRequest request, String challengeEndpoint, String notificationUnsubscribeEndpoint)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException, JSONObjectAdapterException;

	/**
	 * 
	 * @param userId
	 * @param submissionId
	 * @param submissionContributor
	 * @return
	 * @throws NotFoundException 
	 */
	public SubmissionContributor addSubmissionContributor(Long userId, String submissionId, SubmissionContributor submissionContributor) throws NotFoundException;
	
	/**
	 * Get a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission getSubmission(Long userId, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the SubmissionStatus object for a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public SubmissionStatus getSubmissionStatus(Long userId, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Update the SubmissionStatus object for a Submission. Note that the
	 * requesting user must be an admin of the Evaluation for which this
	 * Submission was created.
	 * 
	 * @param userId
	 * @param submissionStatus
	 * @return
	 * @throws NotFoundException
	 */
	public SubmissionStatus updateSubmissionStatus(Long userId,
			SubmissionStatus submissionStatus) throws NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param batch
	 * @return
	 * @throws NotFoundException
	 */
	BatchUploadResponse updateSubmissionStatusBatch(Long userId, String evalId,
			SubmissionStatusBatch batch) throws NotFoundException;
	
	/**
	 * Delete a Submission. Note that the requesting user must be an admin
	 * of the Evaluation for which this Submission was created.
	 * 
	 * Use of this method is discouraged, since Submissions should be immutable.
	 * 
	 * @param userId
	 * @param submissionId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	public void deleteSubmission(Long userId, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all Submissions for a given Evaluation. This method requires admin
	 * rights.
	 * 
	 * If a SubmissionStatusEnum is provided, results will be filtered
	 * accordingly.
	 * 
	 * @param userId
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Submission> getAllSubmissions(Long userId, String evalId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all Submissions by a given Synapse user, for a given Evaluation
	 * 
	 * @param evalId
	 * @param userName
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	PaginatedResults<Submission> getMyOwnSubmissionsByEvaluation(String evalId,
			Long userId, long limit, long offset, HttpServletRequest request) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get the number of Submissions to a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getSubmissionCount(Long userId, String evalId) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Evaluation and user.
	 * 
	 * @param evalId
	 * @param limit
	 * @param offset
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<SubmissionBundle> getMyOwnSubmissionBundlesByEvaluation(
			String evalId, Long userId, long limit, long offset,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Evaluation and status.
	 * Requires admin permission on the Evaluation.
	 * 
	 * @param userName
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(Long userId,
			String evalId, SubmissionStatusEnum status, long limit,
			long offset, HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException;

	/**
	 * Get all SubmissionStatuses for a given Evaluation. This method is
	 * publicly-accessible.
	 * 
	 * If a SubmissionStatusEnum is provided, results will be filtered
	 * accordingly.
	 * 
	 * @param userName
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(Long userId, String evalId, 
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get a pre-signed URL to access a requested File contained within a
	 * specified Submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @param fileHandleId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public String getRedirectURLForFileHandle(Long userId, String submissionId,
			String fileHandleId) throws DatastoreException, NotFoundException;

	////// Methods for managing ACLs //////

	/**
	 * determine whether a user has the given access type for a given evaluation
	 * 
	 * @param evalId
	 * @param userId
	 * @param accessType
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@Deprecated
	public <T extends Entity> boolean hasAccess(String evalId, Long userId,
			HttpServletRequest request, String accessType)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Updates with the given ACL.
	 */
	public AccessControlList updateAcl(Long userId, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Gets the access control list (ACL) governing the given evaluation.
	 */
	public AccessControlList getAcl(Long userId, String evalId)
			throws NotFoundException, DatastoreException, ACLInheritanceException;

	/**
	 * Gets the user permissions for an evaluation.
	 */
	public UserEvaluationPermissions getUserPermissionsForEvaluation(Long userId, String evalId)
			throws NotFoundException, DatastoreException;

	/**
	 * Executes a user query against Submissions of a specified Evaluation.
	 * 
	 * @param userQuery
	 * @param userName
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws ParseException 
	 */
	public QueryTableResults query(String userQuery, Long userId)
			throws DatastoreException, NotFoundException, JSONObjectAdapterException, ParseException;

	public TeamSubmissionEligibility getTeamSubmissionEligibility(Long userId, String evalId, String teamId) throws NotFoundException;

	/**
	 * Process the user's request to cancel their submission.
	 * 
	 * @param userId
	 * @param subId
	 * @return
	 */
	public void processCancelSubmissionRequest(Long userId, String subId);
	
}