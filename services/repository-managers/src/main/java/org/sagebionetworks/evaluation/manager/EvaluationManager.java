package org.sagebionetworks.evaluation.manager;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EvaluationManager {

	/**
	 * Create a new Synapse Evaluation
	 */
	public Evaluation createEvaluation(UserInfo userInfo, Evaluation comp)
			throws DatastoreException, InvalidModelException, NotFoundException;

	/**
	 * Get a Synapse Evaluation by its id
	 */
	public Evaluation getEvaluation(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Gets all Synapse Evaluations tied to the given project
	 */
	public QueryResults<Evaluation> getEvaluationByContentSource(UserInfo userInfo, String id, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * Get a collection of Evaluations, within a given range
	 */
	@Deprecated
	public QueryResults<Evaluation> getInRange(UserInfo userInfo, long limit, long offset) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get a collection of Evaluations to the user may SUBMIT, within a given range
	 */
	public QueryResults<Evaluation> getAvailableInRange(UserInfo userInfo, long limit, long offset) 
			throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Evaluations in the system
	 */
	@Deprecated
	public long getCount(UserInfo userInfo) throws DatastoreException, NotFoundException;

	/**
	 * Find a Evaluation, by name
	 */
	public Evaluation findEvaluation(UserInfo userInfo, String name)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update a Synapse Evaluation.
	 */
	public Evaluation updateEvaluation(UserInfo userInfo, Evaluation comp)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, InvalidModelException,
			ConflictingUpdateException;

	/**
	 * Delete a Synapse Evaluation.
	 */
	public void deleteEvaluation(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update the eTag of a Evaluation. For use when modifying objects
	 * associated with a Evaluation for migration purposes.
	 * 
	 * Note that, besides the eTag change, this method performs a NOOP update.
	 * 
	 * @param evalId
	 * @throws NotFoundException
	 */
	void updateEvaluationEtag(String evalId) throws NotFoundException;
}