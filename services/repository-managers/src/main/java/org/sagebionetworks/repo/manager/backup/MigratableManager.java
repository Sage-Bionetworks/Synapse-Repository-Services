package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstraction for a migratable Manager.
 * @author John
 *
 */
public interface MigratableManager {
	
	/**
	 * Write a Single object to the given stream.
	 * @param idToBackup
	 * @param zos
	 */
	void writeBackupToOutputStream(String idToBackup, OutputStream zos);

	/**
	 * Create or Update an object from a backup stream.
	 * @param zin
	 * @return Must return the ID of object.
	 */
	String createOrUpdateFromBackupStream(InputStream zin);

	/**
	 * Delete an object by its Migratable ID.
	 * @param id
	 */
	void deleteByMigratableId(String id);	

}
