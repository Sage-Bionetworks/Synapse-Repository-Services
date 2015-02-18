package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ChallengeManager {
	
	/**
	 * CREATE permission is required in referenced project
	 * 
	 * @param userInfo
	 * @param challenge
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Challenge createChallenge(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException;

	/**
	 * READ permission is required in the specified project
	 * 
	 * @param userInfo
	 * @param challengeId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	Challenge getChallenge(UserInfo userInfo, long challengeId)
			throws DatastoreException, NotFoundException;

	/**
	 * READ permission is required in the specified project
	 * @param userInfo
	 * @param projectId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Challenge getChallengeByProjectId(UserInfo userInfo, String projectId) throws DatastoreException, NotFoundException;
	
	/**
	 * For a Challenge to be in the returned list the caller given by 'userInfo' must be 
	 * a Synapse Administrator or have READ permission on the associated project.
	 * @param userInfo
	 * @param participantId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public ChallengePagedResults listChallengesForParticipant(UserInfo userInfo, long participantId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * UPDATE permission required in referenced project.
	 * 
	 * @param userInfo
	 * @param challenge
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Challenge updateChallenge(UserInfo userInfo, Challenge challenge) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param challengeId
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public void deleteChallenge(UserInfo userInfo, long challengeId) throws DatastoreException, NotFoundException;
	
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
	public PaginatedIds listParticipantsInChallenge(UserInfo userInfo, long challengeId, Boolean affiliated, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param challengeTeam
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	public ChallengeTeam createChallengeTeam(UserInfo userInfo, ChallengeTeam challengeTeam) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param challengeId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ChallengeTeamPagedResults listChallengeTeams(UserInfo userInfo, long challengeId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param challengeId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedIds listRegistratableTeams(UserInfo userInfo, long challengeId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param challengeTeam
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ChallengeTeam updateChallengeTeam(UserInfo userInfo, ChallengeTeam challengeTeam) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param challengeTeamId
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public void deleteChallengeTeam(UserInfo userInfo, long challengeTeamId) throws DatastoreException, NotFoundException;
	
	/**
	 * Returns a list of Teams on whose behalf the user is eligible to submit.
	 * @param userInfo
	 * @param challengeId
	 * @param submitterPrincipalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	PaginatedIds listSubmissionTeams(UserInfo userInfo, long challengeId, long submitterPrincipalId, long limit, long offset) throws DatastoreException, NotFoundException;
}
