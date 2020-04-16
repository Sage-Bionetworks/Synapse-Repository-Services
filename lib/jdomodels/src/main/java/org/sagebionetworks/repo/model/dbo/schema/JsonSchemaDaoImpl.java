package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_SHA256;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ORG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_BLOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_SEMANTIC;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_VERSION;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.SchemaInfo;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JsonSchemaDaoImpl implements JsonSchemaDao {

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;

	public static final RowMapper<SchemaInfo> SCHEMA_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		SchemaInfo info = new SchemaInfo();
		info.setNumericId(rs.getString(COL_JSON_SCHEMA_ID));
		info.setOrganizationId(rs.getString(COL_JSON_SCHEMA_ORG_ID));
		info.setName(rs.getString(COL_JSON_SCHEMA_NAME));
		info.setCreatedBy(rs.getString(COL_JSON_SCHEMA_CREATED_BY));
		info.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_CREATED_ON));
		return info;
	};

	public static final RowMapper<JsonSchemaVersionInfo> SCHEMA_VERSION_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		JsonSchemaVersionInfo info = new JsonSchemaVersionInfo();
		info.setVersionId(rs.getString(COL_JSON_SCHEMA_VER_ID));
		info.setSemanticVersion(rs.getString(COL_JSON_SCHEMA_VER_SEMANTIC));
		info.setCreatedBy(rs.getString(COL_JSON_SCHEMA_VER_CREATED_BY));
		info.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_VER_CREATED_ON));
		info.setJsonSHA256Hex(rs.getString(COL_JSON_SCHEMA_BLOB_SHA256));
		return info;
	};

	@WriteTransaction
	@Override
	public SchemaInfo createSchemaIfDoesNotExist(SchemaInfo schemaRoot) {
		ValidateArgument.required(schemaRoot, "SchemaInfo");
		ValidateArgument.required(schemaRoot.getOrganizationId(), "schema.organizationId");
		ValidateArgument.required(schemaRoot.getName(), "schema.name");
		ValidateArgument.required(schemaRoot.getCreatedBy(), "schema.createdBy");
		ValidateArgument.required(schemaRoot.getCreatedOn(), "schema.createdOn");
		try {
			// Try to get the schema if it already exists
			return getSchemaInfoForUpdate(schemaRoot.getOrganizationId(), schemaRoot.getName());
		} catch (NotFoundException e) {
			Long numericId = idGenerator.generateNewId(IdType.JSON_SCHEMA_ID);
			// For concurrent calls the loser's data will be ignored.
			jdbcTemplate.update(
					"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA + " (" + COL_JSON_SCHEMA_ID + "," + COL_JSON_SCHEMA_ORG_ID
							+ "," + COL_JSON_SCHEMA_NAME + "," + COL_JSON_SCHEMA_CREATED_BY + ","
							+ COL_JSON_SCHEMA_CREATED_ON + ") VALUES (?,?,?,?,?)",
					numericId, schemaRoot.getOrganizationId(), schemaRoot.getName(), schemaRoot.getCreatedBy(),
					schemaRoot.getCreatedOn());
			return getSchemaInfoForUpdate(schemaRoot.getOrganizationId(), schemaRoot.getName());
		}
	}

	@Override
	public SchemaInfo getSchemaInfoForUpdate(String organizationId, String schemaName) {
		ValidateArgument.required(organizationId, "organizationId");
		ValidateArgument.required(schemaName, "schemaName");
		try {
			return jdbcTemplate
					.queryForObject(
							"SELECT * FROM " + TABLE_JSON_SCHEMA + " WHERE " + COL_JSON_SCHEMA_ORG_ID + " = ? AND "
									+ COL_JSON_SCHEMA_NAME + " = ? FOR UPDATE",
							SCHEMA_INFO_MAPPER, organizationId, schemaName);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JsonSchema not found for organizationId: '" + organizationId
					+ "' and schemaName: '" + schemaName + "'");
		}
	}

	@Override
	public void trunacteAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_VERSION);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_BLOB);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA);
	}

	@WriteTransaction
	@Override
	public String createJsonBlobIfDoesNotExist(String json, String sha256hex) {
		ValidateArgument.required(json, "json");
		ValidateArgument.required(sha256hex, "sha256hex");
		try {
			return getJsonBlobId(sha256hex);
		} catch (NotFoundException e) {
			Long blobId = idGenerator.generateNewId(IdType.JSON_SCHEMA_BLOB_ID);
			jdbcTemplate.update(
					"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA_BLOB + " (" + COL_JSON_SCHEMA_BLOB_ID + ","
							+ COL_JSON_SCHEMA_BLOB_BLOB + "," + COL_JSON_SCHEMA_BLOB_SHA256 + ") VALUES (?,?,?)",
					blobId, json, sha256hex);
			return getJsonBlobId(sha256hex);
		}
	}

	@Override
	public String getJsonBlobId(String sha256hex) {
		ValidateArgument.required(sha256hex, "sha256hex");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_JSON_SCHEMA_BLOB_ID + " FROM " + TABLE_JSON_SCHEMA_BLOB
					+ " WHERE " + COL_JSON_SCHEMA_BLOB_SHA256 + " = ?", String.class, sha256hex);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON blob does not exist for sha256Hex: " + sha256hex);
		}
	}

	@WriteTransaction
	@Override
	public String createNewVersion(NewVersionRequest request) {
		ValidateArgument.required(request, "NewVersionRequest");
		ValidateArgument.required(request.getSchemaId(), "schemaId");
		ValidateArgument.required(request.getCreatedBy(), "createdBy");
		ValidateArgument.required(request.getCreatedOn(), "createdOn");
		ValidateArgument.required(request.getBlobId(), "blobId");

		Long versionId = idGenerator.generateNewId(IdType.JSON_SCHEMA_VERSION_ID);
		DBOJsonSchemaVersion dbo = new DBOJsonSchemaVersion();
		dbo.setVersionId(versionId);
		dbo.setSchemaId(Long.parseLong(request.getSchemaId()));
		dbo.setSemanticVersion(request.getSemanticVersion());
		dbo.setCreatedBy(request.getCreatedBy());
		dbo.setCreatedOn(new Timestamp(request.getCreatedOn().getTime()));
		dbo.setBlobId(Long.parseLong(request.getBlobId()));
		basicDao.createNew(dbo);
		return versionId.toString();
	}

	@Override
	public JsonSchemaVersionInfo getVersionInfo(String versionId) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT V.*, B." + COL_JSON_SCHEMA_BLOB_SHA256 + " FROM " + TABLE_JSON_SCHEMA_VERSION
							+ " V INNER JOIN " + TABLE_JSON_SCHEMA_BLOB + " B ON (V." + COL_JSON_SCHEMA_VER_BLOB_ID
							+ "=B." + COL_JSON_SCHEMA_BLOB_ID + ") WHERE V." + COL_JSON_SCHEMA_VER_ID + "=?",
					SCHEMA_VERSION_INFO_MAPPER, versionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON version not found for versionId: " + versionId);
		}
	}

}
