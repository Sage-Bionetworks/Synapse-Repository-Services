package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeTeamDAO {
	
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException;
	
	/*
	 * List the Teams registered for the given challenge 
	 */
	public List<ChallengeTeam> listForChallenge(long challengeId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listForChallengeCount(long challengeId) throws NotFoundException, DatastoreException;
	
	/*
	 * Returns the Teams which are NOT registered for the challenge and on which is current user is an ADMIN.
	 */
	public List<Team> listRegistratable(long challengeId, long userId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listRegistratableCount(long challengeId, long userId) throws NotFoundException, DatastoreException;
	
	public ChallengeTeam update(ChallengeTeam dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws DatastoreException;
}
