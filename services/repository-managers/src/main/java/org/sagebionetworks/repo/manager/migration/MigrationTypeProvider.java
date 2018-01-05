package org.sagebionetworks.repo.manager.migration;

import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;

public interface MigrationTypeProvider {

	/**
	 * Lookup MigratableDatabaseObject associated with the given type.
	 * @param type
	 * @return
	 */
	public MigratableDatabaseObject getObjectForType(MigrationType type);
}
