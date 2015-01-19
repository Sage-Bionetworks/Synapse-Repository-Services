package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeTeamDAO {
	
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException;
	
	public ChallengeTeam get(String challengeProjectId, String teamId) throws NotFoundException, DatastoreException;
	
	public ChallengeTeam listForChallenge(String challengeProjectId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long ListForChallengeCount(String challengeProjectId) throws NotFoundException, DatastoreException;
	
	public void update(ChallengeTeam dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws NotFoundException, DatastoreException;
}
