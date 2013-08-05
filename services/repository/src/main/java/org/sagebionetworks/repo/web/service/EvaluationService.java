package org.sagebionetworks.repo.web.service;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
	public Evaluation createEvaluation(String userId, Evaluation eval)
			throws DatastoreException, InvalidModelException, NotFoundException;
	
	/**
	 * Get a Synapse Evaluation by its id
	 */
	public Evaluation getEvaluation(String userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Get a collection of Evaluations, within a given range
	 *
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	public PaginatedResults<Evaluation> getEvaluationsInRange(String userId, long limit, long offset,
			HttpServletRequest request) throws DatastoreException, NotFoundException;

	/**
	 * Get a collection of Evaluations in which the user may participate, within a given range
	 *
	 * @param userId the userId (email address) of the user making the request
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	public PaginatedResults<Evaluation> getAvailableEvaluationsInRange(
			String userId, EvaluationStatus status, long limit, long offset, HttpServletRequest request) throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Evaluations in the system
	 *
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	public long getEvaluationCount(String userId) throws DatastoreException,
			NotFoundException;

	/**
	 * Find a Evaluation, by name
	 * 
	 * @param name
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Evaluation findEvaluation(String userId, String name) throws DatastoreException,
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
	public Evaluation updateEvaluation(String userId, Evaluation eval)
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
	public void deleteEvaluation(String userId, String evalId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	////// Methods for managing participants //////

	/**
	 * Add self as a Participant to a Evaluation.
	 * 
	 * @param userName
	 * @param evalId
	 * @return
	 * @throws NotFoundException
	 */
	public Participant addParticipant(String userName, String evalId)
			throws NotFoundException;

	/**
	 * Get a Participant
	 * 
	 * @param principalId
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Participant getParticipant(String userId, String principalId, String evalId)
			throws DatastoreException, NotFoundException;

	/**
	 * Remove a Participant from a Evaluation.
	 * 
	 * @param userId
	 * @param evalId
	 * @param idToRemove
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void removeParticipant(String userId, String evalId,
			String idToRemove) throws DatastoreException, NotFoundException;

	/**
	 * Get all Participants for a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws NumberFormatException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Participant> getAllParticipants(String userId, String evalId, long limit, long offset, HttpServletRequest request)
			throws NumberFormatException, DatastoreException, NotFoundException;

	/**
	 * Get the number of Participants in a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getParticipantCount(String userId, String evalId) throws DatastoreException,
			NotFoundException;

	////// Methods for managing submissions //////

	/**
	 * Create a Submission.
	 * 
	 * @param userId
	 * @param submission
	 * @param entityEtag
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 * @throws JSONObjectAdapterException
	 */
	public Submission createSubmission(String userId, Submission submission, String entityEtag, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException, ParseException, JSONObjectAdapterException;

	/**
	 * Get a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission getSubmission(String userName, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the SubmissionStatus object for a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public SubmissionStatus getSubmissionStatus(String userName, String submissionId)
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
	public SubmissionStatus updateSubmissionStatus(String userId,
			SubmissionStatus submissionStatus) throws NotFoundException;

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
	public void deleteSubmission(String userId, String submissionId)
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
	public PaginatedResults<Submission> getAllSubmissions(String userId, String evalId,
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
	PaginatedResults<Submission> getAllSubmissionsByEvaluationAndUser(String evalId,
			String userName, long limit, long offset, HttpServletRequest request) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get the number of Submissions to a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getSubmissionCount(String userName, String evalId) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Evaluation and user.
	 * 
	 * @param evalId
	 * @param userName
	 * @param limit
	 * @param offset
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByEvaluationAndUser(
			String evalId, String userName, long limit, long offset,
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
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundles(String userName,
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
	PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(String userId, String evalId, 
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
	public URL getRedirectURLForFileHandle(String userName, String submissionId,
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
	public <T extends Entity> boolean hasAccess(String evalId, String userName,
			HttpServletRequest request, String accessType)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Creates a new ACL.
	 */
	public AccessControlList createAcl(String userName, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Updates with the given ACL.
	 */
	public AccessControlList updateAcl(String userName, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Deletes the ACL of the specified evaluation.
	 */
	public void deleteAcl(String userName, String evalId)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * Gets the access control list (ACL) governing the given evaluation.
	 */
	public AccessControlList getAcl(String userName, String evalId)
			throws NotFoundException, DatastoreException, ACLInheritanceException;

	/**
	 * Gets the user permissions for an evaluation.
	 */
	public UserEvaluationPermissions getUserPermissionsForEvaluation(String userName, String evalId)
			throws NotFoundException, DatastoreException;
}