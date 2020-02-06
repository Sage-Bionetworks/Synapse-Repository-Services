package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DATA_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_UPLOAD_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

/**
 * descriptor of what's in a column of a participant data record
 */
@Table(name = TABLE_STORAGE_LOCATION, constraints = {
		"INDEX `CREATED_BY_DATA_HASH_INDEX` (`" + COL_STORAGE_LOCATION_CREATED_BY + "`, `" + COL_STORAGE_LOCATION_DATA_HASH + "`)" })
public class DBOStorageLocation implements MigratableDatabaseObject<DBOStorageLocation, DBOStorageLocation> {

	@Field(name = COL_STORAGE_LOCATION_ID, backupId = true, primary = true, nullable = false)
	private Long id;

	@Field(name = COL_STORAGE_LOCATION_DESCRIPTION, varchar = 512)
	private String description;

	@Field(name = COL_STORAGE_LOCATION_UPLOAD_TYPE, nullable = false, sql = "DEFAULT 'NONE'")
	private UploadType uploadType;

	@Field(name = COL_PROJECT_SETTING_ETAG, etag = true, nullable = false)
	private String etag;

	@Field(name = COL_STORAGE_LOCATION_DATA, serialized = "mediumblob")
	private StorageLocationSetting data;

	@Field(name = COL_STORAGE_LOCATION_DATA_HASH, varchar = 36, nullable = false)
	private String dataHash;

	@Field(name = COL_STORAGE_LOCATION_CREATED_BY, nullable = false)
	private Long createdBy;

	@Field(name = COL_STORAGE_LOCATION_CREATED_ON, nullable = false)
	private Date createdOn;

	private static final TableMapping<DBOStorageLocation> TABLE_MAPPING = AutoTableMapping.create(DBOStorageLocation.class);
	
	private static final MigratableTableTranslation<DBOStorageLocation, DBOStorageLocation> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<DBOStorageLocation>() {
	
	@Override
		public DBOStorageLocation createDatabaseObjectFromBackup(DBOStorageLocation backup) {
			StorageLocationSetting setting = backup.getData();
			if (setting != null) {
				if (setting instanceof ExternalGoogleCloudStorageLocationSetting) {
					ExternalGoogleCloudStorageLocationSetting storageLocation = (ExternalGoogleCloudStorageLocationSetting) setting;
					String sanitiedBaseKey = StorageLocationUtils.sanitizeBaseKey(storageLocation.getBaseKey());
					storageLocation.setBaseKey(sanitiedBaseKey);
					backup.setDataHash(null);
				}
				if (backup.getDataHash() == null) {
					String settingHash = StorageLocationUtils.computeHash(setting);
					backup.setDataHash(settingHash);
				}
			}
			return backup;
		}
	};

	@Override
	public TableMapping<DBOStorageLocation> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.STORAGE_LOCATION;
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

	public UploadType getUploadType() {
		return uploadType;
	}

	public void setUploadType(UploadType uploadType) {
		this.uploadType = uploadType;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public StorageLocationSetting getData() {
		return data;
	}

	public void setData(StorageLocationSetting data) {
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((dataHash == null) ? 0 : dataHash.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((uploadType == null) ? 0 : uploadType.hashCode());
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
		DBOStorageLocation other = (DBOStorageLocation) obj;
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
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (dataHash == null) {
			if (other.dataHash != null)
				return false;
		} else if (!dataHash.equals(other.dataHash))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (uploadType != other.uploadType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOStorageLocation [id=" + id + ", description=" + description + ", uploadType=" + uploadType
				+ ", etag=" + etag + ", data=" + data + ", dataHash=" + dataHash + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + "]";
	}

	@Override
	public MigratableTableTranslation<DBOStorageLocation, DBOStorageLocation> getTranslator() {
		return MIGRATION_TRANSLATOR;
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
