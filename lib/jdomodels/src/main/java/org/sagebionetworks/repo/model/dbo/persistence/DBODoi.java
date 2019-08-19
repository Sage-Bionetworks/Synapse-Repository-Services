package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_DOI_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_UPDATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_DOI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

public class DBODoi implements MigratableDatabaseObject<DBODoi, DBODoi> {

	public static final long NULL_OBJECT_VERSION = -1L;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DOI_ID, true).withIsBackupId(true),
			new FieldColumn("eTag", COL_DOI_ETAG).withIsEtag(true),
			new FieldColumn("doiStatus", COL_DOI_DOI_STATUS),
			new FieldColumn("objectId", COL_DOI_OBJECT_ID),
			new FieldColumn("objectType", COL_DOI_OBJECT_TYPE),
			new FieldColumn("objectVersion", COL_DOI_OBJECT_VERSION),
			new FieldColumn("createdBy", COL_DOI_CREATED_BY),
			new FieldColumn("createdOn", COL_DOI_CREATED_ON),
			new FieldColumn("updatedBy", COL_DOI_UPDATED_BY),
			new FieldColumn("updatedOn", COL_DOI_UPDATED_ON)
	};

	@Override
	public TableMapping<DBODoi> getTableMapping() {
		return new TableMapping<DBODoi>() {
				@Override
				public DBODoi mapRow(ResultSet rs, int rowNum) throws SQLException {
					DBODoi dbo = new DBODoi();
					dbo.setId(rs.getLong(COL_DOI_ID));
					dbo.setETag(rs.getString(COL_DOI_ETAG));
					dbo.setDoiStatus(DoiStatus.valueOf(rs.getString(COL_DOI_DOI_STATUS)));
					dbo.setObjectId(rs.getLong(COL_DOI_OBJECT_ID));
					dbo.setObjectType(ObjectType.valueOf(rs.getString(COL_DOI_OBJECT_TYPE)));
					dbo.setObjectVersion(rs.getLong(COL_DOI_OBJECT_VERSION));
					dbo.setCreatedBy(rs.getLong(COL_DOI_CREATED_BY));
					dbo.setCreatedOn(rs.getTimestamp(COL_DOI_CREATED_ON));
					dbo.setUpdatedBy(rs.getLong(COL_DOI_UPDATED_BY));
					dbo.setUpdatedOn(rs.getTimestamp(COL_DOI_UPDATED_ON));
					return dbo;
				}

				@Override
				public String getTableName() {
					return TABLE_DOI;
				}

				@Override
				public String getDDLFileName() {
					return DDL_FILE_DOI;
				}

				@Override
				public FieldColumn[] getFieldColumns() {
					return FIELDS;
				}

				@Override
				public Class<? extends DBODoi> getDBOClass() {
					return DBODoi.class;
				}
		};
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getETag() {
		return eTag;
	}
	public void setETag(String eTag) {
		this.eTag = eTag;
	}
	public String getDoiStatus() {
		return doiStatus.name();
	}
	public void setDoiStatus(DoiStatus doiStatus) {
		this.doiStatus = doiStatus;
	}
	public Long getObjectId() {
		return objectId;
	}
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}
	public String getObjectType() {
		return objectType.name();
	}
	public void setObjectType(ObjectType objectType) {
		this.objectType = objectType;
	}
	public Long getObjectVersion() {
		return objectVersion;
	}
	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
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
	public Long getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}
	public Timestamp getUpdatedOn() {
		return updatedOn;
	}
	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}

	@Override
	public String toString() {
		return "DBODoi [id=" + id + ", eTag=" + eTag + ", doiStatus="
				+ doiStatus + ", objectId=" + objectId + ", objectType="
				+ objectType + ", objectVersion=" + objectVersion
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", updatedBy=" + updatedBy + ", updatedOn=" + updatedOn +"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((doiStatus == null) ? 0 : doiStatus.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((objectVersion == null) ? 0 : objectVersion.hashCode());
		result = prime * result + ((updatedBy == null) ? 0 : updatedBy.hashCode());
		result = prime * result + ((updatedOn == null) ? 0 : updatedOn.hashCode());
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
		DBODoi other = (DBODoi) obj;
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
		if (doiStatus != other.doiStatus)
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectType != other.objectType)
			return false;
		if (objectVersion == null) {
			if (other.objectVersion != null)
				return false;
		} else if (!objectVersion.equals(other.objectVersion))
			return false;
		if (updatedBy == null) {
			if (other.updatedBy != null)
				return false;
		} else if (!updatedBy.equals(other.updatedBy))
			return false;
		if (updatedOn == null) {
			if (other.updatedOn != null)
				return false;
		} else if (!updatedOn.equals(other.updatedOn))
			return false;
		return true;
	}

	private Long id;
	private String eTag;
	private DoiStatus doiStatus;
	private Long objectId;
	private ObjectType objectType;
	@TemporaryCode(author="jhill",comment="To be removed after prod 274")
	private ObjectType doiObjectType;
	private Long objectVersion;
	private Long createdBy;
	private Timestamp createdOn;
	private Long updatedBy;
	private Timestamp updatedOn;

	@TemporaryCode(author="jhill",comment="To be removed after prod 274")
	public ObjectType getDoiObjectType() {
		return doiObjectType;
	}

	@TemporaryCode(author="jhill",comment="To be removed after prod 274")
	public void setDoiObjectType(ObjectType doiObjectType) {
		this.doiObjectType = doiObjectType;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOI;
	}

	@TemporaryCode(author="jhill",comment="To be removed after prod 274")
	@Override
	public MigratableTableTranslation<DBODoi, DBODoi> getTranslator() {
		return new MigratableTableTranslation<DBODoi, DBODoi>(){

			@Override
			public DBODoi createDatabaseObjectFromBackup(DBODoi backup) {
				// copy the value from the old type to the new type.
				if(backup.doiObjectType != null) {
					backup.setObjectType(backup.getDoiObjectType());
					backup.setDoiObjectType(null);
				}
				return backup;
			}

			@Override
			public DBODoi createBackupFromDatabaseObject(DBODoi dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBODoi> getBackupClass() {
		return DBODoi.class;
	}

	@Override
	public Class<? extends DBODoi> getDatabaseObjectClass() {
		return DBODoi.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}

