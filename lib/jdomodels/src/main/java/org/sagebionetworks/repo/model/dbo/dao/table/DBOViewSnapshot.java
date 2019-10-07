package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VIEW_SNAPSHOT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SNAPSHOT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOViewSnapshot implements MigratableDatabaseObject<DBOViewSnapshot, DBOViewSnapshot> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("snapshotId", COL_VIEW_SNAPSHOT_ID, true).withIsBackupId(true),
			new FieldColumn("viewId", COL_VIEW_SNAPSHOT_VIEW_ID),
			new FieldColumn("version", COL_VIEW_SNAPSHOT_VERSION),
			new FieldColumn("createdBy", COL_VIEW_SNAPSHOT_CREATED_BY),
			new FieldColumn("createdOn", COL_VIEW_SNAPSHOT_CREATED_ON),
			new FieldColumn("bucket", COL_VIEW_SNAPSHOT_BUCKET),
			new FieldColumn("key", COL_VIEW_SNAPSHOT_KEY), };

	Long snapshotId;
	Long viewId;
	Long version;
	Long createdBy;
	Timestamp createdOn;
	String bucket;
	String key;	
	
	@Override
	public TableMapping<DBOViewSnapshot> getTableMapping() {
		return new TableMapping<DBOViewSnapshot>() {

			@Override
			public DBOViewSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOViewSnapshot dbo = new DBOViewSnapshot();
				dbo.setSnapshotId(rs.getLong(COL_VIEW_SNAPSHOT_ID));
				dbo.setViewId(rs.getLong(COL_VIEW_SNAPSHOT_VIEW_ID));
				dbo.setVersion(rs.getLong(COL_VIEW_SNAPSHOT_VERSION));
				dbo.setCreatedBy(rs.getLong(COL_VIEW_SNAPSHOT_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_VIEW_SNAPSHOT_CREATED_ON));
				dbo.setBucket(rs.getString(COL_VIEW_SNAPSHOT_BUCKET));
				dbo.setKey(rs.getString(COL_VIEW_SNAPSHOT_KEY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_VIEW_SNAPSHOT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_VIEW_SNAPSHOT;
			}

			@Override
			public Class<? extends DBOViewSnapshot> getDBOClass() {
				return DBOViewSnapshot.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_SNAPSHOT;
	}

	@Override
	public MigratableTableTranslation<DBOViewSnapshot, DBOViewSnapshot> getTranslator() {
		return new BasicMigratableTableTranslation<DBOViewSnapshot>();
	}

	@Override
	public Class<? extends DBOViewSnapshot> getBackupClass() {
		return DBOViewSnapshot.class;
	}

	@Override
	public Class<? extends DBOViewSnapshot> getDatabaseObjectClass() {
		return DBOViewSnapshot.class;
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

	public Long getViewId() {
		return viewId;
	}

	public void setViewId(Long viewId) {
		this.viewId = viewId;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((snapshotId == null) ? 0 : snapshotId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((viewId == null) ? 0 : viewId.hashCode());
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
		DBOViewSnapshot other = (DBOViewSnapshot) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
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
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (snapshotId == null) {
			if (other.snapshotId != null)
				return false;
		} else if (!snapshotId.equals(other.snapshotId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (viewId == null) {
			if (other.viewId != null)
				return false;
		} else if (!viewId.equals(other.viewId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOViewSnapshot [snapshotId=" + snapshotId + ", viewId=" + viewId + ", version=" + version
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", bucket=" + bucket + ", key=" + key + "]";
	}

}
