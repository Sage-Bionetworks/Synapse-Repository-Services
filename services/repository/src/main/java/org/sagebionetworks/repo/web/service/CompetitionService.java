package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.competition.model.SubmissionBundle;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CompetitionService {

	/**
	 * Create a new Synapse Competition
	 * 
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public Competition createCompetition(String userId, Competition comp)
			throws DatastoreException, InvalidModelException, NotFoundException;
	
	/**
	 * Get a Synapse Competition by its id
	 */
	public Competition getCompetition(String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Get a collection of Competitions, within a given range
	 *
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Competition> getCompetitionsInRange(long limit, long offset,
			HttpServletRequest request) throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Competitions in the system
	 *
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCompetitionCount() throws DatastoreException,
			NotFoundException;

	/**
	 * Find a Competition, by name
	 * 
	 * @param name
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Competition findCompetition(String name) throws DatastoreException,
			NotFoundException, UnauthorizedException;

	/**
	 * Update a Synapse Competition.
	 * 
	 * @param userId
	 * @param comp
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 */
	public Competition updateCompetition(String userId, Competition comp)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, InvalidModelException,
			ConflictingUpdateException;

	/**
	 * Delete a Synapse Competition.
	 * 
	 * @param userId
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public void deleteCompetition(String userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Add self as a Participant to a Competition.
	 * 
	 * @param userName
	 * @param compId
	 * @return
	 * @throws NotFoundException
	 */
	public Participant addParticipant(String userName, String compId)
			throws NotFoundException;

	/**
	 * Add a different user as a Participant to a Competition. Requires admin
	 * rights on the Competition.
	 * 
	 * @param userId
	 * @param compId
	 * @param idToAdd
	 * @return
	 * @throws NotFoundException
	 */
	public Participant addParticipantAsAdmin(String userId, String compId,
			String idToAdd) throws NotFoundException;

	/**
	 * Get a Participant
	 * 
	 * @param principalId
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Participant getParticipant(String principalId, String compId)
			throws DatastoreException, NotFoundException;

	/**
	 * Remove a Participant from a Competition.
	 * 
	 * @param userId
	 * @param compId
	 * @param idToRemove
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void removeParticipant(String userId, String compId,
			String idToRemove) throws DatastoreException, NotFoundException;

	/**
	 * Get all Participants for a given Competition.
	 * 
	 * @param compId
	 * @return
	 * @throws NumberFormatException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Participant> getAllParticipants(String compId, long limit, long offset, HttpServletRequest request)
			throws NumberFormatException, DatastoreException, NotFoundException;

	/**
	 * Get the number of Participants in a given Competition.
	 * 
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getParticipantCount(String compId) throws DatastoreException,
			NotFoundException;

	/**
	 * Create a Submission.
	 * 
	 * @param userId
	 * @param submission
	 * @return
	 * @throws NotFoundException
	 */
	public Submission createSubmission(String userId, Submission submission)
			throws NotFoundException;

	/**
	 * Get a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission getSubmission(String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the SubmissionStatus object for a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public SubmissionStatus getSubmissionStatus(String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Update the SubmissionStatus object for a Submission. Note that the
	 * requesting user must be an admin of the Competition for which this
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
	 * of the Competition for which this Submission was created.
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
	 * Get all Submissions for a given Competition. This method requires admin
	 * rights.
	 * 
	 * If a SubmissionStatusEnum is provided, results will be filtered
	 * accordingly.
	 * 
	 * @param userId
	 * @param compId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Submission> getAllSubmissions(String userId, String compId,
			SubmissionStatusEnum status, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all Submissions by a given Synapse user. These may span multiple
	 * Competitions.
	 * 
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<Submission> getAllSubmissionsByUser(String principalId, long limit, long offset, HttpServletRequest request)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all Submissions by a given Synapse user, for a given Competition
	 * 
	 * @param compId
	 * @param userName
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	PaginatedResults<Submission> getAllSubmissionsByCompetitionAndUser(String compId,
			String userName, long limit, long offset, HttpServletRequest request) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get the number of Submissions to a given Competition.
	 * 
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getSubmissionCount(String compId) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Competition and user.
	 * 
	 * @param compId
	 * @param userName
	 * @param limit
	 * @param offset
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByCompetitionAndUser(
			String compId, String userName, long limit, long offset,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by user.
	 * 
	 * @param princpalId
	 * @param limit
	 * @param offset
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<SubmissionBundle> getAllSubmissionBundlesByUser(
			String princpalId, long limit, long offset,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException;

	/**
	 * Get bundled Submissions and SubmissionStatuses by Competition and status.
	 * Requires admin permission on the Competition.
	 * 
	 * @param userName
	 * @param compId
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
			String compId, SubmissionStatusEnum status, long limit,
			long offset, HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException;

}