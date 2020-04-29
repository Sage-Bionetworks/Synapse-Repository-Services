package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_LATEST_VER_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_LATEST_VER_VER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_LATEST_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_LATEST_VERSION;

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

public class DBOJsonSchemaLatestVersion
		implements MigratableDatabaseObject<DBOJsonSchemaLatestVersion, DBOJsonSchemaLatestVersion> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("schemaId", COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_JSON_SCHEMA_LATEST_VER_ETAG).withIsEtag(true),
			new FieldColumn("versionId", COL_JSON_SCHEMA_LATEST_VER_VER_ID) };

	private Long schemaId;
	private String etag;
	private Long versionId;

	public final static TableMapping<DBOJsonSchemaLatestVersion> MAPPING = new TableMapping<DBOJsonSchemaLatestVersion>() {

		@Override
		public DBOJsonSchemaLatestVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchemaLatestVersion dbo = new DBOJsonSchemaLatestVersion();
			dbo.setSchemaId(rs.getLong(COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID));
			dbo.setEtag(rs.getString(COL_JSON_SCHEMA_LATEST_VER_ETAG));
			dbo.setVersionId(rs.getLong(COL_JSON_SCHEMA_LATEST_VER_VER_ID));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_JSON_SCHEMA_LATEST_VERSION;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA_LATEST_VERSION;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOJsonSchemaLatestVersion> getDBOClass() {
			return DBOJsonSchemaLatestVersion.class;
		}
	};

	@Override
	public TableMapping<DBOJsonSchemaLatestVersion> getTableMapping() {
		return MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.JSON_SCHEMA_LATEST_VERSION;
	}

	public static final MigratableTableTranslation<DBOJsonSchemaLatestVersion, DBOJsonSchemaLatestVersion> TRANSLATOR = new BasicMigratableTableTranslation<DBOJsonSchemaLatestVersion>();

	@Override
	public MigratableTableTranslation<DBOJsonSchemaLatestVersion, DBOJsonSchemaLatestVersion> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOJsonSchemaLatestVersion> getBackupClass() {
		return DBOJsonSchemaLatestVersion.class;
	}

	@Override
	public Class<? extends DBOJsonSchemaLatestVersion> getDatabaseObjectClass() {
		return DBOJsonSchemaLatestVersion.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
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
	 * @return the etag
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * @param etag the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
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

	@Override
	public int hashCode() {
		return Objects.hash(etag, schemaId, versionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOJsonSchemaLatestVersion)) {
			return false;
		}
		DBOJsonSchemaLatestVersion other = (DBOJsonSchemaLatestVersion) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(schemaId, other.schemaId)
				&& Objects.equals(versionId, other.versionId);
	}

}
