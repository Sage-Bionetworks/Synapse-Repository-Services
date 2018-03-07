package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * A migration listener can listen to migration events as migration proceeds.
 * 
 * @author jmhill
 *
 */
public interface MigrationTypeListener {

	/**
	 * Will be called when a batch of database objects are created or updated during migration.
	 * 
	 * This method will be called AFTER the passed list of database objects has been sent to the database.
	 * 
	 * @param delta
	 */
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta);

}
