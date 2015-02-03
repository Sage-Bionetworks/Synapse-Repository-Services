package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ChallengeManager {
	
	/**
	 * 
	 * @param userInfo
	 * @param challenge
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Challenge createChallenge(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param projectId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Challenge getByProjectId(UserInfo userInfo, String projectId) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param participantId
	 * @return
	 * @throws DatastoreException
	 */
	public ChallengePagedResults listForParticipant(UserInfo userInfo, String participantId) throws DatastoreException;

	/**
	 * 
	 * @param userInfo
	 * @param challenge
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Challenge update(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param challengeId
	 * @throws DatastoreException
	 */
	public void delete(UserInfo userInfo, Long challengeId) throws DatastoreException;
	
	/**
	 * 
	 * @param userInfo
	 * @param challengeId
	 * @param affiliated
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedIds listParticipants(UserInfo userInfo, long challengeId, Boolean affiliated, long limit, long offset) throws DatastoreException, NotFoundException;
}
