package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for upload daemon status.
 * 
 * @author John
 *
 */
public interface UploadDaemonStatusDao {

	/**
	 * Create a new status.
	 * @param status
	 * @return
	 */
	public UploadDaemonStatus create(UploadDaemonStatus status);
	
	/**
	 * Get the existing status
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public UploadDaemonStatus get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Delete a given status
	 * @param id
	 */
	public void delete(String id);
	
	/**
	 * Update the given status
	 * 
	 * @param status
	 * @return 
	 */
	public boolean update(UploadDaemonStatus status);
}
