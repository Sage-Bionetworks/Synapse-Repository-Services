package org.sagebionetworks.evaluation.manager;

import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantManager {

	/**
	 * Get a Participant
	 * 
	 * @param userId
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Participant getParticipant(UserInfo userInfo, String participantId, String evalId)
			throws DatastoreException, NotFoundException;

	/**
	 * Add self as a Participant to a Evaluation.
	 * 
	 * @param userInfo
	 * @param evalId
	 * @return
	 * @throws NotFoundException
	 */
	Participant addParticipant(UserInfo userInfo, String evalId) throws NotFoundException;

	/**
	 * Remove a Participant from a Evaluation.
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param idToRemove
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void removeParticipant(UserInfo userInfo, String evalId,
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
	public QueryResults<Participant> getAllParticipants(UserInfo userInfo, String evalId, long limit, long offset)
			throws NumberFormatException, DatastoreException, NotFoundException;

	/**
	 * Get the number of Participants in a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getNumberofParticipants(UserInfo userInfo, String evalId)
			throws DatastoreException, NotFoundException;

}