package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UPLOAD_DESTINATION_LOCATION;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;

/**
 * descriptor of what's in a column of a participant data record
 */
@Table(name = TABLE_UPLOAD_DESTINATION_LOCATION)
public class DBOUploadDestinationLocation implements MigratableDatabaseObject<DBOUploadDestinationLocation, DBOUploadDestinationLocation> {

	@Field(name = COL_UPLOAD_DESTINATION_LOCATION_ID, backupId = true, primary = true, nullable = false)
	private Long id;

	@Field(name = COL_UPLOAD_DESTINATION_LOCATION_DESCRIPTION, varchar = 512)
	private String description;

	@Field(name = COL_PROJECT_SETTING_ETAG, etag = true)
	private String etag;

	@Field(name = COL_UPLOAD_DESTINATION_LOCATION_DATA, serialized = "mediumblob")
	private UploadDestinationLocationSetting data;

	@Field(name = COL_UPLOAD_DESTINATION_LOCATION_CREATED_BY, nullable = false)
	private Long createdBy;

	@Field(name = COL_UPLOAD_DESTINATION_LOCATION_CREATED_ON, nullable = false)
	private Date createdOn;

	private static TableMapping<DBOUploadDestinationLocation> tableMapping = AutoTableMapping.create(DBOUploadDestinationLocation.class);

	@Override
	public TableMapping<DBOUploadDestinationLocation> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.UPLOAD_DESTINATION_LOCATION;
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

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public UploadDestinationLocationSetting getData() {
		return data;
	}

	public void setData(UploadDestinationLocationSetting data) {
		this.data = data;
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
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		DBOUploadDestinationLocation other = (DBOUploadDestinationLocation) obj;
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
		return true;
	}

	@Override
	public String toString() {
		return "DBOUploadDestinationLocation [id=" + id + ", description=" + description + ", etag=" + etag + ", data=" + data
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn + "]";
	}

	@Override
	public MigratableTableTranslation<DBOUploadDestinationLocation, DBOUploadDestinationLocation> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOUploadDestinationLocation, DBOUploadDestinationLocation>() {

			@Override
			public DBOUploadDestinationLocation createDatabaseObjectFromBackup(DBOUploadDestinationLocation backup) {
				return backup;
			}

			@Override
			public DBOUploadDestinationLocation createBackupFromDatabaseObject(DBOUploadDestinationLocation dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOUploadDestinationLocation> getBackupClass() {
		return DBOUploadDestinationLocation.class;
	}

	@Override
	public Class<? extends DBOUploadDestinationLocation> getDatabaseObjectClass() {
		return DBOUploadDestinationLocation.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
