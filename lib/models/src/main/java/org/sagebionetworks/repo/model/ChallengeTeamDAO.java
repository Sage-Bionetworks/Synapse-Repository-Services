package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeTeamDAO {
	
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException;
	
	public ChallengeTeam get(long id) throws NotFoundException, DatastoreException;
	
	/*
	 * List the Teams registered for the given challenge 
	 */
	public List<ChallengeTeam> listForChallenge(long challengeId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listForChallengeCount(long challengeId) throws NotFoundException, DatastoreException;
	
	/*
	 * Returns the IDs of the Teams which are NOT registered for the challenge 
	 * and on which is current user is an ADMIN.
	 */
	public List<String> listRegistratable(long challengeId, long userId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listRegistratableCount(long challengeId, long userId) throws NotFoundException, DatastoreException;
	
	public ChallengeTeam update(ChallengeTeam dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws DatastoreException;

	/*
	 * Returns a list of Teams either (1) on whose behalf the user is eligible to submit or 
	 * (2) on whose behalf the user WOULD be eligible to submit if the Team has been registered 
	 * for the Challenge, and which the User CAN register for the Challenge.
	 */
	public List<SubmissionTeam> listSubmissionTeams(long challengeId,
			long submitterPrincipalId, long limit, long offset);

	public Long listSubmissionTeamsCount(long challengeId,
			long submitterPrincipalId);
	
	
}
