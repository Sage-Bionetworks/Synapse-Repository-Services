package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;


public interface SubmissionStatusAnnotationsAsyncManager {
	

	/**
	 * Update the query tables with any Annotations included in the created SubmissionStatus.
	 * 
	 * @param subId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws JSONObjectAdapterException
	 */
	public void createSubmissionStatus(String subId) 
			throws NotFoundException, DatastoreException, JSONObjectAdapterException;

	/**
	 * Update the query tables with any Annotations included in the updated SubmissionStatus.
	 * 
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 */
	public void updateSubmissionStatus(String id) 
			throws NotFoundException, DatastoreException, JSONObjectAdapterException;

	/**
	 * Delete all entries in the query tables for the deleted SubmissionStatus.
	 * 
	 * @param id
	 * @return
	 */
	public void deleteSubmission(String id);

}
