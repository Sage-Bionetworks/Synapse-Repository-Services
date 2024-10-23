package org.sagebionetworks.repo.model.dbo.limits;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOProjectStorageData implements MigratableDatabaseObject<DBOProjectStorageData, DBOProjectStorageData> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("projectId", SqlConstants.COL_PROJECT_STORAGE_DATA_PROJECT_ID, true).withIsBackupId(true),
		new FieldColumn("etag", SqlConstants.COL_PROJECT_STORAGE_DATA_ETAG).withIsEtag(true),
		new FieldColumn("modifiedOn", SqlConstants.COL_PROJECT_STORAGE_DATA_MODIFIED_ON),
		new FieldColumn("runtimeMs", SqlConstants.COL_PROJECT_STORAGE_DATA_RUNTIME_MS),
		new FieldColumn("storageLocationData", SqlConstants.COL_PROJECT_STORAGE_DATA_LOCATION_DATA)
	};
	
	private static final TableMapping<DBOProjectStorageData> TABLE_MAPPING = new TableMapping<>() {

		@Override
		public DBOProjectStorageData mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DBOProjectStorageData()
				.setProjectId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_DATA_PROJECT_ID))
				.setEtag(rs.getString(SqlConstants.COL_PROJECT_STORAGE_DATA_ETAG))
				.setModifiedOn(new Date(rs.getTimestamp(SqlConstants.COL_PROJECT_STORAGE_DATA_MODIFIED_ON).getTime()))
				.setRuntimeMs(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_DATA_RUNTIME_MS))
				.setStorageLocationData(rs.getString(SqlConstants.COL_PROJECT_STORAGE_DATA_LOCATION_DATA));
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_PROJECT_STORAGE_DATA;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_PROJECT_STORAGE_DATA;
		}

		@Override
		public Class<? extends DBOProjectStorageData> getDBOClass() {
			return DBOProjectStorageData.class;
		}
	};
		
	private Long projectId;
	private String etag;
	private Date modifiedOn;
	private Long runtimeMs;
	private String storageLocationData;
	
	public Long getProjectId() {
		return projectId;
	}
	
	public DBOProjectStorageData setProjectId(Long projectId) {
		this.projectId = projectId;
		return this;
	}
	
	public String getEtag() {
		return etag;
	}
	
	public DBOProjectStorageData setEtag(String etag) {
		this.etag = etag;
		return this;
	}
		
	public Date getModifiedOn() {
		return modifiedOn;
	}
	
	public DBOProjectStorageData setModifiedOn(Date modifedOn) {
		this.modifiedOn = modifedOn;
		return this;
	}
	
	public Long getRuntimeMs() {
		return runtimeMs;
	}
	
	public DBOProjectStorageData setRuntimeMs(Long runtimeMs) {
		this.runtimeMs = runtimeMs;
		return this;
	}

	public String getStorageLocationData() {
		return storageLocationData;
	}
	
	public DBOProjectStorageData setStorageLocationData(String storageLocationUsageMap) {
		this.storageLocationData = storageLocationUsageMap;
		return this;
	}
	
	@Override
	public TableMapping<DBOProjectStorageData> getTableMapping() {
		return TABLE_MAPPING;
	}
	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PROJECT_STORAGE_DATA;
	}

	@Override
	public MigratableTableTranslation<DBOProjectStorageData, DBOProjectStorageData> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOProjectStorageData> getBackupClass() {
		return DBOProjectStorageData.class;
	}

	@Override
	public Class<? extends DBOProjectStorageData> getDatabaseObjectClass() {
		return DBOProjectStorageData.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, projectId, runtimeMs, storageLocationData, modifiedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOProjectStorageData)) {
			return false;
		}
		DBOProjectStorageData other = (DBOProjectStorageData) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(projectId, other.projectId) && Objects.equals(runtimeMs, other.runtimeMs)
				&& Objects.equals(storageLocationData, other.storageLocationData) && Objects.equals(modifiedOn, other.modifiedOn);
	}

	@Override
	public String toString() {
		return String.format("DBOProjectStorageData [projectId=%s, etag=%s, modifiedOn=%s, runtimeMs=%s, storageLocationData=%s]", projectId,
				etag, modifiedOn, runtimeMs, storageLocationData);
	}
}
