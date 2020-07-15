package org.sagebionetworks.repo.model.dbo.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class SecondaryClass implements MigratableDatabaseObject<SecondaryClass, SecondaryClass> {

	@Override
	public TableMapping<SecondaryClass> getTableMapping() {
		return new TableMapping<SecondaryClass>() {

			@Override
			public SecondaryClass mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}

			@Override
			public String getTableName() {
				return "MESSAGE_TO_USER";
			}

			@Override
			public String getDDLFileName() {
				return null;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return null;
			}

			@Override
			public Class<? extends SecondaryClass> getDBOClass() {
				return SecondaryClass.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_TO_USER;
	}

	@Override
	public MigratableTableTranslation<SecondaryClass, SecondaryClass> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends SecondaryClass> getBackupClass() {
		return SecondaryClass.class;
	}

	@Override
	public Class<? extends SecondaryClass> getDatabaseObjectClass() {
		return SecondaryClass.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
