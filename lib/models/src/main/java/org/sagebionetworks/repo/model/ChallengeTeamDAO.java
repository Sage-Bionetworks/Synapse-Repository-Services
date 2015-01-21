package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeTeamDAO {
	
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException;
	
	public List<ChallengeTeam> listForChallenge(String challengeId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listForChallengeCount(String challengeId) throws NotFoundException, DatastoreException;
	
	public ChallengeTeam update(ChallengeTeam dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws NotFoundException, DatastoreException;
}
