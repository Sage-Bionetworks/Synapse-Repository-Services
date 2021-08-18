package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VALIDATION_SCHEMA_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VALIDATION_JSON_SCHEMA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_VALIDATION_JSON_SCHEMA_INDEX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VALIDATION_JSON_SCHEMA_INDEX;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOValidationJsonSchemaIndex implements DatabaseObject<DBOValidationJsonSchemaIndex> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("versionId", COL_VALIDATION_SCHEMA_VERSION_ID, true),
			new FieldColumn("validationSchema", COL_VALIDATION_JSON_SCHEMA), };

	private String versionId;
	private String validationSchema;
	
	TableMapping<DBOValidationJsonSchemaIndex> MAPPING = new TableMapping<DBOValidationJsonSchemaIndex>() {

		@Override
		public DBOValidationJsonSchemaIndex mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOValidationJsonSchemaIndex dbo = new DBOValidationJsonSchemaIndex();
			dbo.setVersionId(rs.getString(COL_VALIDATION_SCHEMA_VERSION_ID));
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

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public void setValidationSchema(String validationSchema) {
		this.validationSchema = validationSchema;
	}
	
	public String getVersionId() {
		return versionId;
	}
	
	public String getValidationSchema() {
		return validationSchema;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((validationSchema == null) ? 0 : validationSchema.hashCode());
		result = prime * result + ((versionId == null) ? 0 : versionId.hashCode());
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
		DBOValidationJsonSchemaIndex other = (DBOValidationJsonSchemaIndex) obj;
		if (validationSchema == null) {
			if (other.validationSchema != null)
				return false;
		} else if (!validationSchema.equals(other.validationSchema))
			return false;
		if (versionId == null) {
			if (other.versionId != null)
				return false;
		} else if (!versionId.equals(other.versionId))
			return false;
		return true;
	}
}
