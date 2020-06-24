package org.sagebionetworks.evaluation.dao;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.model.Evaluation;
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
	 * @return The list of evaluations that match the given filter
	 */
	List<Evaluation> getAccessibleEvaluations(EvaluationFilter filter, long limit, long offset)
			throws DatastoreException, NotFoundException;
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
	
	/**
	 * @param ids
	 * @return The subset of evaluation ids that still exists
	 */
	Set<Long> getAvailableEvaluations(List<Long> ids);

}
