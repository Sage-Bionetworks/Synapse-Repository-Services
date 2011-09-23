package org.sagebionetworks.repo.manager.backup.migration;

import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * Drives the migration of objects from older versions to the current.
 * 
 * @author John
 *
 */
public interface MigrationDriver {
	
	/**
	 * Migrate a NodeRevision to the current revision.
	 * @param toMigrate
	 * @param type
	 * @return
	 */
	public NodeRevision migrateToCurrentVersion(NodeRevision toMigrate, ObjectType type);

}
