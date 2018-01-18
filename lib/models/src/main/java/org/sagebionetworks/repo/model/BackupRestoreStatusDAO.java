package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.web.NotFoundException;


/**
 * DAO for BackupRestoreStatus CRUD.
 * @author John
 *
 */
@Deprecated
public interface BackupRestoreStatusDAO {
	
	/**
	 * Create a new status.
	 * @param status
	 * @return The ID of the newly created status.
	 * @throws DatastoreException 
	 */
	public String create(BackupRestoreStatus status) throws DatastoreException;
	
	/**
	 * @throws NotFoundException 
	 * Get a status object using its ID.
	 * @param id
	 * @return
	 * @throws DatastoreException 
	 * @throws  
	 */
	public BackupRestoreStatus get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Update a status object.
	 * @param status
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void update(BackupRestoreStatus status) throws DatastoreException, NotFoundException;
	
	/**
	 * Delete the status object
	 * @param id
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * @throws NotFoundException 
	 * @throws  
	 * Should a job be forced to terminate?
	 * @param id
	 * @throws DatastoreException 
	 * @throws  
	 */
	public void setForceTermination(String id, boolean terminate) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public boolean shouldJobTerminate(String id) throws DatastoreException, NotFoundException;

}
