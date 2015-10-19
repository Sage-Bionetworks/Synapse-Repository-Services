package org.sagebionetworks.repo.model.evaluation;

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
	public String create(SubmissionStatus dto)
			throws DatastoreException;

	/**
	 * Get a SubmissionStatus object, by Submission ID
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public SubmissionStatus get(String id) throws DatastoreException,
			NotFoundException;
	
	/**
	 * List the SubmissionStatus objects for the given ids.
	 * Any invalid IDs are skipped.
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 */
	public List<SubmissionStatus> list(List<String> ids) throws DatastoreException;

	/**
	 * 
	 * @param batch
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public Long getEvaluationIdForBatch(List<SubmissionStatus> batch) throws DatastoreException,
				InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * 
	 * @param batch
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public void update(List<SubmissionStatus> batch) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * Delete a SubmissionStatus object.
	 * 
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	long getCount() throws DatastoreException;

}