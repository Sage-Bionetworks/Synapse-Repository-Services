package org.sagebionetworks.competition.manager;

import java.util.List;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantManager {

	/**
	 * Get a Participant
	 * 
	 * @param userId
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Participant getParticipant(String userId, String compId)
			throws DatastoreException, NotFoundException;

	/**
	 * Add self as a Participant to a Competition.
	 * 
	 * @param userInfo
	 * @param compId
	 * @return
	 * @throws NotFoundException
	 */
	Participant addParticipant(UserInfo userInfo, String compId) throws NotFoundException;

	/**
	 * Add a different user as a Participant to a Competition.
	 * 'userInfo' is the requesting user, 'compId' is of the target competition,
	 * and 'idToAdd' is of the user to be added as a Participant.
	 * 
	 * @param userInfo
	 * @param compId
	 * @param idToAdd
	 * @return
	 * @throws NotFoundException
	 */
	public Participant addParticipantAsAdmin(UserInfo userInfo, String compId,
			String idToAdd) throws NotFoundException;

	/**
	 * Remove a Participant from a Competition.
	 * 
	 * @param userInfo
	 * @param compId
	 * @param idToRemove
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void removeParticipant(UserInfo userInfo, String compId,
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
	public List<Participant> getAllParticipants(String compId)
			throws NumberFormatException, DatastoreException, NotFoundException;

	/**
	 * Get the number of Participants in a given Competition.
	 * 
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getNumberofParticipants(String compId)
			throws DatastoreException, NotFoundException;

}