package org.sagebionetworks.competition.service;

import java.util.Set;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface CompetitionService {

	/**
	 * Create a new Synapse Competition. The requesting user becomes the owner
	 * of the Competition.
	 * 
	 * @param userId
	 * @param comp
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition createCompetition(String userId, Competition comp)
			throws DatastoreException, NotFoundException;

	/**
	 * Get a Copmetition by ID
	 * 
	 * @param userId
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Competition getCompetition(String userId, String compId)
			throws DatastoreException, NotFoundException;

	/**
	 * Update a Synapse Competition
	 * 
	 * @param userId
	 * @param comp
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition updateCompetition(String userId, Competition comp)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Delete a Synapse Competition
	 * 
	 * @param userId
	 * @param compId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteCompetition(String userId, String compId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/* ***************************
	 * Administrator management
	 */
	/**
	 * Get the list of administrators for a Competition.
	 * 
	 * @param userId
	 * @param compId
	 */
	public void getAdministrators(String userId, String compId);

	/**
	 * Set the list of administrators for a Competition.
	 * 
	 * @param userId
	 * @param compId
	 * @param adminIds
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setAdministrators(String userId, String compId,
			Set<String> adminIds);

	/**
	 * Add a user as an administrator of a Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param idToAdd
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addAdministrator(String userId, String compId, String idToAdd);

	/**
	 * Revoke a user's administrator status for a Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param idToRemove
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAdministrator(String userId, String compId,
			String idToRemove);

	/* ***************************
	 * Participant management
	 */
	/**
	 * Add a new Participant to a Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param idToAdd
	 * @return
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId,
			String idToAdd);

	/**
	 * Remove a Participant from a Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param idToRemove
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId,
			String idToRemove);

	/**
	 * Get the set of all Participants in a Competition
	 * 
	 * @param userId
	 * @param compId
	 * @return
	 */
	public Set<Participant> getAllParticipants(String userId, String compId);

	/**
	 * Get all Submissions (scored and unscored) to a particular Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param participantId
	 * @return
	 */
	public Set<Submission> getAllSubmissions(String userId, String compId);

	/**
	 * Get all unscored Submissions to a particular Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param participantId
	 * @return
	 */
	public Set<Submission> getAllUnscoredSubmissions(String userId,
			String compId);

	/**
	 * Get all Submissions by a given user
	 * 
	 * @param userId
	 * @param participantSynapseId
	 * @return
	 */
	public Set<Submission> getAllSubmissionsByUser(String userId,
			String participantId);

	/**
	 * Submit an Entity to a Competition
	 * 
	 * @param userId
	 * @param compId
	 * @param entityId
	 * @return
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission addSubmission(String userId, String compId,
			String entityId);

	/**
	 * Update a Submission
	 * 
	 * @param userId
	 * @param submission
	 * @return
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission updateSubmission(String userId, Submission submission);

	/**
	 * Delete a Submission
	 * 
	 * @param userId
	 * @param submission
	 * @return
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission deleteSubmission(String userId, String submissionId);

}