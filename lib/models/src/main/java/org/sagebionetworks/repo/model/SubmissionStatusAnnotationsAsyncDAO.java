package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;


public interface SubmissionStatusAnnotationsAsyncDAO {
	

	/**
	 * Called when a SubmissionStatus is updated. 
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 */
	public boolean updateSubmissionStatus(String id) throws NotFoundException, DatastoreException, JSONObjectAdapterException;

	/**
	 * Called when a Submission is deleted.
	 * @param id
	 * @return
	 */
	public boolean deleteSubmission(String id);

}
