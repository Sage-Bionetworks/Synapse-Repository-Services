package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JONS_SCHEMA_BINDING_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_BIND_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_BINDING;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_OBJECT_BINDING;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOJsonSchemaBindObject
		implements MigratableDatabaseObject<DBOJsonSchemaBindObject, DBOJsonSchemaBindObject> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("bindId", COL_JSON_SCHEMA_BINDING_BIND_ID, true).withIsBackupId(true),
			new FieldColumn("schemaId", COL_JSON_SCHEMA_BINDING_SCHEMA_ID),
			new FieldColumn("versionId", COL_JSON_SCHEMA_BINDING_VERSION_ID),
			new FieldColumn("objectId", COL_JONS_SCHEMA_BINDING_OBJECT_ID),
			new FieldColumn("objectType", COL_JSON_SCHEMA_BINDING_OBJECT_TYPE),
			new FieldColumn("createdBy", COL_JSON_SCHEMA_BINDING_CREATED_BY),
			new FieldColumn("createdOn", COL_JSON_SCHEMA_BINDING_CREATED_ON), };

	private Long bindId;
	private Long schemaId;
	private Long versionId;
	private Long objectId;
	private String objectType;
	private Long createdBy;
	private Timestamp createdOn;

	/**
	 * @return the bindId
	 */
	public Long getBindId() {
		return bindId;
	}

	/**
	 * @param bindId the bindId to set
	 */
	public void setBindId(Long bindId) {
		this.bindId = bindId;
	}

	/**
	 * @return the schemaId
	 */
	public Long getSchemaId() {
		return schemaId;
	}

	/**
	 * @param schemaId the schemaId to set
	 */
	public void setSchemaId(Long schemaId) {
		this.schemaId = schemaId;
	}

	/**
	 * @return the versionId
	 */
	public Long getVersionId() {
		return versionId;
	}

	/**
	 * @param versionId the versionId to set
	 */
	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}

	/**
	 * @return the objectId
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	/**
	 * @return the objectType
	 */
	public String getObjectType() {
		return objectType;
	}

	/**
	 * @param objectType the objectType to set
	 */
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	/**
	 * @return the createdBy
	 */
	public Long getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the createdOn
	 */
	public Timestamp getCreatedOn() {
		return createdOn;
	}

	/**
	 * @param createdOn the createdOn to set
	 */
	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public static final TableMapping<DBOJsonSchemaBindObject> TABLE_MAPPING = new TableMapping<DBOJsonSchemaBindObject>() {

		@Override
		public DBOJsonSchemaBindObject mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchemaBindObject dbo = new DBOJsonSchemaBindObject();
			dbo.setBindId(rs.getLong(COL_JSON_SCHEMA_BINDING_BIND_ID));
			dbo.setSchemaId(rs.getLong(COL_JSON_SCHEMA_BINDING_SCHEMA_ID));
			dbo.setVersionId(rs.getLong(COL_JSON_SCHEMA_BINDING_VERSION_ID));
			if(rs.wasNull()) {
				dbo.setVersionId(null);
			}
			dbo.setObjectId(rs.getLong(COL_JONS_SCHEMA_BINDING_OBJECT_ID));
			dbo.setObjectType(rs.getString(COL_JSON_SCHEMA_BINDING_OBJECT_TYPE));
			dbo.setCreatedBy(rs.getLong(COL_JSON_SCHEMA_BINDING_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_BINDING_CREATED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_JSON_SCHEMA_OBJECT_BINDING;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA_BINDING;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOJsonSchemaBindObject> getDBOClass() {
			return DBOJsonSchemaBindObject.class;
		}
	};

	@Override
	public TableMapping<DBOJsonSchemaBindObject> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.JSON_SCHEMA_OBJECT_BINDING;
	}

	public static final MigratableTableTranslation<DBOJsonSchemaBindObject, DBOJsonSchemaBindObject> TRANSLATOR = new BasicMigratableTableTranslation<DBOJsonSchemaBindObject>();

	@Override
	public MigratableTableTranslation<DBOJsonSchemaBindObject, DBOJsonSchemaBindObject> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOJsonSchemaBindObject> getBackupClass() {
		return DBOJsonSchemaBindObject.class;
	}

	@Override
	public Class<? extends DBOJsonSchemaBindObject> getDatabaseObjectClass() {
		return DBOJsonSchemaBindObject.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bindId, createdBy, createdOn, objectId, objectType, schemaId, versionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOJsonSchemaBindObject)) {
			return false;
		}
		DBOJsonSchemaBindObject other = (DBOJsonSchemaBindObject) obj;
		return Objects.equals(bindId, other.bindId) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(objectType, other.objectType) && Objects.equals(schemaId, other.schemaId)
				&& Objects.equals(versionId, other.versionId);
	}

	@Override
	public String toString() {
		return "DBOJsonSchemaBindObject [bindId=" + bindId + ", schemaId=" + schemaId + ", versionId=" + versionId
				+ ", objectId=" + objectId + ", objectType=" + objectType + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + "]";
	}

}
