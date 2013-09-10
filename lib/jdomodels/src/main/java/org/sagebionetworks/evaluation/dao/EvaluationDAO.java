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
	 * Creates a Evaluation from backup. The passed eTag is persisted.
	 */
	public String createFromBackup(Evaluation dto, Long ownerId)
			throws DatastoreException;

	/**
	 * Retrieve the object given its id
	 */
	public Evaluation get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Retrieves an evaluation via the project it draws its content from
	 * May return null if no such evaluation exists
	 */
	public List<Evaluation> getByContentSource(String projectId, long limit, long offset) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all Evaluations, in a given range. Note that Evaluations of all
	 * states are returned.
	 */
	public List<Evaluation> getInRange(long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * @return the Evaluations that any of the given principalIds may participate in
	 */
	public List<Evaluation> getAvailableInRange(List<Long> principalIds, EvaluationStatus status, long limit, long offset) throws DatastoreException;

	/**
	 * Get all Evaluations, in a given range, filtered by EvaluationStatus.
	 */
	public List<Evaluation> getInRange(long limit, long offset, EvaluationStatus status) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total count of Evaluations in the system
	 */
	public long getCount() throws DatastoreException;
	
	public long getAvailableCount(List<Long> principalIds, EvaluationStatus status) throws DatastoreException;

	/**
	 * Updates a Evaluation. Note that this operation requires a current eTag,
	 * which will be updated upon completion.
	 *
	 * @param dto non-null id is required
	 */
	public void update(Evaluation dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * Updates a Evaluation from backup. The passed eTag is persisted.
	 */
	public void updateFromBackup(Evaluation dto) throws DatastoreException,
				InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * Delete the object given by the given ID
	 * 
	 * @param id the id of the object to be deleted
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

}
