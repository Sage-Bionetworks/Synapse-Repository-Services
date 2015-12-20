package org.sagebionetworks.evaluation.manager;

import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantManager {

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
}