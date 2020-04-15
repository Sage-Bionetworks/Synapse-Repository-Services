package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_S3_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_S3_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_SEMANTIC;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;


public class DBOJsonSchemaVersion implements MigratableDatabaseObject<DBOJsonSchemaVersion, DBOJsonSchemaVersion> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("versionNumber", COL_JSON_SCHEMA_VER_ID, true).withIsBackupId(true),
			new FieldColumn("schemaId", COL_JSON_SCHEMA_VER_SCHEMA_ID),
			new FieldColumn("semanticVersion", COL_JSON_SCHEMA_VER_SEMANTIC),
			new FieldColumn("createdBy", COL_JSON_SCHEMA_VER_CREATED_BY),
			new FieldColumn("createdOn", COL_JSON_SCHEMA_VER_CREATED_ON),
			new FieldColumn("s3Bucket", COL_JSON_SCHEMA_VER_S3_BUCKET),
			new FieldColumn("s3Key", COL_JSON_SCHEMA_VER_S3_KEY),};
	
	private Long versionId;
	private Long schemaId;
	private String semanticVersion;
	private Long createdBy;
	private Timestamp createdOn;
	private String s3Bucket;
	private String s3Key;
	
	public static TableMapping<DBOJsonSchemaVersion> MAPPING = new TableMapping<DBOJsonSchemaVersion>() {

		@Override
		public DBOJsonSchemaVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchemaVersion dbo = new DBOJsonSchemaVersion();
			dbo.setVersionId(rs.getLong(COL_JSON_SCHEMA_VER_ID));
			dbo.setSchemaId(rs.getLong(COL_JSON_SCHEMA_VER_SCHEMA_ID));
			dbo.setSemanticVersion(rs.getString(COL_JSON_SCHEMA_VER_SEMANTIC));
			dbo.setCreatedBy(rs.getLong(COL_JSON_SCHEMA_VER_CREATED_BY));
			dbo.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_VER_CREATED_ON));
			dbo.setS3Bucket(rs.getString(COL_JSON_SCHEMA_VER_S3_BUCKET));
			dbo.setS3Key(rs.getString(COL_JSON_SCHEMA_VER_S3_KEY));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_JSON_SCHEMA_VERSION;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA_VERSION;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOJsonSchemaVersion> getDBOClass() {
			return DBOJsonSchemaVersion.class;
		}};

	@Override
	public TableMapping<DBOJsonSchemaVersion> getTableMapping() {
		return MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.JSON_SCHEMA_VERSION;
	}

	public static final MigratableTableTranslation<DBOJsonSchemaVersion, DBOJsonSchemaVersion> TRANSLATOR = new BasicMigratableTableTranslation<DBOJsonSchemaVersion>();
	
	@Override
	public MigratableTableTranslation<DBOJsonSchemaVersion, DBOJsonSchemaVersion> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOJsonSchemaVersion> getBackupClass() {
		return DBOJsonSchemaVersion.class;
	}

	@Override
	public Class<? extends DBOJsonSchemaVersion> getDatabaseObjectClass() {
		return DBOJsonSchemaVersion.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?, ?>> seconday = new ArrayList<MigratableDatabaseObject<?,?>>(1);
		seconday.add(new DBOJsonSchemaDependency());
		return seconday;
	}

	public Long getVersionId() {
		return versionId;
	}

	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}

	public Long getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(Long schemaId) {
		this.schemaId = schemaId;
	}

	public String getSemanticVersion() {
		return semanticVersion;
	}

	public void setSemanticVersion(String semanticVersion) {
		this.semanticVersion = semanticVersion;
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

	public String getS3Bucket() {
		return s3Bucket;
	}

	public void setS3Bucket(String s3Bucket) {
		this.s3Bucket = s3Bucket;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, s3Bucket, s3Key, schemaId, semanticVersion, versionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOJsonSchemaVersion)) {
			return false;
		}
		DBOJsonSchemaVersion other = (DBOJsonSchemaVersion) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(s3Bucket, other.s3Bucket) && Objects.equals(s3Key, other.s3Key)
				&& Objects.equals(schemaId, other.schemaId) && Objects.equals(semanticVersion, other.semanticVersion)
				&& Objects.equals(versionId, other.versionId);
	}

	@Override
	public String toString() {
		return "DBOJsonSchemaVersion [versionNumber=" + versionId + ", schemaId=" + schemaId + ", semanticVersion="
				+ semanticVersion + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", s3Bucket=" + s3Bucket
				+ ", s3Key=" + s3Key + "]";
	}
}
