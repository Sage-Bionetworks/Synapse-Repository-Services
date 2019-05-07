package org.sagebionetworks.repo.model.dbo.migration;

import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Abstraction to lookup MigratableDatabaseObjects given MigrationType.
 *
 */
public interface MigrationTypeProvider {

	/**
	 * Lookup MigratableDatabaseObject associated with the given type.
	 * @param type
	 * @return
	 */
	public MigratableDatabaseObject getObjectForType(MigrationType type);

	public UnmodifiableXStream getXStream(BackupAliasType backupAliasType);
}
