package org.sagebionetworks.repo.model.dbo.statistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOProjectStorageUsageCache implements DatabaseObject<DBOProjectStorageUsageCache> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("projectId", SqlConstants.COL_PROJECT_STORAGE_USAGE_PROJECT_ID, true),
		new FieldColumn("updatedOn", SqlConstants.COL_PROJECT_STORAGE_USAGE_UPDATED_ON),
		new FieldColumn("storageLocationData", SqlConstants.COL_PROJECT_STORAGE_USAGE_LOCATION_DATA)
	};
	
	private static final TableMapping<DBOProjectStorageUsageCache> TABLE_MAPPING = new TableMapping<>() {

		@Override
		public DBOProjectStorageUsageCache mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DBOProjectStorageUsageCache()
				.setProjectId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_USAGE_PROJECT_ID))
				.setUpdatedOn(new Date(rs.getTimestamp(SqlConstants.COL_PROJECT_STORAGE_USAGE_UPDATED_ON).getTime()))
				.setStorageLocationData(rs.getString(SqlConstants.COL_PROJECT_STORAGE_USAGE_LOCATION_DATA));
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_PROJECT_STORAGE_USAGE;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_PROJECT_STORAGE_USAGE;
		}

		@Override
		public Class<? extends DBOProjectStorageUsageCache> getDBOClass() {
			return DBOProjectStorageUsageCache.class;
		}
	};
		
	private Long projectId;
	private Date updatedOn;
	private String storageLocationData;
	
	public Long getProjectId() {
		return projectId;
	}
	
	public DBOProjectStorageUsageCache setProjectId(Long projectId) {
		this.projectId = projectId;
		return this;
	}
		
	public Date getUpdatedOn() {
		return updatedOn;
	}
	
	public DBOProjectStorageUsageCache setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
		return this;
	}

	public String getStorageLocationData() {
		return storageLocationData;
	}
	
	public DBOProjectStorageUsageCache setStorageLocationData(String storageLocationUsageMap) {
		this.storageLocationData = storageLocationUsageMap;
		return this;
	}
	
	@Override
	public TableMapping<DBOProjectStorageUsageCache> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		return Objects.hash(projectId, updatedOn, storageLocationData);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOProjectStorageUsageCache)) {
			return false;
		}
		DBOProjectStorageUsageCache other = (DBOProjectStorageUsageCache) obj;
		return Objects.equals(projectId, other.projectId) && Objects.equals(storageLocationData, other.storageLocationData)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return String.format("DBOProjectStorageUsageCache [projectId=%s, updatedOn=%s, storageLocationData=%s]", projectId,
				updatedOn, storageLocationData);
	}

}
