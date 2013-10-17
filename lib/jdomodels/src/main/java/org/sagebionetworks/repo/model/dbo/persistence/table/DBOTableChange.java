package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_CHANGE_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_FILE_HAND_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TABLE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_CHANGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_CHANGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object fo the TableChange table.
 * @author John
 *
 */
public class DBOTableChange implements MigratableDatabaseObject<DBOTableChange, DBOTableChange> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("changeNumber", COL_TABLE_CHANGE_NUMBER, true).withIsBackupId(true),
		new FieldColumn("tableId", COL_TABLE_TABLE_ID),
		new FieldColumn("tableVersionNumber", COL_TABLE_TABLE_VERSION),
		new FieldColumn("fileHandleId", COL_TABLE_FILE_HAND_ID),
	};
	
	private Long changeNumber;
	private Long tableId;
	private Long tableVersionNumber;
	private Long fileHandleId;
	
	@Override
	public TableMapping<DBOTableChange> getTableMapping() {
		return new TableMapping<DBOTableChange>() {

			@Override
			public DBOTableChange mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOTableChange change = new DBOTableChange();
				change.setChangeNumber(rs.getLong(COL_TABLE_CHANGE_NUMBER));
				change.setTableId(rs.getLong(COL_TABLE_TABLE_ID));
				change.setTableVersionNumber(rs.getLong(COL_TABLE_TABLE_VERSION));
				change.setFileHandleId(rs.getLong(COL_TABLE_FILE_HAND_ID));
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_TABLE_CHANGE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TABLE_CHANGE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTableChange> getDBOClass() {
				return DBOTableChange.class;
			}
		};
	}

	public Long getChangeNumber() {
		return changeNumber;
	}

	public void setChangeNumber(Long changeNumber) {
		this.changeNumber = changeNumber;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getTableVersionNumber() {
		return tableVersionNumber;
	}

	public void setTableVersionNumber(Long tableVersionNumber) {
		this.tableVersionNumber = tableVersionNumber;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return null;
	}

	@Override
	public MigratableTableTranslation<DBOTableChange, DBOTableChange> getTranslator() {
		return new MigratableTableTranslation<DBOTableChange, DBOTableChange>() {
			
			@Override
			public DBOTableChange createDatabaseObjectFromBackup(DBOTableChange backup) {
				return backup;
			}
			
			@Override
			public DBOTableChange createBackupFromDatabaseObject(DBOTableChange dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOTableChange> getBackupClass() {
		return DBOTableChange.class;
	}

	@Override
	public Class<? extends DBOTableChange> getDatabaseObjectClass() {
		return DBOTableChange.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}
