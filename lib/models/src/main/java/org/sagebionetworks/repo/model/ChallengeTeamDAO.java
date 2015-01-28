package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeTeamDAO {
	
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException;
	
	public List<ChallengeTeamSummary> listForChallenge(String challengeId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listForChallengeCount(String challengeId) throws NotFoundException, DatastoreException;
	
	/*
	 * Returns the Teams which are NOT registered for the challenge and on which is current user is an ADMIN.
	 */
	public List<Team> listRegistratable(String challengeId, String userId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listRegistratableCount(String challengeId, String userId) throws NotFoundException, DatastoreException;
	
	public ChallengeTeam update(ChallengeTeam dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws NotFoundException, DatastoreException;
}
