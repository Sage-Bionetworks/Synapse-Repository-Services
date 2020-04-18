package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_SHA256;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_BLOB;

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

public class DBOJsonSchemaBlob implements MigratableDatabaseObject<DBOJsonSchemaBlob, DBOJsonSchemaBlob> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("blobId", COL_JSON_SCHEMA_BLOB_ID, true).withIsBackupId(true),
			new FieldColumn("json", COL_JSON_SCHEMA_BLOB_BLOB),
			new FieldColumn("sha256Hex", COL_JSON_SCHEMA_BLOB_SHA256), };

	Long blobId;
	String json;
	String sha256Hex;

	public static final TableMapping<DBOJsonSchemaBlob> MAPPING = new TableMapping<DBOJsonSchemaBlob>() {

		@Override
		public DBOJsonSchemaBlob mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOJsonSchemaBlob dbo = new DBOJsonSchemaBlob();
			dbo.setBlobId(rs.getLong(COL_JSON_SCHEMA_BLOB_ID));
			dbo.setJson(rs.getString(COL_JSON_SCHEMA_BLOB_BLOB));
			dbo.setSha256Hex(rs.getString(COL_JSON_SCHEMA_BLOB_SHA256));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_JSON_SCHEMA_BLOB;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA_BLOB;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOJsonSchemaBlob> getDBOClass() {
			return DBOJsonSchemaBlob.class;
		}
	};

	@Override
	public TableMapping<DBOJsonSchemaBlob> getTableMapping() {
		return MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.JSON_SCHEMA_BLOB;
	}

	public static final MigratableTableTranslation<DBOJsonSchemaBlob, DBOJsonSchemaBlob> TRANSLATOR = new BasicMigratableTableTranslation<DBOJsonSchemaBlob>();

	@Override
	public MigratableTableTranslation<DBOJsonSchemaBlob, DBOJsonSchemaBlob> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOJsonSchemaBlob> getBackupClass() {
		return DBOJsonSchemaBlob.class;
	}

	@Override
	public Class<? extends DBOJsonSchemaBlob> getDatabaseObjectClass() {
		return DBOJsonSchemaBlob.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	/**
	 * @return the blobId
	 */
	public Long getBlobId() {
		return blobId;
	}

	/**
	 * @param blobId the blobId to set
	 */
	public void setBlobId(Long blobId) {
		this.blobId = blobId;
	}

	/**
	 * @return the json
	 */
	public String getJson() {
		return json;
	}

	/**
	 * @param json the json to set
	 */
	public void setJson(String json) {
		this.json = json;
	}

	/**
	 * @return the sha256Hex
	 */
	public String getSha256Hex() {
		return sha256Hex;
	}

	/**
	 * @param sha256Hex the sha256Hex to set
	 */
	public void setSha256Hex(String sha256Hex) {
		this.sha256Hex = sha256Hex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(blobId, json, sha256Hex);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOJsonSchemaBlob)) {
			return false;
		}
		DBOJsonSchemaBlob other = (DBOJsonSchemaBlob) obj;
		return Objects.equals(blobId, other.blobId) && Objects.equals(json, other.json)
				&& Objects.equals(sha256Hex, other.sha256Hex);
	}

	@Override
	public String toString() {
		return "DBOJsonSchemaBlob [blobId=" + blobId + ", json=" + json + ", sha256Hex=" + sha256Hex + "]";
	}

}
