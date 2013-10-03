package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_IS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_OBJECT_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object that binds an Object (entity) to a ColumnModel.
 * @author John
 *
 */
public class DBOBoundColumn implements MigratableDatabaseObject<DBOBoundColumn, DBOBoundColumn>{
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("columnId", COL_BOUND_CM_COLUMN_ID, true).withIsBackupId(true),
		new FieldColumn("objectId", COL_BOUND_CM_OBJECT_ID, true),
		new FieldColumn("isCurrent", COL_BOUND_CM_IS_CURRENT),
	};
	
	Long columnId;
	Long objectId;
	Boolean isCurrent;

	@Override
	public TableMapping<DBOBoundColumn> getTableMapping() {
		return new TableMapping<DBOBoundColumn>() {
			@Override
			public DBOBoundColumn mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOBoundColumn dbo = new DBOBoundColumn();
				dbo.setColumnId(rs.getLong(COL_BOUND_CM_COLUMN_ID));
				dbo.setObjectId(rs.getLong(COL_BOUND_CM_OBJECT_ID));
				dbo.setIsCurrent(rs.getBoolean(COL_BOUND_CM_IS_CURRENT));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_BOUND_COLUMN;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_BOUND_COLUMN;
			}
			
			@Override
			public Class<? extends DBOBoundColumn> getDBOClass() {
				return DBOBoundColumn.class;
			}
		};
	}

	public Long getColumnId() {
		return columnId;
	}

	public void setColumnId(Long columnId) {
		this.columnId = columnId;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public Boolean getIsCurrent() {
		return isCurrent;
	}

	public void setIsCurrent(Boolean isCurrent) {
		this.isCurrent = isCurrent;
	}

	@Override
	public MigrationType getMigratableTableType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MigratableTableTranslation<DBOBoundColumn, DBOBoundColumn> getTranslator() {
		return new MigratableTableTranslation<DBOBoundColumn, DBOBoundColumn>() {

			@Override
			public DBOBoundColumn createDatabaseObjectFromBackup(
					DBOBoundColumn backup) {
				return backup;
			}

			@Override
			public DBOBoundColumn createBackupFromDatabaseObject(
					DBOBoundColumn dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOBoundColumn> getBackupClass() {
		return DBOBoundColumn.class;
	}

	@Override
	public Class<? extends DBOBoundColumn> getDatabaseObjectClass() {
		return DBOBoundColumn.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnId == null) ? 0 : columnId.hashCode());
		result = prime * result
				+ ((isCurrent == null) ? 0 : isCurrent.hashCode());
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOBoundColumn other = (DBOBoundColumn) obj;
		if (columnId == null) {
			if (other.columnId != null)
				return false;
		} else if (!columnId.equals(other.columnId))
			return false;
		if (isCurrent == null) {
			if (other.isCurrent != null)
				return false;
		} else if (!isCurrent.equals(other.isCurrent))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		return true;
	}

}
