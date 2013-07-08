package org.sagebionetworks.repo.manager.backup;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Acts as a node backup source for creating a backup and a node backup destination
 * for restoring from a backup.
 * 
 * @author jmhill
 *
 */
public interface NodeBackupManager extends NodeBackupSource {

	/**
	 * Clear all data in preparation for the restore.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void clearAllData();

}
