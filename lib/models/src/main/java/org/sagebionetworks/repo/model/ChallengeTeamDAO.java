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
	 * Returns a list of Teams which either 
	 * (1) are registered for the challenge with the given user as a member, OR
	 * (2) the user is an admin
	 */
	public List<String> listSubmissionTeams(long challengeId,
			long submitterPrincipalId, long limit, long offset);

	public long listSubmissionTeamsCount(long challengeId,
			long submitterPrincipalId);
	
	
}
