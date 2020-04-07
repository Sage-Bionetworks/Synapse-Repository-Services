package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ORG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOJsonSchema implements MigratableDatabaseObject<DBOJsonSchema, DBOJsonSchema> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_JSON_SCHEMA_ID, true).withIsBackupId(true),
			new FieldColumn("organizationId", COL_JSON_SCHEMA_ORG_ID),
			new FieldColumn("name", COL_JSON_SCHEMA_NAME),
			new FieldColumn("createdBy", COL_JSON_SCHEMA_CREATED_BY),
			new FieldColumn("createdOn", COL_JSON_SCHEMA_CREATED_ON), };

	private Long id;
	private Long organizationId;
	private String name;
	private Long createdBy;
	private Timestamp createdOn;

	public static final TableMapping<DBOJsonSchema> MAPPING = new TableMapping<DBOJsonSchema>() {

		@Override
		public DBOJsonSchema mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchema dbo = new DBOJsonSchema();
			dbo.setId(rs.getLong(COL_JSON_SCHEMA_ID));
			dbo.setOrganizationId(rs.getLong(COL_JSON_SCHEMA_ORG_ID));
			dbo.setName(rs.getString(COL_JSON_SCHEMA_NAME));
			dbo.setCreatedBy(rs.getLong(COL_JSON_SCHEMA_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_CREATED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_JSON_SCHEMA;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOJsonSchema> getDBOClass() {
			return DBOJsonSchema.class;
		}

	};

	@Override
	public TableMapping<DBOJsonSchema> getTableMapping() {
		return MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.JSON_SCHEMA;
	}

	public static final MigratableTableTranslation<DBOJsonSchema, DBOJsonSchema> TRANSLATOR = new BasicMigratableTableTranslation<DBOJsonSchema>();

	@Override
	public MigratableTableTranslation<DBOJsonSchema, DBOJsonSchema> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOJsonSchema> getBackupClass() {
		return DBOJsonSchema.class;
	}

	@Override
	public Class<? extends DBOJsonSchema> getDatabaseObjectClass() {
		return DBOJsonSchema.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOrganizationId() {
		return organizationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOrganizationId(Long organizationId) {
		this.organizationId = organizationId;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((organizationId == null) ? 0 : organizationId.hashCode());
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
		DBOJsonSchema other = (DBOJsonSchema) obj;
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (organizationId == null) {
			if (other.organizationId != null)
				return false;
		} else if (!organizationId.equals(other.organizationId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOJsonSchema [id=" + id + ", organizationId=" + organizationId + ", name=" + name + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + "]";
	}

}
