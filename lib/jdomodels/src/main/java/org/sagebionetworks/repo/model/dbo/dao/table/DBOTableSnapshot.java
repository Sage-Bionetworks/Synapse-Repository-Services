package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_SNAPSHOT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_SNAPSHOT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

public class DBOTableSnapshot implements MigratableDatabaseObject<DBOTableSnapshot, DBOTableSnapshot> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("snapshotId", COL_TABLE_SNAPSHOT_ID, true).withIsBackupId(true),
			new FieldColumn("tableId", COL_TABLE_SNAPSHOT_TABLE_ID),
			new FieldColumn("version", COL_TABLE_SNAPSHOT_VERSION),
			new FieldColumn("createdBy", COL_TABLE_SNAPSHOT_CREATED_BY),
			new FieldColumn("createdOn", COL_TABLE_SNAPSHOT_CREATED_ON),
			new FieldColumn("bucket", COL_TABLE_SNAPSHOT_BUCKET),
			new FieldColumn("key", COL_TABLE_SNAPSHOT_KEY), };
	
	private static final TableMapping<DBOTableSnapshot> TABLE_MAPPING = new TableMapping<DBOTableSnapshot>() {

		@Override
		public DBOTableSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOTableSnapshot dbo = new DBOTableSnapshot();
			dbo.setSnapshotId(rs.getLong(COL_TABLE_SNAPSHOT_ID));
			dbo.setTableId(rs.getLong(COL_TABLE_SNAPSHOT_TABLE_ID));
			dbo.setVersion(rs.getLong(COL_TABLE_SNAPSHOT_VERSION));
			dbo.setCreatedBy(rs.getLong(COL_TABLE_SNAPSHOT_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_TABLE_SNAPSHOT_CREATED_ON));
			dbo.setBucket(rs.getString(COL_TABLE_SNAPSHOT_BUCKET));
			dbo.setKey(rs.getString(COL_TABLE_SNAPSHOT_KEY));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_TABLE_SNAPSHOT;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_TABLE_SNAPSHOT;
		}

		@Override
		public Class<? extends DBOTableSnapshot> getDBOClass() {
			return DBOTableSnapshot.class;
		}
	};

	Long snapshotId;
	Long viewId;
	Long tableId;
	Long version;
	Long createdBy;
	Timestamp createdOn;
	String bucket;
	String key;	
	
	@Override
	public TableMapping<DBOTableSnapshot> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_SNAPSHOT;
	}

	@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "This can be removed with the viewId field once the stack is released")
	@Override
	public MigratableTableTranslation<DBOTableSnapshot, DBOTableSnapshot> getTranslator() {
		return new BasicMigratableTableTranslation<DBOTableSnapshot>() {
			@Override
			public DBOTableSnapshot createDatabaseObjectFromBackup(DBOTableSnapshot backup) {
				// Makes sure to copy the viewId to the tableId field when restoring a backup
				if (backup.getTableId() == null) {
					backup.setTableId(backup.getViewId());
				}
				return super.createDatabaseObjectFromBackup(backup);
			}
		};
	}

	@Override
	public Class<? extends DBOTableSnapshot> getBackupClass() {
		return DBOTableSnapshot.class;
	}

	@Override
	public Class<? extends DBOTableSnapshot> getDatabaseObjectClass() {
		return DBOTableSnapshot.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(Long snapshotId) {
		this.snapshotId = snapshotId;
	}

	@Deprecated
	public Long getViewId() {
		return viewId;
	}

	@Deprecated
	public void setViewId(Long viewId) {
		this.viewId = viewId;
	}
	
	public Long getTableId() {
		return tableId;
	}
	
	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
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

	@Override
	public int hashCode() {
		return Objects.hash(bucket, createdBy, createdOn, key, snapshotId, tableId, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOTableSnapshot)) {
			return false;
		}
		DBOTableSnapshot other = (DBOTableSnapshot) obj;
		return Objects.equals(bucket, other.bucket) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(key, other.key)
				&& Objects.equals(snapshotId, other.snapshotId) && Objects.equals(tableId, other.tableId)
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "DBOTableSnapshot [snapshotId=" + snapshotId + ", tableId=" + tableId + ", version=" + version + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + ", bucket=" + bucket + ", key=" + key + "]";
	}

}
