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

public class DBOProjectStorageLimit implements MigratableDatabaseObject<DBOProjectStorageLimit, DBOProjectStorageLimit> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", SqlConstants.COL_PROJECT_STORAGE_LIMIT_ID, true).withIsBackupId(true),
		new FieldColumn("etag", SqlConstants.COL_PROJECT_STORAGE_LIMIT_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", SqlConstants.COL_PROJECT_STORAGE_LIMIT_CREATED_BY),
		new FieldColumn("createdOn", SqlConstants.COL_PROJECT_STORAGE_LIMIT_CREATED_ON),
		new FieldColumn("modifiedBy", SqlConstants.COL_PROJECT_STORAGE_LIMIT_MODIFIED_BY),
		new FieldColumn("modifiedOn", SqlConstants.COL_PROJECT_STORAGE_LIMIT_MODIFIED_ON),
		new FieldColumn("projectId", SqlConstants.COL_PROJECT_STORAGE_LIMIT_PROJECT_ID),
		new FieldColumn("storageLocationId", SqlConstants.COL_PROJECT_STORAGE_LIMIT_LOCATION_ID),
		new FieldColumn("maxBytes", SqlConstants.COL_PROJECT_STORAGE_LIMIT_MAX_BYTES)
	};
	
	private Long id;
	private String etag;
	private Long createdBy;
	private Date createdOn;
	private Long modifiedBy;
	private Date modifiedOn;
	private Long projectId;
	private Long storageLocationId;
	private Long maxBytes;
	
	public Long getId() {
		return id;
	}
	
	public DBOProjectStorageLimit setId(Long id) {
		this.id = id;
		return this;
	}	

	public String getEtag() {
		return etag;
	}

	public DBOProjectStorageLimit setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOProjectStorageLimit setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public DBOProjectStorageLimit setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOProjectStorageLimit setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public DBOProjectStorageLimit setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public Long getProjectId() {
		return projectId;
	}

	public DBOProjectStorageLimit setProjectId(Long projectId) {
		this.projectId = projectId;
		return this;
	}

	public Long getStorageLocationId() {
		return storageLocationId;
	}

	public DBOProjectStorageLimit setStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
		return this;
	}

	public Long getMaxBytes() {
		return maxBytes;
	}

	public DBOProjectStorageLimit setMaxBytes(Long maxBytes) {
		this.maxBytes = maxBytes;
		return this;
	}

	@Override
	public TableMapping<DBOProjectStorageLimit> getTableMapping() {
		return new TableMapping<DBOProjectStorageLimit>() {
			
			@Override
			public DBOProjectStorageLimit mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOProjectStorageLimit()
					.setId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_LIMIT_ID))
					.setEtag(rs.getString(SqlConstants.COL_PROJECT_STORAGE_LIMIT_ETAG))
					.setCreatedBy(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_LIMIT_CREATED_BY))
					.setCreatedOn(new Date(rs.getTimestamp(SqlConstants.COL_PROJECT_STORAGE_LIMIT_CREATED_ON).getTime()))
					.setModifiedBy(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_LIMIT_MODIFIED_BY))
					.setModifiedOn(new Date(rs.getTimestamp(SqlConstants.COL_PROJECT_STORAGE_LIMIT_MODIFIED_ON).getTime()))
					.setProjectId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_LIMIT_PROJECT_ID))
					.setStorageLocationId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_LIMIT_LOCATION_ID))
					.setMaxBytes(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_LIMIT_MAX_BYTES));
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_PROJECT_STORAGE_LIMIT;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_PROJECT_STORAGE_LIMIT;
			}
			
			@Override
			public Class<? extends DBOProjectStorageLimit> getDBOClass() {
				return DBOProjectStorageLimit.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PROJECT_STORAGE_LIMIT;
	}

	@Override
	public MigratableTableTranslation<DBOProjectStorageLimit, DBOProjectStorageLimit> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOProjectStorageLimit> getBackupClass() {
		return DBOProjectStorageLimit.class;
	}

	@Override
	public Class<? extends DBOProjectStorageLimit> getDatabaseObjectClass() {
		return DBOProjectStorageLimit.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, etag, id, maxBytes, modifiedBy, modifiedOn, projectId, storageLocationId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOProjectStorageLimit)) {
			return false;
		}
		DBOProjectStorageLimit other = (DBOProjectStorageLimit) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(maxBytes, other.maxBytes) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(projectId, other.projectId)
				&& Objects.equals(storageLocationId, other.storageLocationId);
	}

	@Override
	public String toString() {
		return String.format(
				"DBOProjectStorageLimit [id=%s, projectId=%s, etag=%s, createdBy=%s, createdOn=%s, modifiedBy=%s, modifiedOn=%s, storageLocationId=%s, maxBytes=%s]",
				id, projectId, etag, createdBy, createdOn, modifiedBy, modifiedOn, storageLocationId, maxBytes);
	}

}
