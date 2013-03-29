package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_DOI_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_DOI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;

public class DBODoi implements DatabaseObject<DBODoi> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DOI_ID, true),
			new FieldColumn("eTag", COL_DOI_ETAG),
			new FieldColumn("doiStatus", COL_DOI_DOI_STATUS),
			new FieldColumn("objectId", COL_DOI_OBJECT_ID),
			new FieldColumn("doiObjectType", COL_DOI_OBJECT_TYPE),
			new FieldColumn("objectVersion", COL_DOI_OBJECT_VERSION),
			new FieldColumn("createdBy", COL_DOI_CREATED_BY),
			new FieldColumn("createdOn", COL_DOI_CREATED_ON),
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
					dbo.setDoiObjectType(DoiObjectType.valueOf(rs.getString(COL_DOI_OBJECT_TYPE)));
					// Object version is nullable
					// We can't just use rs.getLong() which returns a primitive long
					Object obj = rs.getObject(COL_DOI_OBJECT_VERSION);
					if (obj != null) {
						dbo.setObjectVersion(rs.getLong(COL_DOI_OBJECT_VERSION));
					}
					dbo.setCreatedBy(rs.getLong(COL_DOI_CREATED_BY));
					dbo.setCreatedOn(rs.getTimestamp(COL_DOI_CREATED_ON));
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
	public String getDoiObjectType() {
		return doiObjectType.name();
	}
	public void setDoiObjectType(DoiObjectType doiObjectType) {
		this.doiObjectType = doiObjectType;
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
	public Timestamp getUpdatedOn() {
		return updatedOn;
	}
	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}

	@Override
	public String toString() {
		return "DBODoi [id=" + id + ", eTag=" + eTag + ", doiStatus="
				+ doiStatus + ", objectId=" + objectId + ", doiObjectType="
				+ doiObjectType + ", objectVersion=" + objectVersion
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", updatedOn=" + updatedOn + "]";
	}

	private Long id;
	private String eTag;
	private DoiStatus doiStatus;
	private Long objectId;
	private DoiObjectType doiObjectType;
	private Long objectVersion;
	private Long createdBy;
	private Timestamp createdOn;
	private Timestamp updatedOn;
}

