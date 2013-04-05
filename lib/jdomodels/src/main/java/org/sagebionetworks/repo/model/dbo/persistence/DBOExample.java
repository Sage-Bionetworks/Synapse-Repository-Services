package org.sagebionetworks.repo.model.dbo.persistence;

import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigratableTableType;

public class DBOExample implements MigratableDatabaseObject<DBOExample, ExampleBackup> {

	@Override
	public TableMapping<DBOExample> getTableMapping() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DBOExample createDatabaseObjectFromBackup(ExampleBackup backup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExampleBackup createBackupFromDatabaseObject(DBOExample dbo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MigratableTableType getMigratableTableType() {
		return MigratableTableType.EXAMPLE;
	}

}
