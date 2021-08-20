package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VALIDATION_SCHEMA_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VALIDATION_JSON_SCHEMA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_VALIDATION_JSON_SCHEMA_INDEX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VALIDATION_JSON_SCHEMA_INDEX;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOValidationJsonSchemaIndex implements DatabaseObject<DBOValidationJsonSchemaIndex> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("versionId", COL_VALIDATION_SCHEMA_VERSION_ID, true),
			new FieldColumn("validationSchema", COL_VALIDATION_JSON_SCHEMA), };

	private Long versionId;
	private String validationSchema;
	
	TableMapping<DBOValidationJsonSchemaIndex> MAPPING = new TableMapping<DBOValidationJsonSchemaIndex>() {

		@Override
		public DBOValidationJsonSchemaIndex mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOValidationJsonSchemaIndex dbo = new DBOValidationJsonSchemaIndex();
			dbo.setVersionId(rs.getLong(COL_VALIDATION_SCHEMA_VERSION_ID));
			dbo.setValidationSchema(rs.getString(COL_VALIDATION_JSON_SCHEMA));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_VALIDATION_JSON_SCHEMA_INDEX;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_VALIDATION_JSON_SCHEMA_INDEX;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOValidationJsonSchemaIndex> getDBOClass() {
			return DBOValidationJsonSchemaIndex.class;
		}
	};

	@Override
	public TableMapping<DBOValidationJsonSchemaIndex> getTableMapping() {
		return MAPPING;
	}

	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}

	public void setValidationSchema(String validationSchema) {
		this.validationSchema = validationSchema;
	}
	
	public Long getVersionId() {
		return versionId;
	}
	
	public String getValidationSchema() {
		return validationSchema;
	}

	@Override
	public int hashCode() {
		return Objects.hash(validationSchema, versionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOValidationJsonSchemaIndex)) {
			return false;
		}
		DBOValidationJsonSchemaIndex other = (DBOValidationJsonSchemaIndex) obj;
		return Objects.equals(validationSchema, other.validationSchema) && Objects.equals(versionId, other.versionId);
	}
}
