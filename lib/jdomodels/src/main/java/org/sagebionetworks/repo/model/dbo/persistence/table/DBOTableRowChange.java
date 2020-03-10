package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_KEY_NEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_ROW_CHANGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.table.TableChangeType;

/**
 * Database object for the TableEntity row changes.
 * 
 * @author John
 *
 */
public class DBOTableRowChange implements MigratableDatabaseObject<DBOTableRowChange, DBOTableRowChange> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("tableId", COL_TABLE_ROW_TABLE_ID, true).withIsBackupId(true),
			new FieldColumn("rowVersion", COL_TABLE_ROW_VERSION, true),
			new FieldColumn("etag", COL_TABLE_ROW_TABLE_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_TABLE_ROW_CREATED_BY),
			new FieldColumn("createdOn", COL_TABLE_ROW_CREATED_ON),
			new FieldColumn("bucket", COL_TABLE_ROW_BUCKET),
			new FieldColumn("keyNew", COL_TABLE_ROW_KEY_NEW),
			new FieldColumn("rowCount", COL_TABLE_ROW_COUNT),
			new FieldColumn("changeType", COL_TABLE_ROW_TYPE),
			new FieldColumn("transactionId", COL_TABLE_ROW_TRX_ID),};

	private Long tableId;
	private String etag;
	private Long rowVersion;
	private String columnIds;
	private Long createdBy;
	private Long createdOn;
	private String bucket;
	private String key;
	private String keyNew;
	private Long rowCount;
	private String changeType;
	private Long transactionId;

	@Override
	public TableMapping<DBOTableRowChange> getTableMapping() {
		return new TableMapping<DBOTableRowChange>() {

			@Override
			public DBOTableRowChange mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTableRowChange change = new DBOTableRowChange();
				change.setTableId(rs.getLong(COL_TABLE_ROW_TABLE_ID));
				change.setRowVersion(rs.getLong(COL_TABLE_ROW_VERSION));
				change.setEtag(rs.getString(COL_TABLE_ROW_TABLE_ETAG));
				change.setCreatedBy(rs.getLong(COL_TABLE_ROW_CREATED_BY));
				change.setCreatedOn(rs.getLong(COL_TABLE_ROW_CREATED_ON));
				change.setBucket(rs.getString(COL_TABLE_ROW_BUCKET));
				change.setKeyNew(rs.getString(COL_TABLE_ROW_KEY_NEW));
				change.setRowCount(rs.getLong(COL_TABLE_ROW_COUNT));
				change.setChangeType(rs.getString(COL_TABLE_ROW_TYPE));
				long transactionId = rs.getLong(COL_TABLE_ROW_TRX_ID);
				if(!rs.wasNull()) {
					change.setTransactionId(transactionId);
				}
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_ROW_CHANGE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TABLE_ROW_CHANGE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTableRowChange> getDBOClass() {
				return DBOTableRowChange.class;
			}
		};
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public Long getRowCount() {
		return rowCount;
	}

	public void setRowCount(Long rowCount) {
		this.rowCount = rowCount;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getRowVersion() {
		return rowVersion;
	}

	public void setRowVersion(Long rowVersion) {
		this.rowVersion = rowVersion;
	}

	public String getColumnIds() {
		return columnIds;
	}

	public void setColumnIds(String columnIds) {
		this.columnIds = columnIds;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	/**
	 * This new key will replace old key.
	 * 
	 * @return
	 */
	public String getKeyNew() {
		return keyNew;
	}

	/**
	 * This new key will replace the old key.
	 * 
	 * @param keyNew
	 */
	public void setKeyNew(String keyNew) {
		this.keyNew = keyNew;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TABLE_CHANGE;
	}

	@Override
	public MigratableTableTranslation<DBOTableRowChange, DBOTableRowChange> getTranslator() {
		return new MigratableTableTranslation<DBOTableRowChange, DBOTableRowChange>() {

			@Override
			public DBOTableRowChange createDatabaseObjectFromBackup(DBOTableRowChange backup) {
				if (TableChangeType.COLUMN.equals(TableChangeType.valueOf(backup.getChangeType()))) {
					if (backup.getKeyNew() == null) {
						if (backup.getKey() == null) {
							throw new IllegalArgumentException("Column change missing both key and keyNew.");
						} else {
							backup.setKeyNew(backup.getKey());
						}
					}
				}
				return backup;
			}

			@Override
			public DBOTableRowChange createBackupFromDatabaseObject(DBOTableRowChange dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOTableRowChange> getBackupClass() {
		return DBOTableRowChange.class;
	}

	@Override
	public Class<? extends DBOTableRowChange> getDatabaseObjectClass() {
		return DBOTableRowChange.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result + ((columnIds == null) ? 0 : columnIds.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((keyNew == null) ? 0 : keyNew.hashCode());
		result = prime * result + ((rowCount == null) ? 0 : rowCount.hashCode());
		result = prime * result + ((rowVersion == null) ? 0 : rowVersion.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
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
		DBOTableRowChange other = (DBOTableRowChange) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (changeType == null) {
			if (other.changeType != null)
				return false;
		} else if (!changeType.equals(other.changeType))
			return false;
		if (columnIds == null) {
			if (other.columnIds != null)
				return false;
		} else if (!columnIds.equals(other.columnIds))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (keyNew == null) {
			if (other.keyNew != null)
				return false;
		} else if (!keyNew.equals(other.keyNew))
			return false;
		if (rowCount == null) {
			if (other.rowCount != null)
				return false;
		} else if (!rowCount.equals(other.rowCount))
			return false;
		if (rowVersion == null) {
			if (other.rowVersion != null)
				return false;
		} else if (!rowVersion.equals(other.rowVersion))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		if (transactionId == null) {
			if (other.transactionId != null)
				return false;
		} else if (!transactionId.equals(other.transactionId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOTableRowChange [tableId=" + tableId + ", etag=" + etag + ", rowVersion=" + rowVersion
				+ ", columnIds=" + columnIds + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", bucket="
				+ bucket + ", key=" + key + ", keyNew=" + keyNew + ", rowCount=" + rowCount + ", changeType="
				+ changeType + ", transactionId=" + transactionId + "]";
	}

}
