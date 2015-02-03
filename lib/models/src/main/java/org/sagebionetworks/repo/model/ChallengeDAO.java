package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;


public interface ChallengeDAO {
	
	public Challenge create(Challenge dto) throws DatastoreException;
	
	public Challenge get(long challengeId) throws NotFoundException, DatastoreException;
	
	public Challenge getForProject(String projectId) throws NotFoundException, DatastoreException;
	
	/*
	 * returns the Challenges for which the given participant is registered
	 */
	public List<Challenge> listForUser(long principalId, long limit, long offset) 
			throws NotFoundException, DatastoreException;
	
	public long listForUserCount(long principalId) throws NotFoundException, DatastoreException;
	
	/*
	 * returns the Challenges for which the given participant is registered and the given requester has read access 
	 * (that it, has READ access to the project linked to this challenge)
	 * a requester is given by the list of their principals (user ID and groups they belong to)
	 */
	public List<Challenge> listForUser(long principalId, Set<Long> requesterPrincipals, long limit, long offset) 
			throws NotFoundException, DatastoreException;
	
	public long listForUserCount(long principalId, Set<Long> requesterPrincipals) 
			throws NotFoundException, DatastoreException;

	public Challenge update(Challenge dto) throws NotFoundException, DatastoreException;
	
	public void delete(long id) throws NotFoundException, DatastoreException;
	
	/**
	 * Return challenge participants.  If affiliated=true, return just participants affiliated with 
	 * some registered Team.  If false, return those affiliated with no Team.  If missing return 
	 * all participants.
	 */
	public List<Long> listParticipants(long challengeId, Boolean affiliated, long limit, long offset) 
			throws NotFoundException, DatastoreException;

	public long listParticipantsCount(long challengeId, Boolean affiliated) 
			throws NotFoundException, DatastoreException;
}
