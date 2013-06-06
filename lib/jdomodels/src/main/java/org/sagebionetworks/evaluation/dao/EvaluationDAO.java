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
	 * 
	 * @param name
	 * @return
	 * @throws DatastoreException 
	 */
	public String lookupByName(String name) throws DatastoreException;

	/**
	 * Create a new Evaluation
	 * 
	 * @param dto     object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(Evaluation dto, Long ownerId) throws DatastoreException,
			InvalidModelException;

	/**
	 * Creates a Evaluation from backup. The passed eTag is persisted.
	 * 
	 * @param dto
	 * @param ownerId
	 * @return
	 * @throws DatastoreException
	 */
	public String createFromBackup(Evaluation dto, Long ownerId)
			throws DatastoreException;

	/**
	 * Retrieve the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Evaluation get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all Evaluations, in a given range. Note that Evaluations of all
	 * states are returned.
	 * 
	 * @param limit
	 * @param offset
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Evaluation> getInRange(long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * return the Evaluations that any of the given principalIds may participate in
	 * @param principalIds
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Evaluation> getAvailableInRange(List<Long> principalIds, EvaluationStatus status, long limit, long offset) throws DatastoreException;

	/**
	 * Get all Evaluations, in a given range, filtered by EvaluationStatus.
	 * 
	 * @param limit
	 * @param offset
	 * @param status
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Evaluation> getInRange(long limit, long offset, EvaluationStatus status) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total count of Evaluations in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 */
	public long getCount() throws DatastoreException;
	
	public long getAvailableCount(List<Long> principalIds, EvaluationStatus status) throws DatastoreException;

	/**
	 * Updates a Evaluation. Note that this operation requires a current eTag,
	 * which will be updated upon completion.
	 *
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(Evaluation dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * Updates a Evaluation from backup. The passed eTag is persisted.
	 * 
	 * @param dto
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public void updateFromBackup(Evaluation dto) throws DatastoreException,
				InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * Delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

}
