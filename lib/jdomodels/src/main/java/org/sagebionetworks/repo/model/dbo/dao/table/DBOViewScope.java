package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_CONTAINER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VIEW_SCOPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SCOPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOViewScope  implements MigratableDatabaseObject<DBOViewScope, DBOViewScope> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_VIEW_SCOPE_ID, true).withIsBackupId(true),
		new FieldColumn("viewId", COL_VIEW_SCOPE_VIEW_ID),
		new FieldColumn("containerId", COL_VIEW_SCOPE_CONTAINER_ID),
	};

	@Override
	public TableMapping<DBOViewScope> getTableMapping() {
		return new TableMapping<DBOViewScope>(){

			@Override
			public DBOViewScope mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOViewScope dto = new DBOViewScope();
		
				return dto;
			}

			@Override
			public String getTableName() {
				return TABLE_VIEW_SCOPE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_VIEW_SCOPE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOViewScope> getDBOClass() {
				return DBOViewScope.class;
			}};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_SCOPE;
	}

	@Override
	public MigratableTableTranslation<DBOViewScope, DBOViewScope> getTranslator() {
		return new MigratableTableTranslation<DBOViewScope, DBOViewScope>(){

			@Override
			public DBOViewScope createDatabaseObjectFromBackup(
					DBOViewScope backup) {
				return backup;
			}

			@Override
			public DBOViewScope createBackupFromDatabaseObject(DBOViewScope dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOViewScope> getBackupClass() {
		return DBOViewScope.class;
	}

	@Override
	public Class<? extends DBOViewScope> getDatabaseObjectClass() {
		return DBOViewScope.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
