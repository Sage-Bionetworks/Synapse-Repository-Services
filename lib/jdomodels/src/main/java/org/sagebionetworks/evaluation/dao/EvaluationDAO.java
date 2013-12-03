package org.sagebionetworks.evaluation.dao;

import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EvaluationDAO {	
	
	/**
	 * Lookup a Evaluation ID by name. Returns null if the name is not in use.
	 */
	public String lookupByName(String name) throws DatastoreException;

	/**
	 * Create a new Evaluation
	 * 
	 * @param dto object to be created
	 * @return the id of the newly created object
	 */
	public String create(Evaluation dto, Long ownerId) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieve the object given its id
	 */
	public Evaluation get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Retrieves all evaluations (subject to limit and offset) drawing content from the project
	 */
	public List<Evaluation> getByContentSource(String id, long limit, long offset) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all Evaluations, in a given range. Note that Evaluations of all
	 * states are returned.
	 */
	public List<Evaluation> getInRange(long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * @return the Evaluations that any of the given principalIds may participate in
	 */
	public List<Evaluation> getAvailableInRange(List<Long> principalIds, long limit, long offset) throws DatastoreException;

	/**
	 * Get all Evaluations, in a given range, filtered by EvaluationStatus.
	 */
	public List<Evaluation> getInRange(long limit, long offset, EvaluationStatus status) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total count of Evaluations in the system
	 */
	public long getCount() throws DatastoreException;
	
	/**
	 * Gets the total count of evaluations tied to a project
	 */
	public long getCountByContentSource(String id) throws DatastoreException;
	
	public long getAvailableCount(List<Long> principalIds) throws DatastoreException;

	/**
	 * Updates a Evaluation. Note that this operation requires a current eTag,
	 * which will be updated upon completion.
	 *
	 * @param dto non-null id is required
	 */
	public void update(Evaluation dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * Delete the object given by the given ID
	 * 
	 * @param id the id of the object to be deleted
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

}
