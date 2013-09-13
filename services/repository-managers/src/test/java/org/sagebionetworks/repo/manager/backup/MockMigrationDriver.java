package org.sagebionetworks.repo.manager.backup;

import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;

public class MockMigrationDriver implements MigrationDriver {

	@Override
	public EntityType migrateToCurrentVersion(NodeRevisionBackup toMigrate,
			EntityType type) {
		// TODO Auto-generated method stub
		return type;
	}
}
