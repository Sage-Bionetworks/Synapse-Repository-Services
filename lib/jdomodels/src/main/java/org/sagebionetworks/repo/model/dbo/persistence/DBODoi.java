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
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;

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
		return doiObjectType.name();
	}
	public void setObjectType(ObjectType objectType) {
		this.doiObjectType = objectType;
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
				+ doiObjectType + ", objectVersion=" + objectVersion
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", updatedBy=" + updatedBy + ", updatedOn=" + updatedOn +"]";
	}

	private Long id;
	private String eTag;
	private DoiStatus doiStatus;
	private Long objectId;
	private ObjectType doiObjectType;
	private Long objectVersion;
	private Long createdBy;
	private Timestamp createdOn;
	private Long updatedBy;
	private Timestamp updatedOn;

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOI;
	}

	@Override
	public MigratableTableTranslation<DBODoi, DBODoi> getTranslator() {
		return new BasicMigratableTableTranslation<>();
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

