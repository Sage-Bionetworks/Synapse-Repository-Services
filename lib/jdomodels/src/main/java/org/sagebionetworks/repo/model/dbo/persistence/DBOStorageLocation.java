package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DATA_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_UPLOAD_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_STORAGE_LOCATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.TemporaryCode;

/**
 * descriptor of what's in a column of a participant data record
 */
public class DBOStorageLocation implements MigratableDatabaseObject<DBOStorageLocation, DBOStorageLocation> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_STORAGE_LOCATION_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("description", COL_STORAGE_LOCATION_DESCRIPTION),
			new FieldColumn("uploadType", COL_STORAGE_LOCATION_UPLOAD_TYPE),
			new FieldColumn("etag", COL_PROJECT_SETTING_ETAG).withIsEtag(true),
			new FieldColumn("json", COL_STORAGE_LOCATION_JSON),
			new FieldColumn("dataHash", COL_STORAGE_LOCATION_DATA_HASH),
			new FieldColumn("createdBy", COL_STORAGE_LOCATION_CREATED_BY),
			new FieldColumn("createdOn", COL_STORAGE_LOCATION_CREATED_ON), };

	private Long id;
	private String description;
	private String uploadType;
	private String etag;
	@TemporaryCode(author = "john", comment = "replaced date with json")
	private StorageLocationSetting data;
	private String json;
	private String dataHash;
	private Long createdBy;
	private Date createdOn;

	@Override
	public TableMapping<DBOStorageLocation> getTableMapping() {
		return new TableMapping<DBOStorageLocation>() {

			@Override
			public DBOStorageLocation mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOStorageLocation dbo = new DBOStorageLocation();
				dbo.setId(rs.getLong(COL_STORAGE_LOCATION_ID));
				dbo.setDescription(rs.getString(COL_STORAGE_LOCATION_DESCRIPTION));
				dbo.setUploadType(rs.getString(COL_STORAGE_LOCATION_UPLOAD_TYPE));
				dbo.setEtag(rs.getString(COL_PROJECT_SETTING_ETAG));
				dbo.setJson(rs.getString(COL_PROJECT_SETTING_JSON));
				dbo.setDataHash(rs.getString(COL_STORAGE_LOCATION_DATA_HASH));
				dbo.setCreatedBy(rs.getLong(COL_STORAGE_LOCATION_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_STORAGE_LOCATION_CREATED_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_STORAGE_LOCATION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_STORAGE_LOCATION;
			}

			@Override
			public Class<? extends DBOStorageLocation> getDBOClass() {
				return DBOStorageLocation.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.STORAGE_LOCATION;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUploadType() {
		return uploadType;
	}

	public void setUploadType(String uploadType) {
		this.uploadType = uploadType;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	private StorageLocationSetting getData() {
		return data;
	}

	private void setData(StorageLocationSetting data) {
		this.data = data;
	}

	public String getDataHash() {
		return dataHash;
	}

	public void setDataHash(String dataHash) {
		this.dataHash = dataHash;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, data, dataHash, description, etag, id, json, uploadType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOStorageLocation other = (DBOStorageLocation) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(data, other.data) && Objects.equals(dataHash, other.dataHash)
				&& Objects.equals(description, other.description) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(json, other.json)
				&& Objects.equals(uploadType, other.uploadType);
	}

	@Override
	public String toString() {
		return "DBOStorageLocation [id=" + id + ", description=" + description + ", uploadType=" + uploadType
				+ ", etag=" + etag + ", data=" + data + ", json=" + json + ", dataHash=" + dataHash + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + "]";
	}

	@Override
	public MigratableTableTranslation<DBOStorageLocation, DBOStorageLocation> getTranslator() {
		return new MigratableTableTranslation<DBOStorageLocation, DBOStorageLocation>() {

			@Override
			public DBOStorageLocation createDatabaseObjectFromBackup(DBOStorageLocation backup) {
				if (backup.getData() != null) {
					if (backup.getJson() != null) {
						throw new IllegalArgumentException("Both 'data' and 'json' have values");
					}
					try {
						backup.setJson(EntityFactory.createJSONStringForEntity(backup.getData()));
						backup.setData(null);
					} catch (JSONObjectAdapterException e) {
						throw new RuntimeException(e);
					}
				}
				return backup;
			}

			@Override
			public DBOStorageLocation createBackupFromDatabaseObject(DBOStorageLocation dbo) {
				return dbo;
			}

		};
	}

	@Override
	public Class<? extends DBOStorageLocation> getBackupClass() {
		return DBOStorageLocation.class;
	}

	@Override
	public Class<? extends DBOStorageLocation> getDatabaseObjectClass() {
		return DBOStorageLocation.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}
}
