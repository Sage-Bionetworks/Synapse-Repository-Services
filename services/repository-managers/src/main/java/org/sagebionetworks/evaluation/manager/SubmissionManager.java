package org.sagebionetworks.evaluation.manager;

import java.util.List;

import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface SubmissionManager {

	/**
	 * Get a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission getSubmission(UserInfo userInfo, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the SubmissionStatus object for a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public SubmissionStatus getSubmissionStatus(UserInfo userInfo, String submissionId)
			throws DatastoreException, NotFoundException;

	
	/**
	 * Create a Submission.
	 * 
	 * @param userInfo
	 * @param submission
	 * @return
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 */
	public Submission createSubmission(UserInfo userInfo, Submission submission, String entityEtag, String submissionEligibilityHash, EntityBundle bundle)
			throws NotFoundException, DatastoreException, JSONObjectAdapterException;
	
	/**
	 * 
	 * @param submission
	 * @return
	 */
	public List<MessageToUserAndBody> createSubmissionNotifications(
			UserInfo userInfo, Submission submission, String submissionEligibilityHash,
			String challengeEndpoint, String notificationUnsubscribeEndpoint);

	/**
	 * Update the SubmissionStatus object for a Submission. Note that the
	 * requesting user must be an admin of the Evaluation for which this
	 * Submission was created.
	 * 
	 * @param userInfo
	 * @param submissionStatus
	 * @return
	 * @throws NotFoundException
	 */
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo,
			SubmissionStatus submissionStatus) throws NotFoundException, ConflictingUpdateException;
	
	/**
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param batch
	 * @return
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public BatchUploadResponse updateSubmissionStatusBatch(UserInfo userInfo, String evalId,
			SubmissionStatusBatch batch) throws NotFoundException, ConflictingUpdateException;
	/**
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @param submissionContributor
	 * @return
	 */
	public SubmissionContributor addSubmissionContributor(UserInfo userInfo,
			String submissionId, SubmissionContributor submissionContributor);

	/**
	 * Delete a Submission. Note that the requesting user must be an admin
	 * of the Evaluation for which this Submission was created.
	 * 
	 * Use of this method is discouraged, since Submissions should be immutable.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	public void deleteSubmission(UserInfo userInfo, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all Submissions for a given Evaluation. This method requires admin
	 * rights.
	 * 
	 * If a SubmissionStatusEnum is provided, results will be filtered
	 * accordingly.
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param status
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllSubmissions(UserInfo userInfo, String evalId,
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all Submissions by a given Synapse user to a given Evaluation.
	 * 
	 * @param evalId
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getMyOwnSubmissionsByEvaluation(UserInfo userInfo,
			String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the number of Submissions to a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getSubmissionCount(UserInfo userInfo, String evalId) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Evaluation and user.
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<SubmissionBundle> getMyOwnSubmissionBundlesByEvaluation(
			UserInfo userInfo, String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Evaluation and status.
	 * Requires admin permission on the Evaluation.
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<SubmissionBundle> getAllSubmissionBundles(UserInfo userInfo,
			String evalId, SubmissionStatusEnum status, long limit, long offset)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get SubmissionStatuses by Evaluation and status.
	 * 
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<SubmissionStatus> getAllSubmissionStatuses(UserInfo userInfo, String evalId, 
			SubmissionStatusEnum status, long limit, long offset)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get a redirect URL for a given FileHandle in a specified Submission.
	 * Requires admin-level permissions on the associated Evaluation.
	 * 
	 * @param submissionId
	 * @param filehandleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getRedirectURLForFileHandle(UserInfo userInfo,
			String submissionId, String filehandleId)
			throws DatastoreException, NotFoundException;

	/**
	 * Process the user cancel request
	 * 
	 * @param userInfo
	 * @param submissionId
	 */
	public void processUserCancelRequest(UserInfo userInfo, String submissionId);
}