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
	public Participant getParticipant(String userId, String evalId)
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
	 * Add a different user as a Participant to a Evaluation.
	 * 'userInfo' is the requesting user, 'evalId' is of the target Evaluation,
	 * and 'idToAdd' is of the user to be added as a Participant.
	 * 
	 * @param userInfo
	 * @param evalId
	 * @param idToAdd
	 * @return
	 * @throws NotFoundException
	 */
	public Participant addParticipantAsAdmin(UserInfo userInfo, String evalId,
			String idToAdd) throws NotFoundException;

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
	public QueryResults<Participant> getAllParticipants(String evalId, long limit, long offset)
			throws NumberFormatException, DatastoreException, NotFoundException;

	/**
	 * Get the number of Participants in a given Evaluation.
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getNumberofParticipants(String evalId)
			throws DatastoreException, NotFoundException;

}