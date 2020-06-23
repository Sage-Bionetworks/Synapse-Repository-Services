package org.sagebionetworks.evaluation.dao;

import java.util.List;

import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionStatusDAO {

	/**
	 * Create a new SubmissionStatus object
	 * 
	 * @param dto
	 * @return the ID of the SubmissionStatus
	 * @throws DatastoreException
	 */
	String create(SubmissionStatus dto) throws DatastoreException;

	/**
	 * Get a SubmissionStatus object, by Submission ID
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	SubmissionStatus get(String id) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param batch
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	Long getEvaluationIdForBatch(List<SubmissionStatus> batch)
			throws DatastoreException, InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * 
	 * @param batch
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	void update(List<SubmissionStatus> batch)
			throws DatastoreException, InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * Delete a SubmissionStatus object.
	 * 
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void delete(String id) throws DatastoreException, NotFoundException;

	long getCount() throws DatastoreException;

}