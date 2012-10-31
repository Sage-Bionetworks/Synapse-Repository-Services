package org.sagebionetworks.competition.manager;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CompetitionManager {

	/**
	 * Create a new Synapse Competition
	 */
	public String createCompetition(String userId, Competition comp)
			throws DatastoreException, InvalidModelException;

	/**
	 * Get a Synapse Competition by its id
	 */
	public Competition getCompetition(String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Get a collection of Competitions, within a given range
	 */
	public QueryResults<Competition> getInRange(long startIncl, long endExcl) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Competitions in the system
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Find a Competition, by name
	 */
	public Competition findCompetition(String name)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update a Synapse Competition.
	 */
	public Competition updateCompetition(String userId, Competition comp)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, InvalidModelException,
			ConflictingUpdateException;

	/**
	 * Delete a Synapse Competition.
	 */
	public void deleteCompetition(String userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Check whether a given user is an adminsitrator of a given Competition.
	 * 
	 * @param userId
	 * @param comp
	 * @return
	 */
	public boolean isCompAdmin(String userId, Competition comp);
	
}