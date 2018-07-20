package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_COLUMN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_BOUND_COLUMN_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_ORDINAL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object that binds an Object (entity) to a ColumnModel.
 * @author John
 *
 */
public class DBOBoundColumnOrdinal implements MigratableDatabaseObject<DBOBoundColumnOrdinal, DBOBoundColumnOrdinal>{
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("columnId", COL_BOUND_CM_ORD_COLUMN_ID, true),
		new FieldColumn("objectId", COL_BOUND_CM_ORD_OBJECT_ID, true).withIsBackupId(true),
		new FieldColumn("ordinal", COL_BOUND_CM_ORD_ORDINAL),
	};
	
	Long columnId;
	Long objectId;
	Long ordinal;

	@Override
	public TableMapping<DBOBoundColumnOrdinal> getTableMapping() {
		return new TableMapping<DBOBoundColumnOrdinal>() {
			@Override
			public DBOBoundColumnOrdinal mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOBoundColumnOrdinal dbo = new DBOBoundColumnOrdinal();
				dbo.setColumnId(rs.getLong(COL_BOUND_CM_ORD_COLUMN_ID));
				dbo.setObjectId(rs.getLong(COL_BOUND_CM_ORD_OBJECT_ID));
				dbo.setOrdinal(rs.getLong(COL_BOUND_CM_ORD_ORDINAL));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_BOUND_COLUMN_ORDINAL;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_BOUND_COLUMN_ORDINAL;
			}
			
			@Override
			public Class<? extends DBOBoundColumnOrdinal> getDBOClass() {
				return DBOBoundColumnOrdinal.class;
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

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.BOUND_COLUMN_ORDINAL;
	}

	@Override
	public MigratableTableTranslation<DBOBoundColumnOrdinal, DBOBoundColumnOrdinal> getTranslator() {
		return new BasicMigratableTableTranslation<DBOBoundColumnOrdinal>();
	}

	@Override
	public Class<? extends DBOBoundColumnOrdinal> getBackupClass() {
		return DBOBoundColumnOrdinal.class;
	}

	@Override
	public Class<? extends DBOBoundColumnOrdinal> getDatabaseObjectClass() {
		return DBOBoundColumnOrdinal.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	public Long getOrdinal() {
		return ordinal;
	}

	public void setOrdinal(Long ordinal) {
		this.ordinal = ordinal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnId == null) ? 0 : columnId.hashCode());
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((ordinal == null) ? 0 : ordinal.hashCode());
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
		DBOBoundColumnOrdinal other = (DBOBoundColumnOrdinal) obj;
		if (columnId == null) {
			if (other.columnId != null)
				return false;
		} else if (!columnId.equals(other.columnId))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (ordinal == null) {
			if (other.ordinal != null)
				return false;
		} else if (!ordinal.equals(other.ordinal))
			return false;
		return true;
	}


}
