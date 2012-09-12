package org.sagebionetworks.repo.manager.backup.migration;

import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.EntityType;

/**
 * Represents a single step in the migration of a NodeRevisionBackup.
 * @author John
 *
 */
public interface RevisionMigrationStep {

	/**
	 * Migrate a single step, from one version to the next.
	 * @param toMigrate
	 * @param type
	 * @return
	 */
	public EntityType migrateOneStep(NodeRevisionBackup toMigrate,	EntityType type);
}
