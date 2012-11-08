package org.sagebionetworks.competition.manager;

import java.util.Set;

import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	 * Add a Participant to an existing Competition. 'userId' is of the
	 * requesting user, 'compId' is of the target competition, and
	 * 'idToAdd' is of the user to be added as a Participant.
	 * 
	 * Note that Competition admins can any user at any time to a Competition,
	 * while non-admins can only manage their own Participation.
	 * 
	 * @param userId
	 * @param compId
	 * @param idToAdd
	 * @return
	 * @throws NotFoundException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId,
			String idToAdd) throws NotFoundException;

	/**
	 * Remove a Participant from a Competition.
	 * 
	 * @param userId
	 * @param compId
	 * @param idToRemove
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId,
			String idToRemove) throws DatastoreException, NotFoundException;

	/**
	 * Get all Participants for a given Competition. This method requires
	 * admin rights on the requested Competition.
	 * 
	 * @param userId
	 * @param compId
	 * @return
	 * @throws NumberFormatException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Set<Participant> getAllParticipants(String userId, String compId)
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