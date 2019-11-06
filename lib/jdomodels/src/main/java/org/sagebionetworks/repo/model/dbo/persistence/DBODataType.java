package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_UPDATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_TYPE_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODataType implements MigratableDatabaseObject<DBODataType, DBODataType> {

	private Long id;
	private Long objectId;
	private String objectType;
	private String dataType;
	private Long updatedBy;
	private Long updatedOn;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_TYPE_ID).withIsBackupId(true),
			new FieldColumn("objectId", COL_DATA_TYPE_OBJECT_ID).withIsPrimaryKey(true),
			new FieldColumn("objectType", COL_DATA_TYPE_OBJECT_TYPE).withIsPrimaryKey(true),
			new FieldColumn("dataType", COL_DATA_TYPE_TYPE),
			new FieldColumn("updatedBy", COL_DATA_TYPE_UPDATED_BY),
			new FieldColumn("updatedOn", COL_DATA_TYPE_UPDATED_ON), };

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Long getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Long updatedOn) {
		this.updatedOn = updatedOn;
	}

	@Override
	public TableMapping<DBODataType> getTableMapping() {
		return new TableMapping<DBODataType>() {

			@Override
			public DBODataType mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODataType dbo = new DBODataType();
				dbo.setId(rs.getLong(COL_DATA_TYPE_ID));
				dbo.setObjectId(rs.getLong(COL_DATA_TYPE_OBJECT_ID));
				dbo.setObjectType(rs.getString(COL_DATA_TYPE_OBJECT_TYPE));
				dbo.setDataType(rs.getString(COL_DATA_TYPE_TYPE));
				dbo.setUpdatedBy(rs.getLong(COL_DATA_TYPE_UPDATED_BY));
				dbo.setUpdatedOn(rs.getLong(COL_DATA_TYPE_UPDATED_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_TYPE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_TYPE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODataType> getDBOClass() {
				return DBODataType.class;
			}};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_TYPE;
	}

	@Override
	public MigratableTableTranslation<DBODataType, DBODataType> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODataType> getBackupClass() {
		return DBODataType.class;
	}

	@Override
	public Class<? extends DBODataType> getDatabaseObjectClass() {
		return DBODataType.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
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
		DBODataType other = (DBODataType) obj;
		if (dataType != other.dataType)
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

	@Override
	public String toString() {
		return "DBODataType [id=" + id + ", objectId=" + objectId + ", objectType=" + objectType + ", dataType="
				+ dataType + ", updatedBy=" + updatedBy + ", updatedOn=" + updatedOn + "]";
	}
}
