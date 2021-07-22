package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_KEY_NEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_ROW_CHANGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ROW_CHANGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object for the TableEntity row changes.
 * 
 * @author John
 *
 */
public class DBOTableRowChange implements MigratableDatabaseObject<DBOTableRowChange, DBOTableRowChange> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_TABLE_ROW_ID, true).withIsBackupId(true),
			new FieldColumn("tableId", COL_TABLE_ROW_TABLE_ID),
			new FieldColumn("rowVersion", COL_TABLE_ROW_VERSION),
			new FieldColumn("etag", COL_TABLE_ROW_TABLE_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_TABLE_ROW_CREATED_BY),
			new FieldColumn("createdOn", COL_TABLE_ROW_CREATED_ON),
			new FieldColumn("bucket", COL_TABLE_ROW_BUCKET),
			new FieldColumn("keyNew", COL_TABLE_ROW_KEY_NEW),
			new FieldColumn("rowCount", COL_TABLE_ROW_COUNT),
			new FieldColumn("changeType", COL_TABLE_ROW_TYPE),
			new FieldColumn("transactionId", COL_TABLE_ROW_TRX_ID)
	};
	
	private static final TableMapping<DBOTableRowChange> TABLE_MAPPING = new TableMapping<DBOTableRowChange>() {

		@Override
		public DBOTableRowChange mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOTableRowChange change = new DBOTableRowChange();
			change.setId(rs.getLong(COL_TABLE_ROW_ID));
			if (rs.wasNull()) {
				change.setId(null);
			}
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
	
	private static final MigratableTableTranslation<DBOTableRowChange, DBOTableRowChange> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<DBOTableRowChange>();

	private Long id;
	private Long tableId;
	private String etag;
	private Long rowVersion;
	private String columnIds;
	private Long createdBy;
	private Long createdOn;
	private String bucket;
	private String keyNew;
	private Long rowCount;
	private String changeType;
	private Long transactionId;

	@Override
	public TableMapping<DBOTableRowChange> getTableMapping() {
		return TABLE_MAPPING;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
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
		return MIGRATION_TRANSLATOR;
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
		return Objects.hash(bucket, changeType, columnIds, createdBy, createdOn, etag, id, keyNew, rowCount, rowVersion, tableId,
				transactionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOTableRowChange other = (DBOTableRowChange) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(changeType, other.changeType)
				&& Objects.equals(columnIds, other.columnIds) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(keyNew, other.keyNew) && Objects.equals(rowCount, other.rowCount)
				&& Objects.equals(rowVersion, other.rowVersion) && Objects.equals(tableId, other.tableId)
				&& Objects.equals(transactionId, other.transactionId);
	}

	@Override
	public String toString() {
		return "DBOTableRowChange [id=" + id + ", tableId=" + tableId + ", etag=" + etag + ", rowVersion=" + rowVersion + ", columnIds="
				+ columnIds + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", bucket=" + bucket + ", keyNew="
				+ keyNew + ", rowCount=" + rowCount + ", changeType=" + changeType + ", transactionId=" + transactionId + "]";
	}

}
