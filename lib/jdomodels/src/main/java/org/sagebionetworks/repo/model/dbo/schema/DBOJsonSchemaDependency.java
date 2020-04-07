package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEP_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEP_SEM_VER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEP_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_DEPENDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_DEPENDENCY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOJsonSchemaDependency implements MigratableDatabaseObject<DBOJsonSchemaDependency, DBOJsonSchemaDependency> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("versionNumber", COL_JSON_SCHEMA_DEP_VERSION_NUM, true).withIsBackupId(true),
			new FieldColumn("dependsOnSchemaId", COL_JSON_SCHEMA_DEP_SCHEMA_ID, true),
			new FieldColumn("dependsOnSemanticVersion", COL_JSON_SCHEMA_DEP_SEM_VER, true)};
	
	private Long versionNumber;
	private Long dependsOnSchemaId;
	private String dependsOnSemanticVersion;
	
	public static final TableMapping<DBOJsonSchemaDependency> MAPPING = new TableMapping<DBOJsonSchemaDependency>() {

		@Override
		public DBOJsonSchemaDependency mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchemaDependency dbo = new DBOJsonSchemaDependency();
			dbo.setVersionNumber(rs.getLong(COL_JSON_SCHEMA_DEP_VERSION_NUM));
			dbo.setDependsOnSchemaId(rs.getLong(COL_JSON_SCHEMA_DEP_SCHEMA_ID));
			dbo.setDependsOnSemanticVersion(rs.getString(COL_JSON_SCHEMA_DEP_SEM_VER));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_JSON_SCHEMA_DEPENDENCY;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA_DEPENDS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOJsonSchemaDependency> getDBOClass() {
			return DBOJsonSchemaDependency.class;
		}};
	
	@Override
	public TableMapping<DBOJsonSchemaDependency> getTableMapping() {
		return MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.JSON_SCHEMA_DEPENDENCY;
	}

	public static final MigratableTableTranslation<DBOJsonSchemaDependency, DBOJsonSchemaDependency> TRANSLATOR = new BasicMigratableTableTranslation<DBOJsonSchemaDependency>();
	
	@Override
	public MigratableTableTranslation<DBOJsonSchemaDependency, DBOJsonSchemaDependency> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOJsonSchemaDependency> getBackupClass() {
		return DBOJsonSchemaDependency.class;
	}

	@Override
	public Class<? extends DBOJsonSchemaDependency> getDatabaseObjectClass() {
		return DBOJsonSchemaDependency.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
	}

	public Long getDependsOnSchemaId() {
		return dependsOnSchemaId;
	}

	public void setDependsOnSchemaId(Long dependsOnSchemaId) {
		this.dependsOnSchemaId = dependsOnSchemaId;
	}

	public String getDependsOnSemanticVersion() {
		return dependsOnSemanticVersion;
	}

	public void setDependsOnSemanticVersion(String dependsOnSemanticVersion) {
		this.dependsOnSemanticVersion = dependsOnSemanticVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dependsOnSchemaId == null) ? 0 : dependsOnSchemaId.hashCode());
		result = prime * result + ((dependsOnSemanticVersion == null) ? 0 : dependsOnSemanticVersion.hashCode());
		result = prime * result + ((versionNumber == null) ? 0 : versionNumber.hashCode());
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
		DBOJsonSchemaDependency other = (DBOJsonSchemaDependency) obj;
		if (dependsOnSchemaId == null) {
			if (other.dependsOnSchemaId != null)
				return false;
		} else if (!dependsOnSchemaId.equals(other.dependsOnSchemaId))
			return false;
		if (dependsOnSemanticVersion == null) {
			if (other.dependsOnSemanticVersion != null)
				return false;
		} else if (!dependsOnSemanticVersion.equals(other.dependsOnSemanticVersion))
			return false;
		if (versionNumber == null) {
			if (other.versionNumber != null)
				return false;
		} else if (!versionNumber.equals(other.versionNumber))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOJsonSchemaDependency [versionNumber=" + versionNumber + ", dependsOnSchemaId=" + dependsOnSchemaId
				+ ", dependsOnSemanticVersion=" + dependsOnSemanticVersion + "]";
	}

}
