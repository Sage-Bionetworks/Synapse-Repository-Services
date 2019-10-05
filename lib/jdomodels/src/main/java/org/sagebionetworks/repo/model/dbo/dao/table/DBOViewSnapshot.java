package org.sagebionetworks.repo.model.dbo.dao.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOViewSnapshot implements MigratableDatabaseObject<DBOViewSnapshot, DBOViewSnapshot> {

	@Override
	public TableMapping<DBOViewSnapshot> getTableMapping() {
		return new TableMapping<DBOViewSnapshot>() {
			
			@Override
			public DBOViewSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getTableName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getDDLFileName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Class<? extends DBOViewSnapshot> getDBOClass() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_SNAPSHOT;
	}

	@Override
	public MigratableTableTranslation<DBOViewSnapshot, DBOViewSnapshot> getTranslator() {
		return new BasicMigratableTableTranslation<DBOViewSnapshot>();
	}

	@Override
	public Class<? extends DBOViewSnapshot> getBackupClass() {
		return DBOViewSnapshot.class;
	}

	@Override
	public Class<? extends DBOViewSnapshot> getDatabaseObjectClass() {
		return DBOViewSnapshot.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
