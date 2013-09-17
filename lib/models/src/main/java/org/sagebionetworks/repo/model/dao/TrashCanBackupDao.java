package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;

/**
 * Operations for backup and data migration on the trash can table.
 *
 * @author Eric Wu
 */
public interface TrashCanBackupDao {

	/**
	 * Gets the trashed entity given its ID.
	 */
	TrashedEntity get(String entityId) throws DatastoreException;

	/**
	 * Deletes the trashed entity from the trash can table.
	 */
	void delete(String entityId) throws DatastoreException;

	/**
	 * Updates the trashed entity from backup. If the entity
	 * does not exist, it will be created.
	 */
	void update(TrashedEntity entity) throws DatastoreException;

	long getCount() throws DatastoreException;
}
