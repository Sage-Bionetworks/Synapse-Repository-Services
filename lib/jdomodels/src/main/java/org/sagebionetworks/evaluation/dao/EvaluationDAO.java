package org.sagebionetworks.evaluation.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
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

	/**
	 * Creates a new evaluation round
	 * @param evaluationRound
	 * @return the stored evaluation round
	 */
	public EvaluationRound createEvaluationRound(EvaluationRound evaluationRound);

	/**
	 * Update an existing evaluation round
	 * @param evaluationRound
	 */
	public void updateEvaluationRound(EvaluationRound evaluationRound);

	/**
	 * Deletes the evaluation round identified by the provided ID
	 * @param evaluationId
	 * @param evaluationRoundId
	 */
	public void deleteEvaluationRound(String evaluationId, String evaluationRoundId);

	/**
	 * Get the evaluation round identified by the provided ID
	 * @param evaluationId
	 * @param evaluationRoundId
	 * @return EvaluationRound for the current Id.
	 */
	public EvaluationRound getEvaluationRound(String evaluationId, String evaluationRoundId);

	/**
	 * Get evaluation rounds associated with the evaluationId.
	 * Results will be ordered by Round's start date
	 * @param evaluationId id of the Evaluation
	 * @param limit maximum number of results to return
	 * @param offset starting offset for results
	 * @return EvaluationRounds associated with the evaluationId, ordered by roundStart dates in the EvaluationRound
	 * Empty list if no results.
	 */
	public List<EvaluationRound> getAssociatedEvaluationRounds(String evaluationId, long limit, long offset);

	/**
	 * Get the EvaluationRound for a specified Evaluation ID such that the specified timestamp
	 * resides between the EvaluationRound's start and end timestamps.
	 *
	 * The start timestamp is inclusive
	 * The end timestamp is exclusive
	 *
	 * For example:
	 * EvaluationRound A : start=5 , end=35
	 * EvaluationRound B : start=40, end=55
	 *
	 * Any of {timestamp=25, timestamp=5} would return A
	 * {timestamp=45, timestamp=40} return B,
	 * {timestamp=35, timestamp=37, timestamp=4, timestamp=55, timestamp=56} return Optional.empty()
	 *  @param evaluationId id of the Evaluation to search
	 * @param timestamp the timestamp for which a matching EvaluationRound's round start and round end timestamp must encapsulate
	 */
	public EvaluationRound getEvaluationRoundForTimestamp(String evaluationId, Instant timestamp);

	/**
	 *
	 * @param evaluationId id of the Evaluation
	 * @return true if the Evaluation has any EvaluationRounds associated with it. Otherwise, false.
	 */
	boolean hasEvaluationRounds(String evaluationId);

	/**
	 * Lists existing EvaluationRounds for which provided start-end timestamp range overlap
	 *
	 * @param evaluationId
	 * @param startTimestamp
	 * @param endTimestamp
	 * @return existing EvaluationRounds for which provided start-end timestamp range overlap
	 */
	List<EvaluationRound> overlappingEvaluationRounds(String evaluationId, Instant startTimestamp, Instant endTimestamp);
}
