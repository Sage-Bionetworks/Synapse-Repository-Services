package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_DOI_STATUS;
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

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBODoi implements AutoIncrementDatabaseObject<DBODoi> {

	public static enum DoiStatus {
		IN_PROCESS,
		READY,
		ERROR
	}

	public static enum ObjectType {
		ENTITY,
		EVALUATION
	}

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DOI_ID, true),
			new FieldColumn("doiStatus", COL_DOI_DOI_STATUS),
			new FieldColumn("objectId", COL_DOI_OBJECT_ID),
			new FieldColumn("objectType", COL_DOI_OBJECT_TYPE),
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
					dbo.setDoiStatus(DoiStatus.valueOf(rs.getString(COL_DOI_DOI_STATUS)));
					dbo.setObjectId(rs.getLong(COL_DOI_OBJECT_ID));
					dbo.setObjectType(ObjectType.valueOf(rs.getString(COL_DOI_OBJECT_TYPE)));
					dbo.setObjectVersion(rs.getLong(COL_DOI_OBJECT_VERSION));
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
	public DoiStatus getDoiStatus() {
		return doiStatus;
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
	public ObjectType getObjectType() {
		return objectType;
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
	public Timestamp getUpdatedOn() {
		return updatedOn;
	}
	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}

	private Long id;
	private DoiStatus doiStatus;
	private Long objectId;
	private ObjectType objectType;
	private Long objectVersion;
	private Long createdBy;
	private Timestamp createdOn;
	private Timestamp updatedOn;
}

