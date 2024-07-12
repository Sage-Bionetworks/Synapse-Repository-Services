package org.sagebionetworks.repo.model.dbo.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class StubAutoIncrement implements MigratableDatabaseObject<StubAutoIncrement, StubAutoIncrement>, AutoIncrementDatabaseObject<StubAutoIncrement>{
	
	@Override
	public TableMapping<StubAutoIncrement> getTableMapping() {
		return new TableMapping<StubAutoIncrement>() {
			@Override
			public String getTableName() {
				return MigratableTableDAOImplUnitTest.STUB_TABLE_NAME;
			}

			@Override
			public String getDDLFileName() {
				return null;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return new FieldColumn[0];
			}

			@Override
			public Class<? extends StubAutoIncrement> getDBOClass() {
				return null;
			}

			@Override
			public StubAutoIncrement mapRow(ResultSet resultSet, int i) throws SQLException {
				return null;
			}
		};
	}

	@Override
	public Long getId() {
		return null;
	}

	@Override
	public void setId(Long id) { }

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_SCOPE;
	}

	@Override
	public MigratableTableTranslation<StubAutoIncrement, StubAutoIncrement> getTranslator() {
		return null;
	}

	@Override
	public Class<? extends StubAutoIncrement> getBackupClass() {
		return StubAutoIncrement.class;
	}

	@Override
	public Class<? extends StubAutoIncrement> getDatabaseObjectClass() {
		return null;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	
}