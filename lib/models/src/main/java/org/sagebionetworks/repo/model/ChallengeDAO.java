package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeDAO {
	
	public Challenge create(Challenge dto) throws DatastoreException;
	
	public List<ChallengeSummary> listForUser(String principalId, long limit, long offset) throws NotFoundException, DatastoreException;
	
	public long listForUserCount(String principalId) throws NotFoundException, DatastoreException;
	
	public Challenge update(Challenge dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws NotFoundException, DatastoreException;
}
