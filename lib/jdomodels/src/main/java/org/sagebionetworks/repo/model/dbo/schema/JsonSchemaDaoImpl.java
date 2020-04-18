package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
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
import org.springframework.dao.DuplicateKeyException;
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
	public SchemaInfo createSchemaIfDoesNotExist(NewSchemaRequest request) {
		ValidateArgument.required(request, "NewSchemaRequest");
		ValidateArgument.required(request.getOrganizationId(), "request.organizationId");
		ValidateArgument.required(request.getSchemaName(), "request.schemaName");
		ValidateArgument.required(request.getCreatedBy(), "request.createdBy");
		try {
			// Try to get the schema if it already exists
			return getSchemaInfoForUpdate(request.getOrganizationId(), request.getSchemaName());
		} catch (NotFoundException e) {
			Long numericId = idGenerator.generateNewId(IdType.JSON_SCHEMA_ID);
			Timestamp now = new Timestamp(System.currentTimeMillis());
			// For concurrent calls the loser's data will be ignored.
			jdbcTemplate.update(
					"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA + " (" + COL_JSON_SCHEMA_ID + "," + COL_JSON_SCHEMA_ORG_ID
							+ "," + COL_JSON_SCHEMA_NAME + "," + COL_JSON_SCHEMA_CREATED_BY + ","
							+ COL_JSON_SCHEMA_CREATED_ON + ") VALUES (?,?,?,?,?)",
					numericId, request.getOrganizationId(), request.getSchemaName(), request.getCreatedBy(), now);
			return getSchemaInfoForUpdate(request.getOrganizationId(), request.getSchemaName());
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
	public JsonSchemaVersionInfo createNewVersion(NewVersionRequest request) {
		ValidateArgument.required(request, "NewVersionRequest");
		ValidateArgument.required(request.getSchemaId(), "schemaId");
		ValidateArgument.required(request.getCreatedBy(), "createdBy");
		ValidateArgument.required(request.getBlobId(), "blobId");

		Long versionId = idGenerator.generateNewId(IdType.JSON_SCHEMA_VERSION_ID);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		try {
			jdbcTemplate.update(
					"INSERT INTO " + TABLE_JSON_SCHEMA_VERSION + " (" + COL_JSON_SCHEMA_VER_ID + ","
							+ COL_JSON_SCHEMA_VER_SCHEMA_ID + "," + COL_JSON_SCHEMA_VER_SEMANTIC + ","
							+ COL_JSON_SCHEMA_VER_CREATED_BY + "," + COL_JSON_SCHEMA_VER_CREATED_ON + ","
							+ COL_JSON_SCHEMA_VER_BLOB_ID + ") VALUES (?,?,?,?,?,?)",
					versionId, request.getSchemaId(), request.getSemanticVersion(), request.getCreatedBy(), now,
					request.getBlobId());
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(
					"Semantic version: '" + request.getSemanticVersion() + "' already exists for this JSON schema");
		}
		return getVersionInfo(versionId.toString());
	}

	@Override
	public JsonSchemaVersionInfo getVersionInfo(String versionId) {
		ValidateArgument.required(versionId, "versionId");
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
