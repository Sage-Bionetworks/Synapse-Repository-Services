package org.sagebionetworks.repo.model.dbo.migration;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;

/**
 * Basic MigratableTableTranslation can be used when both the database object and backup object
 * are of the same type and no real translation is needed.
 * @param <D>
 */
public class BasicMigratableTableTranslation <D extends DatabaseObject<?>> implements MigratableTableTranslation<D, D> {


	@Override
	public D createDatabaseObjectFromBackup(D backup) {
		return backup;
	}

	@Override
	public D createBackupFromDatabaseObject(D dbo) {
		return dbo;
	}

}
