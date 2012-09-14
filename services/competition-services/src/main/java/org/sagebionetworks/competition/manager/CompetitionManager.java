package org.sagebionetworks.competition.manager;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CompetitionManager {

	/**
	 * Create a new Synapse Competition
	 * 
	 * @param userInfo
	 * @param comp
	 * @return the id of the new Competition
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String createCompetition(UserInfo userInfo, Competition comp)
			throws DatastoreException, InvalidModelException;

	/**
	 * Get a Synapse Competition by its id
	 * 
	 * @param userInfo
	 * @param id
	 * @return the requested Competition, if found
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Competition getCompetition(String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Get a collection of Competitions, within a given range
	 * 
	 * @param userInfo
	 * @param startIncl
	 * @param endExcl
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public QueryResults<Competition> getInRange(long startIncl, long endExcl) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Competitions in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Find a Competition, by name
	 * 
	 * @param name
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	public Competition findCompetition(String name)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update a Synapse Competition.
	 * 
	 * @param userInfo
	 * @param comp
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public Competition updateCompetition(UserInfo userInfo, Competition comp)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, InvalidModelException,
			ConflictingUpdateException;

	/**
	 * Delete a Synapse Competition.
	 * 
	 * @param userInfo
	 * @param id
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void deleteCompetition(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;
	
}