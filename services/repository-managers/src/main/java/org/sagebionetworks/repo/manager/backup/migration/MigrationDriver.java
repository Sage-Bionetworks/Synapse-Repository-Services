package org.sagebionetworks.repo.manager.backup.migration;

import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.EntityType;

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
	public EntityType migrateToCurrentVersion(NodeRevisionBackup toMigrate, EntityType type);
	
	
	
	public void migrateNodePrincipals(NodeBackup nodeBackup);

}
