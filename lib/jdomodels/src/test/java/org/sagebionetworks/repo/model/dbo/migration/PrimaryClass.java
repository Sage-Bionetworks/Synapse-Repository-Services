package org.sagebionetworks.repo.model.dbo.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class PrimaryClass implements MigratableDatabaseObject<PrimaryClass, PrimaryClass> {

	@Override
	public TableMapping<PrimaryClass> getTableMapping() {
		return new TableMapping<PrimaryClass>() {

			@Override
			public PrimaryClass mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}

			@Override
			public String getTableName() {
				return "MESSAGE_CONTENT";
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
			public Class<? extends PrimaryClass> getDBOClass() {
				return null;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_CONTENT;
	}

	@Override
	public MigratableTableTranslation<PrimaryClass, PrimaryClass> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends PrimaryClass> getBackupClass() {
		return PrimaryClass.class;
	}

	@Override
	public Class<? extends PrimaryClass> getDatabaseObjectClass() {
		return PrimaryClass.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Arrays.asList(new SecondaryClass());
	}
	
}