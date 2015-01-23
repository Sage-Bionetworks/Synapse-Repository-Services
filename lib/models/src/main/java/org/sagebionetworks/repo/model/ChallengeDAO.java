package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeDAO {
	
	public Challenge create(Challenge dto) throws DatastoreException;
	
	public Challenge getForProject(String projectId) throws NotFoundException, DatastoreException;
	
	public List<ChallengeSummary> listForUser(String principalId, long limit, long offset) 
			throws NotFoundException, DatastoreException;
	
	public long listForUserCount(String principalId) throws NotFoundException, DatastoreException;
	
	public Challenge update(Challenge dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws NotFoundException, DatastoreException;
	
	/**
	 * Return challenge participants.  If affiliated=true, return just participants affiliated with 
	 * some registered Team.  If false, return those affiliated with no Team.  If missing return 
	 * all participants.
	 */
	public List<UserGroupHeader> listParticipants(String challengeId, Boolean affiliated, Long limit, Long offset) 
			throws NotFoundException, DatastoreException;

	public long listParticipantsCount(String challengeId, Boolean affiliated) 
			throws NotFoundException, DatastoreException;
}
