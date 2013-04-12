package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.doi.Doi;

/**
 * DAO for DOI data migration only.
 */
public interface DoiMigratableDao extends MigratableDAO {

	/**
	 * Gets the DOI backup object.
	 */
	Doi get(String id) throws DatastoreException;

	/**
	 * Creates or updates, if it already exists, from the backup object.
	 */
	boolean createOrUpdate(Doi backup) throws DatastoreException;

	/**
	 * Deletes by ID.
	 */
	void delete(String id) throws DatastoreException;
}
