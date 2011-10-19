package org.sagebionetworks.repo.manager.backup.migration;

import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * Drives the migration of objects from older versions to the current.
 * 
 * @author John
 *
 */
public interface MigrationDriver {
	
	/**
	 * Migrate a NodeRevisionBackup to the current revision.
	 * @param toMigrate
	 * @param type
	 * @return
	 */
	public NodeRevisionBackup migrateToCurrentVersion(NodeRevisionBackup toMigrate, ObjectType type);

}
