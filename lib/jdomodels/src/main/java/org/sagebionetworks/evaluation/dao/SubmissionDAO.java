package org.sagebionetworks.evaluation.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionDAO {

	/**
	 * Create a new Submission
	 * 
	 * @param dto
	 * @return ID of the created Submission
	 */
	public String create(Submission dto);


	void addSubmissionContributor(String submissionId, SubmissionContributor dto);

	/**
	 * Get a Submission by ID
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission get(String id) throws DatastoreException,
			NotFoundException;

	/**
	 * Get the total number of Submissions in Synapse
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Delete a Submission
	 * 
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * Get all of the Submissions by a given User (may span multiple Evaluations)
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByUser(String userId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	public long getCountByUser(String userId) throws DatastoreException,
	NotFoundException;

	/**
	 * Get all of the Submissions for a given Evaluation
	 * 
	 * @param evalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByEvaluation(String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total number of Submissions for a given Evaluation
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCountByEvaluation(String evalId) throws DatastoreException, 
			NotFoundException;

	/**
	 * Get all Submissions from a given Evaluation with a certain status. (e.g.
	 * get all UNSCORED submissions from Evaluation x).
	 * 
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByEvaluationAndStatus(String evalId,
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, NotFoundException;

	public long getCountByEvaluationAndStatus(String evalId,
	SubmissionStatusEnum status) throws DatastoreException,
	NotFoundException;

	/**
	 * Get all Submissions from a given user for a given Evaluation.
	 * 
	 * @param evalId
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByEvaluationAndUser(String evalId, String principalId,
			long limit, long offset)
			throws DatastoreException, NotFoundException;

	public long getCountByEvaluationAndUser(String evalId, String userId)
			throws DatastoreException, NotFoundException;


	/*
	 * Find the number of Team submissions in the given evaluation queue and time interval
	 * optionally filtering by submission status.  Caller may also omit start or end
	 * dates to create open ended time segments.
	 * 
	 */
	long countSubmissionsByTeam(long evaluationId, long teamId,
			Date startDateIncl, Date endDateExcl,
			Set<SubmissionStatusEnum> statuses);

	/*
	 * Find the number of submissions in a given evaluation queue contributed to by each member of a given team
	 * and time interval, optionally filtering by submission status.  Caller may also omit start or end
	 * dates to create open ended time segments.
	 * 
	 */
	Map<Long, Long> countSubmissionsByTeamMembers(long evaluationId,
			long teamId, Date startDateIncl, Date endDateExcl,
			Set<SubmissionStatusEnum> statuses);

	/*
	 * return the number of submissions involving the given contributor in the given evaluation queue,
	 * optionally filtered by time segment and/or sub-statuses
	 */
	long countSubmissionsByContributor(long evaluationId,
			long contributorId, Date startDateIncl, Date endDateExcl,
			Set<SubmissionStatusEnum> statuses);

	/*
	 * list the team members in the given team who have submitted individually or on another team
	 * in the specified time interval (optional), filtered by the given sub-statues (optional)
	 * 
	 */
	List<Long> getTeamMembersSubmittingElsewhere(long evaluationId, long teamId,
			Date startDateIncl, Date endDateExcl,
			Set<SubmissionStatusEnum> statuses);

	/*
	 * Determine whether the given user has contributed to any team submission in the given
	 * evaluation, in the specified time interval (optional), filtered by the given sub-statues (optional)
	 */
	boolean hasContributedToTeamSubmission(long evaluationId,
			long contributorId, Date startDateIncl, Date endDateExcl,
			Set<SubmissionStatusEnum> statuses);

	/**
	 * Get the Id of the user who created the submission.
	 * 
	 * @param submissionId
	 * @return
	 */
	String getCreatedBy(String submissionId);
	
	/*
	 * Return true if and only if the given Docker Repository name is in a Submission under some Evaluation 
	 * in which the given user (represented by a list of principalIds) has the given access type.
	 */
	boolean isDockerRepoNameInAnyEvaluationWithAccess(String dockerRepoName, Set<Long> principalIds, ACCESS_TYPE accessType);

	// Include contributors
	SubmissionBundle getBundle(String id);
	
	SubmissionBundle getBundle(String submissionId, boolean includeContributors);

	List<SubmissionBundle> getAllBundlesByEvaluation(String evalId, long limit,
			long offset) throws DatastoreException, NotFoundException;


	List<SubmissionBundle> getAllBundlesByEvaluationAndStatus(String evalId,
			SubmissionStatusEnum status, long limit, long offset)
			throws DatastoreException, NotFoundException;


	List<SubmissionBundle> getAllBundlesByEvaluationAndUser(String evalId,
			String principalId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * @param evaluationId The id of an evaluation
	 * @return The list of {@link IdAndEtag} of the submissions associated with the evaluation with the given id
	 */
	List<IdAndEtag> getSubmissionIdAndEtag(Long evaluationId);

	/**
	 * 
	 * @param evaluationIds The list of evaluation id
	 * @return For each of the evaluation id, computes the sum of the CRC for each submission in the evaluation
	 */
	Map<Long, Long> getSumOfSubmissionCRCsForEachEvaluation(List<Long> evaluationIds);

	/**
	 * @param submissionIds
	 * @param maxAnnotationChars
	 * @return The data to be indexed in the replication
	 */
	List<ObjectDataDTO> getSubmissionData(List<Long> submissionIds, int maxAnnotationChars);
	
	// For testing
	void truncateAll();

	/**
	 *
	 * @param evalId
	 * @param evalRoundId
	 * @return true if any Submissions are associated with the evalRoundId. false otherwise.
	 */
	boolean hasSubmissionForEvaluationRound(String evalId, String evalRoundId);
}