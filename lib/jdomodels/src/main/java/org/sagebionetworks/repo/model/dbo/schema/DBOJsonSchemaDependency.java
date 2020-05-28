package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEPEPNDENCY_DEPENDS_ON_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEPENDENCY_DEPENDS_ON_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_DEPENDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_DEPENDENCY;

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

public class DBOJsonSchemaDependency implements MigratableDatabaseObject<DBOJsonSchemaDependency, DBOJsonSchemaDependency> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("versionId", COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID, true).withIsBackupId(true),
			new FieldColumn("dependsOnSchemaId", COL_JSON_SCHEMA_DEPEPNDENCY_DEPENDS_ON_SCHEMA_ID, true),
			new FieldColumn("dependsOnVersionId", COL_JSON_SCHEMA_DEPENDENCY_DEPENDS_ON_VERSION_ID, true)};
	
	private Long versionId;
	private Long dependsOnSchemaId;
	private Long dependsOnVersionId;
	
	public static final TableMapping<DBOJsonSchemaDependency> MAPPING = new TableMapping<DBOJsonSchemaDependency>() {

		@Override
		public DBOJsonSchemaDependency mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchemaDependency dbo = new DBOJsonSchemaDependency();
			dbo.setVersionId(rs.getLong(COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID));
			dbo.setDependsOnSchemaId(rs.getLong(COL_JSON_SCHEMA_DEPEPNDENCY_DEPENDS_ON_SCHEMA_ID));
			dbo.setDependsOnVersionId(rs.getLong(COL_JSON_SCHEMA_DEPENDENCY_DEPENDS_ON_VERSION_ID));
			if(rs.wasNull()) {
				dbo.setDependsOnVersionId(null);
			}
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

	public Long getVersionId() {
		return versionId;
	}

	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}

	public Long getDependsOnSchemaId() {
		return dependsOnSchemaId;
	}

	public void setDependsOnSchemaId(Long dependsOnSchemaId) {
		this.dependsOnSchemaId = dependsOnSchemaId;
	}

	/**
	 * @return the dependsOnVersionId
	 */
	public Long getDependsOnVersionId() {
		return dependsOnVersionId;
	}

	/**
	 * @param dependsOnVersionId the dependsOnVersionId to set
	 */
	public void setDependsOnVersionId(Long dependsOnVersionId) {
		this.dependsOnVersionId = dependsOnVersionId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependsOnSchemaId, dependsOnVersionId, versionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOJsonSchemaDependency)) {
			return false;
		}
		DBOJsonSchemaDependency other = (DBOJsonSchemaDependency) obj;
		return Objects.equals(dependsOnSchemaId, other.dependsOnSchemaId)
				&& Objects.equals(dependsOnVersionId, other.dependsOnVersionId)
				&& Objects.equals(versionId, other.versionId);
	}

	@Override
	public String toString() {
		return "DBOJsonSchemaDependency [versionId=" + versionId + ", dependsOnSchemaId=" + dependsOnSchemaId
				+ ", dependsOnVersionId=" + dependsOnVersionId + "]";
	}


}
