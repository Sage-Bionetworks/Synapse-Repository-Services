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
import java.util.Objects;

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
		return Objects.hash(dataType, id, objectId, objectType, updatedBy, updatedOn);
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
		return Objects.equals(dataType, other.dataType) && Objects.equals(id, other.id)
				&& Objects.equals(objectId, other.objectId) && Objects.equals(objectType, other.objectType)
				&& Objects.equals(updatedBy, other.updatedBy) && Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "DBODataType [id=" + id + ", objectId=" + objectId + ", objectType=" + objectType + ", dataType="
				+ dataType + ", updatedBy=" + updatedBy + ", updatedOn=" + updatedOn + "]";
	}
}
