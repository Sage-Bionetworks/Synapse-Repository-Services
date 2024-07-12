package org.sagebionetworks.repo.model.dbo.migration;

import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

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
	 * 
	 * @param type
	 * @return
	 */
	MigratableDatabaseObject getObjectForType(MigrationType type);

	void writeObjects(BackupAliasType backupAliasType, MigrationType currentType, List<?> backupObjects, Writer writer);

	<B> Optional<List<B>> readObjects(Class<? extends B> clazz, BackupAliasType backupAliasType, InputStream input,
			MigrationFileType fileType);

}
