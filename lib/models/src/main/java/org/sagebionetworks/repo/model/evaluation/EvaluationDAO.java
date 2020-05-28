package org.sagebionetworks.repo.model.evaluation;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
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
	 * and optionally filtered by the start and end times derived from the Evaluation's Quota
	 */
	public List<Evaluation> getAccessibleEvaluationsForProject(String projectId, List<Long> principalIds, ACCESS_TYPE accessType, Long optionalTimeToFilterBy, long limit, long offset) throws DatastoreException, NotFoundException;
	
	/**
	 * @return the Evaluations that any of the given principalIds may 
	 * participate in, optionally filtered by a  given list of Evaluations
	 * and optionally filtered by the start and end times derived from the Evaluation's Quota
	 */
	public List<Evaluation> getAccessibleEvaluations(List<Long> principalIds, ACCESS_TYPE accessType, Long optionalTimeToFilterBy, long limit, long offset, List<Long> evaluationIds) throws DatastoreException;

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
