package org.sagebionetworks.evaluation.dao;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionFileHandleDAO {

	/**
	 * Create a new SubmissionFileHandle entry
	 * 
	 * @param submissionId
	 * @param fileHandleId
	 */
	public void create(String submissionId, String fileHandleId);

	/**
	 * Get the total number of file SubmissionFileHandles stored in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Get all the FileHandle IDs associated with a given Submission
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<String> getAllBySubmission(String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Delete a SubmissionFileHandle entry
	 * 
	 * @param submissionId
	 * @param fileHandleId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String submissionId, String fileHandleId)
			throws DatastoreException, NotFoundException;

}