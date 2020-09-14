package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JONS_SCHEMA_BINDING_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_BIND_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BINDING_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_BLOB_SHA256;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEPENDENCY_DEPENDS_ON_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_DEPEPNDENCY_DEPENDS_ON_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_LATEST_VER_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_LATEST_VER_VER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ORG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_BLOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VER_SEMANTIC;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_DEPENDENCY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_LATEST_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_OBJECT_BINDING;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ORGANIZATION;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.NormalizedJsonSchema;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JsonSchemaDaoImpl implements JsonSchemaDao {

	public static final String FK_SCHEMA_DEPENDS_ON_SCHEMA_ID = "FK_SCHEMA_DEPENDS_ON_SCHEMA_ID";
	private static final String FK_SCHEMA_DEPENDS_ON_VERSION_ID = "FK_SCHEMA_DEPENDS_ON_VERSION_ID";
	private static final String FK_BOUND_DEPENDS_ON_SCHEMA_ID = "FK_BOUND_DEPENDS_ON_SCHEMA_ID";
	private static final String FK_BOUND_DEPENDS_ON_VERSION_ID = "FK_BOUND_DEPENDS_ON_VERSION_ID";

	public static final int MAX_SCHEMA_NAME_CHARS = 250;
	public static final int MAX_SEMANTIC_VERSION_CHARS = 250;

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public static final RowMapper<JsonSchemaVersionInfo> SCHEMA_VERSION_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		JsonSchemaVersionInfo info = new JsonSchemaVersionInfo();
		info.setOrganizationId(rs.getString(COL_ORGANIZATION_ID));
		info.setOrganizationName(rs.getString(COL_ORGANIZATION_NAME));
		info.setSchemaId(rs.getString(COL_JSON_SCHEMA_ID));
		info.setSchemaName(rs.getString(COL_JSON_SCHEMA_NAME));
		info.setVersionId(rs.getString(COL_JSON_SCHEMA_VER_ID));
		info.setSemanticVersion(rs.getString(COL_JSON_SCHEMA_VER_SEMANTIC));
		info.setCreatedBy(rs.getString(COL_JSON_SCHEMA_VER_CREATED_BY));
		info.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_VER_CREATED_ON));
		info.setJsonSHA256Hex(rs.getString(COL_JSON_SCHEMA_BLOB_SHA256));
		StringBuilder $idBuilder = new StringBuilder(info.getOrganizationName());
		$idBuilder.append(JsonSchemaConstants.PATH_DELIMITER);
		$idBuilder.append(info.getSchemaName());
		if(info.getSemanticVersion() != null) {
			$idBuilder.append(JsonSchemaConstants.VERSION_PRFIX);
			$idBuilder.append(info.getSemanticVersion());
		}
		info.set$id($idBuilder.toString());
		return info;
	};

	public static final RowMapper<JsonSchemaInfo> SCHEMA_INFO_MAPPER = (ResultSet rs, int rowNum) -> {
		JsonSchemaInfo info = new JsonSchemaInfo();
		info.setOrganizationId(rs.getString(COL_ORGANIZATION_ID));
		info.setOrganizationName(rs.getString(COL_ORGANIZATION_NAME));
		info.setSchemaId(rs.getString(COL_JSON_SCHEMA_ID));
		info.setSchemaName(rs.getString(COL_JSON_SCHEMA_NAME));
		info.setCreatedBy(rs.getString(COL_JSON_SCHEMA_CREATED_BY));
		info.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_CREATED_ON));
		return info;
	};

	public static final RowMapper<JsonSchema> SCHEMA_MAPPER = (ResultSet rs, int rowNum) -> {
		String json = rs.getString(COL_JSON_SCHEMA_BLOB_BLOB);
		try {
			return EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	};

	public static final RowMapper<DBOJsonSchemaDependency> DEPENDENCY_MAPPER = new DBOJsonSchemaDependency()
			.getTableMapping();
	public static final RowMapper<DBOJsonSchemaBindObject> BIND_OBJECT_MAPPER = new DBOJsonSchemaBindObject()
			.getTableMapping();

	String createSchemaIfDoesNotExist(String organizationId, String schemaName, Long createdBy) {
		ValidateArgument.required(organizationId, "organizationId");
		ValidateArgument.required(schemaName, "schemaName");
		ValidateArgument.required(createdBy, "createdBy");
		try {
			// Try to get the schema if it already exists
			return getSchemaInfoForUpdate(organizationId, schemaName);
		} catch (NotFoundException e) {
			Long numericId = idGenerator.generateNewId(IdType.JSON_SCHEMA_ID);
			Timestamp now = new Timestamp(System.currentTimeMillis());
			// For concurrent calls the loser's data will be ignored.
			jdbcTemplate.update(
					"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA + " (" + COL_JSON_SCHEMA_ID + "," + COL_JSON_SCHEMA_ORG_ID
							+ "," + COL_JSON_SCHEMA_NAME + "," + COL_JSON_SCHEMA_CREATED_BY + ","
							+ COL_JSON_SCHEMA_CREATED_ON + ") VALUES (?,?,?,?,?)",
					numericId, organizationId, schemaName, createdBy, now);
			return getSchemaInfoForUpdate(organizationId, schemaName);
		}
	}

	String getSchemaInfoForUpdate(String organizationId, String schemaName) {
		ValidateArgument.required(organizationId, "organizationId");
		ValidateArgument.required(schemaName, "schemaName");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT " + COL_JSON_SCHEMA_ID + " FROM " + TABLE_JSON_SCHEMA + " WHERE " + COL_JSON_SCHEMA_ORG_ID
							+ " = ? AND " + COL_JSON_SCHEMA_NAME + " = ? FOR UPDATE",
					String.class, organizationId, schemaName);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JsonSchema not found for organizationId: '" + organizationId
					+ "' and schemaName: '" + schemaName + "'");
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_OBJECT_BINDING);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_DEPENDENCY);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_LATEST_VERSION);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_VERSION);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_BLOB);
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA);
	}

	String createJsonBlobIfDoesNotExist(JsonSchema schema) {
		ValidateArgument.required(schema, "schema");
		NormalizedJsonSchema normalized = new NormalizedJsonSchema(schema);
		try {
			return getJsonBlobId(normalized.getSha256Hex());
		} catch (NotFoundException e) {
			Long blobId = idGenerator.generateNewId(IdType.JSON_SCHEMA_BLOB_ID);
			jdbcTemplate.update(
					"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA_BLOB + " (" + COL_JSON_SCHEMA_BLOB_ID + ","
							+ COL_JSON_SCHEMA_BLOB_BLOB + "," + COL_JSON_SCHEMA_BLOB_SHA256 + ") VALUES (?,?,?)",
					blobId, normalized.getNormalizedSchemaJson(), normalized.getSha256Hex());
			return getJsonBlobId(normalized.getSha256Hex());
		}
	}

	String getJsonBlobId(String sha256hex) {
		ValidateArgument.required(sha256hex, "sha256hex");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_JSON_SCHEMA_BLOB_ID + " FROM " + TABLE_JSON_SCHEMA_BLOB
					+ " WHERE " + COL_JSON_SCHEMA_BLOB_SHA256 + " = ?", String.class, sha256hex);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON blob does not exist for sha256Hex: " + sha256hex);
		}
	}

	/**
	 * Set the cached latest version for the given schema
	 * 
	 * @param schemaId
	 * @param latestVersionId
	 */
	private void setLatestVersion(String schemaId, Long latestVersionId) {
		jdbcTemplate.update(
				"INSERT INTO " + TABLE_JSON_SCHEMA_LATEST_VERSION + " (" + COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID + ","
						+ COL_JSON_SCHEMA_LATEST_VER_ETAG + "," + COL_JSON_SCHEMA_LATEST_VER_VER_ID
						+ ") VALUES (?,UUID(),?) ON DUPLICATE KEY UPDATE " + COL_JSON_SCHEMA_LATEST_VER_ETAG
						+ " = UUID(), " + COL_JSON_SCHEMA_LATEST_VER_VER_ID + " = ?",
				schemaId, latestVersionId, latestVersionId);
	}

	String getLatestVersionEtag(String schemaId) {
		return jdbcTemplate.queryForObject("SELECT " + COL_JSON_SCHEMA_LATEST_VER_ETAG + " FROM "
				+ TABLE_JSON_SCHEMA_LATEST_VERSION + " WHERE " + COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID + " = ?",
				String.class, schemaId);
	}

	/**
	 * Create a new version for the given schemaId and set the latest version to
	 * point to the results.
	 * 
	 * @param schemaId
	 * @param semanticVersion
	 * @param createdBy
	 * @param blobId
	 * @return
	 */
	JsonSchemaVersionInfo createNewVersion(String schemaId, String semanticVersion, Long createdBy, String blobId) {
		ValidateArgument.required(schemaId, "schemaId");
		ValidateArgument.required(createdBy, "createdBy");
		ValidateArgument.required(blobId, "blobId");

		Long versionId = idGenerator.generateNewId(IdType.JSON_SCHEMA_VERSION_ID);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		try {
			// insert the version row
			jdbcTemplate.update(
					"INSERT INTO " + TABLE_JSON_SCHEMA_VERSION + " (" + COL_JSON_SCHEMA_VER_ID + ","
							+ COL_JSON_SCHEMA_VER_SCHEMA_ID + "," + COL_JSON_SCHEMA_VER_SEMANTIC + ","
							+ COL_JSON_SCHEMA_VER_CREATED_BY + "," + COL_JSON_SCHEMA_VER_CREATED_ON + ","
							+ COL_JSON_SCHEMA_VER_BLOB_ID + ") VALUES (?,?,?,?,?,?)",
					versionId, schemaId, semanticVersion, createdBy, now, blobId);
			// set this version to be the latest
			setLatestVersion(schemaId, versionId);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(
					"Semantic version: '" + semanticVersion + "' already exists for this JSON schema");
		}
		return getVersionInfo(versionId.toString());
	}

	@Override
	public JsonSchemaVersionInfo getVersionInfo(String versionId) {
		ValidateArgument.required(versionId, "versionId");
		try {
			return jdbcTemplate.queryForObject("SELECT O." + COL_ORGANIZATION_ID + ", O." + COL_ORGANIZATION_NAME
					+ ", S." + COL_JSON_SCHEMA_ID + ", S." + COL_JSON_SCHEMA_NAME + ", V.*, B."
					+ COL_JSON_SCHEMA_BLOB_SHA256 + " FROM " + TABLE_JSON_SCHEMA_VERSION + " V INNER JOIN "
					+ TABLE_JSON_SCHEMA_BLOB + " B ON (V." + COL_JSON_SCHEMA_VER_BLOB_ID + "=B."
					+ COL_JSON_SCHEMA_BLOB_ID + ") JOIN " + TABLE_JSON_SCHEMA + " S ON (V."
					+ COL_JSON_SCHEMA_VER_SCHEMA_ID + " = S." + COL_JSON_SCHEMA_ID + " ) JOIN " + TABLE_ORGANIZATION
					+ " O ON (S." + COL_JSON_SCHEMA_ORG_ID + " = O." + COL_ORGANIZATION_ID + ") WHERE V."
					+ COL_JSON_SCHEMA_VER_ID + "=?", SCHEMA_VERSION_INFO_MAPPER, versionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON version not found for versionId: " + versionId);
		}
	}

	@WriteTransaction
	@Override
	public JsonSchemaVersionInfo createNewSchemaVersion(NewSchemaVersionRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getOrganizationId(), "request.organizationId");
		ValidateArgument.required(request.getCreatedBy(), "request.createdBy");
		ValidateArgument.required(request.getSchemaName(), "request.schemaName");
		ValidateArgument.required(request.getJsonSchema(), "request.jsonSchema");
		if (request.getSchemaName().length() > MAX_SCHEMA_NAME_CHARS) {
			throw new IllegalArgumentException("Schema name must be " + MAX_SCHEMA_NAME_CHARS + " characters or less");
		}
		if (request.getSemanticVersion() != null) {
			if (request.getSemanticVersion().length() > MAX_SEMANTIC_VERSION_CHARS) {
				throw new IllegalArgumentException(
						"Semantic version must be " + MAX_SEMANTIC_VERSION_CHARS + " characters or less");
			}
		}
		String schemaId = createSchemaIfDoesNotExist(request.getOrganizationId(), request.getSchemaName(),
				request.getCreatedBy());
		String blobId = createJsonBlobIfDoesNotExist(request.getJsonSchema());
		JsonSchemaVersionInfo info = createNewVersion(schemaId, request.getSemanticVersion(), request.getCreatedBy(),
				blobId);
		bindDependencies(info.getVersionId(), request.getDependencies());
		return info;
	}

	@WriteTransaction
	@Override
	public void deleteSchema(String schemaId) {
		ValidateArgument.required(schemaId, "schemaId");
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_LATEST_VERSION + " WHERE "
				+ COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID + " = ?", schemaId);
		try {
			jdbcTemplate.update(
					"DELETE FROM " + TABLE_JSON_SCHEMA_VERSION + " WHERE " + COL_JSON_SCHEMA_VER_SCHEMA_ID + " = ?",
					schemaId);
			jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA + " WHERE " + COL_JSON_SCHEMA_ID + " = ?",
					schemaId);
		} catch (DataIntegrityViolationException e) {
			boolean isSchemaVersion = false;
			handelDataIntegrityViolationExceptionOnDelete(isSchemaVersion, e);
		}
	}
	
	/**
	 * Handle 
	 * @param schemaId
	 * @param e
	 */
	void handelDataIntegrityViolationExceptionOnDelete(boolean isSchemaVersion, DataIntegrityViolationException e) {
		String message = e.getMessage();
		StringBuilder builder = new StringBuilder("Cannot delete a schema");
		if(isSchemaVersion) {
			builder.append(" version");
		}
		if (message.contains(FK_SCHEMA_DEPENDS_ON_SCHEMA_ID) || message.contains(FK_SCHEMA_DEPENDS_ON_VERSION_ID)) {
			builder.append(" that is referenced by another schema");
		} else if (message.contains(FK_BOUND_DEPENDS_ON_SCHEMA_ID) || message.contains(FK_BOUND_DEPENDS_ON_VERSION_ID)) {
			builder.append(" that is bound to an object");
		} else {
			// Do not know what this is so throw the original exception.
			throw e;
		}
		throw new IllegalArgumentException(builder.toString(), e);
	}

	@Override
	public String getSchemaId(String organizationName, String schemaName) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT " + COL_JSON_SCHEMA_ID + " FROM " + TABLE_JSON_SCHEMA + " S JOIN " + TABLE_ORGANIZATION
							+ " O ON (S." + COL_JSON_SCHEMA_ORG_ID + " = O." + COL_ORGANIZATION_ID + ") WHERE O."
							+ COL_ORGANIZATION_NAME + " = ? AND S." + COL_JSON_SCHEMA_NAME + " = ?",
					String.class, organizationName, schemaName);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON schema not found for organization name '" + organizationName
					+ "' and schema name: '" + schemaName + "'");
		}
	}

	String getSchemaIdForUpdate(String versionId) {
		ValidateArgument.required(versionId, "versionId");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_JSON_SCHEMA_VER_SCHEMA_ID + " FROM "
					+ TABLE_JSON_SCHEMA_VERSION + " WHERE " + COL_JSON_SCHEMA_VER_ID + " = ? FOR UPDATE", String.class,
					versionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON schema not found for versionId: " + versionId);
		}
	}

	@WriteTransaction
	@Override
	public void deleteSchemaVersion(String versionId) {
		ValidateArgument.required(versionId, "versionId");
		// lock on the schema
		String schemaId = getSchemaIdForUpdate(versionId);
		try {
			// clear the latest version cache
			jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA_LATEST_VERSION + " WHERE "
					+ COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID + " = ?", schemaId);
			// delete the requested version
			jdbcTemplate.update(
					"DELETE FROM " + TABLE_JSON_SCHEMA_VERSION + " WHERE " + COL_JSON_SCHEMA_VER_ID + " = ?",
					versionId);
		} catch (DataIntegrityViolationException e) {
			boolean isSchemaVersion = true;
			handelDataIntegrityViolationExceptionOnDelete(isSchemaVersion, e);
		}
		// find the latest version for the schema
		Optional<Long> latestVersionIdOptional = findLatestVersionId(schemaId);
		if (latestVersionIdOptional.isPresent()) {
			setLatestVersion(schemaId, latestVersionIdOptional.get());
		} else {
			// deleted the last version so delete the schema.
			deleteSchema(schemaId);
		}
	}

	@Override
	public String getVersionId(String organizationName, String schemaName, String semanticVersion) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		ValidateArgument.required(semanticVersion, "semanticVersion");
		String sql = "SELECT V." + COL_JSON_SCHEMA_VER_ID + " FROM " + TABLE_ORGANIZATION + " O JOIN "
				+ TABLE_JSON_SCHEMA + " S ON (O." + COL_ORGANIZATION_ID + " = S." + COL_JSON_SCHEMA_ORG_ID + ") JOIN "
				+ TABLE_JSON_SCHEMA_VERSION + " V ON (S." + COL_JSON_SCHEMA_ID + " = V." + COL_JSON_SCHEMA_VER_SCHEMA_ID
				+ ") WHERE O." + COL_ORGANIZATION_NAME + " = ? AND S." + COL_JSON_SCHEMA_NAME + " = ? AND V."
				+ COL_JSON_SCHEMA_VER_SEMANTIC + " = ?";
		try {
			return jdbcTemplate.queryForObject(sql, String.class, organizationName, schemaName, semanticVersion);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON Schema not found for organizationName: '" + organizationName
					+ "' and schemaName: '" + schemaName + "' and semanticVersion: '" + semanticVersion + "'");
		}
	}

	@Override
	public String getLatestVersionId(String organizationName, String schemaName) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		try {
			return jdbcTemplate.queryForObject("SELECT L." + COL_JSON_SCHEMA_LATEST_VER_VER_ID + " FROM "
					+ TABLE_ORGANIZATION + " O JOIN " + TABLE_JSON_SCHEMA + " S ON (O." + COL_ORGANIZATION_ID + " = S."
					+ COL_JSON_SCHEMA_ORG_ID + ") JOIN " + TABLE_JSON_SCHEMA_LATEST_VERSION + " L ON (S."
					+ COL_JSON_SCHEMA_ID + " = L." + COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID + ") WHERE O."
					+ COL_ORGANIZATION_NAME + " = ? AND " + COL_JSON_SCHEMA_NAME + " = ?", String.class,
					organizationName, schemaName);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON Schema not found for organizationName: '" + organizationName
					+ "' and schemaName: '" + schemaName + "'");
		}
	}

	@Override
	public String getLatestVersionId(String schemaId) {
		ValidateArgument.required(schemaId, "schemaId");
		try {
			return jdbcTemplate
					.queryForObject(
							"SELECT " + COL_JSON_SCHEMA_LATEST_VER_VER_ID + " FROM " + TABLE_JSON_SCHEMA_LATEST_VERSION
									+ " WHERE " + COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID + " = ?",
							String.class, schemaId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON Schema not found for schemaId: '" + schemaId + "'");
		}
	}

	@Override
	public JsonSchema getSchema(String versionId) {
		ValidateArgument.required(versionId, "versionId");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT B." + COL_JSON_SCHEMA_BLOB_BLOB + " FROM " + TABLE_JSON_SCHEMA_VERSION + " V JOIN "
							+ TABLE_JSON_SCHEMA_BLOB + " B ON (V." + COL_JSON_SCHEMA_VER_BLOB_ID + " = B."
							+ COL_JSON_SCHEMA_BLOB_ID + ") WHERE V." + COL_JSON_SCHEMA_VER_ID + " = ?",
					SCHEMA_MAPPER, versionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON Schema not found for versionId: '" + versionId + "'");
		}
	}

	@Override
	public JsonSchemaVersionInfo getVersionInfo(String organizationName, String schemaName, String semanticVersion) {
		String versionId = getVersionId(organizationName, schemaName, semanticVersion);
		return getVersionInfo(versionId);
	}

	@Override
	public JsonSchemaVersionInfo getVersionLatestInfo(String organizationName, String schemaName) {
		String versionId = getLatestVersionId(organizationName, schemaName);
		return getVersionInfo(versionId);
	}

	Optional<Long> findLatestVersionId(String schemaId) {
		ValidateArgument.required(schemaId, "schemaId");
		Long versionId = jdbcTemplate.queryForObject("SELECT MAX(" + COL_JSON_SCHEMA_VER_ID + ") FROM "
				+ TABLE_JSON_SCHEMA_VERSION + " WHERE " + COL_JSON_SCHEMA_VER_SCHEMA_ID + " = ?", Long.class, schemaId);
		return Optional.ofNullable(versionId);
	}

	@Override
	public List<JsonSchemaInfo> listSchemas(String organizationName, long limit, long offset) {
		ValidateArgument.required(organizationName, "organizationName");
		return jdbcTemplate.query(
				"SELECT O." + COL_ORGANIZATION_ID + ", O." + COL_ORGANIZATION_NAME + ", S.* FROM " + TABLE_ORGANIZATION
						+ " O JOIN " + TABLE_JSON_SCHEMA + " S ON (O." + COL_ORGANIZATION_ID + " = "
						+ COL_JSON_SCHEMA_ORG_ID + ") WHERE O." + COL_ORGANIZATION_NAME + " = ? ORDER BY S."
						+ COL_JSON_SCHEMA_ID + " LIMIT ? OFFSET ?",
				SCHEMA_INFO_MAPPER, organizationName, limit, offset);
	}

	@Override
	public List<JsonSchemaVersionInfo> listSchemaVersions(String organizationName, String schemaName, long limit,
			long offset) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		return jdbcTemplate.query(
				"SELECT O." + COL_ORGANIZATION_ID + ", O." + COL_ORGANIZATION_NAME + ", S." + COL_JSON_SCHEMA_ID
						+ ", S." + COL_JSON_SCHEMA_NAME + ", V.*, B." + COL_JSON_SCHEMA_BLOB_SHA256 + " FROM "
						+ TABLE_ORGANIZATION + " O JOIN " + TABLE_JSON_SCHEMA + " S ON (O." + COL_ORGANIZATION_ID
						+ " = S." + COL_JSON_SCHEMA_ORG_ID + ") JOIN " + TABLE_JSON_SCHEMA_VERSION + " V ON (S."
						+ COL_JSON_SCHEMA_ID + " = V." + COL_JSON_SCHEMA_VER_SCHEMA_ID + ") JOIN "
						+ TABLE_JSON_SCHEMA_BLOB + " B ON (V." + COL_JSON_SCHEMA_VER_BLOB_ID + " = B."
						+ COL_JSON_SCHEMA_BLOB_ID + ") WHERE O." + COL_ORGANIZATION_NAME + " = ? AND S."
						+ COL_JSON_SCHEMA_NAME + " = ? ORDER BY V." + COL_JSON_SCHEMA_VER_ID + " LIMIT ? OFFSET ?",
				SCHEMA_VERSION_INFO_MAPPER, organizationName, schemaName, limit, offset);
	}

	@WriteTransaction
	void bindDependencies(String versionId, List<SchemaDependency> dependencies) {
		ValidateArgument.required(versionId, "versionId");
		if (dependencies == null || dependencies.isEmpty()) {
			return;
		}
		SchemaDependency[] dependenciesArray = dependencies.toArray(new SchemaDependency[dependencies.size()]);
		jdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA_DEPENDENCY + " (" + COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID
						+ "," + COL_JSON_SCHEMA_DEPEPNDENCY_DEPENDS_ON_SCHEMA_ID + ","
						+ COL_JSON_SCHEMA_DEPENDENCY_DEPENDS_ON_VERSION_ID + ") VALUES (?,?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						SchemaDependency depend = dependenciesArray[i];
						ps.setString(1, versionId);
						ps.setString(2, depend.getDependsOnSchemaId());
						ps.setString(3, depend.getDependsOnVersionId());
					}

					@Override
					public int getBatchSize() {
						return dependenciesArray.length;
					}
				});
	}

	List<DBOJsonSchemaDependency> getDependencies(String versionId) {
		ValidateArgument.required(versionId, "versionId");
		return jdbcTemplate.query("SELECT * FROM " + TABLE_JSON_SCHEMA_DEPENDENCY + " WHERE "
				+ COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID + " = ?", DEPENDENCY_MAPPER, versionId);
	}

	@WriteTransaction
	@Override
	public JsonSchemaObjectBinding bindSchemaToObject(BindSchemaRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSchemaId(), "request.schemaId");
		ValidateArgument.required(request.getObjectId(), "request.objectId");
		ValidateArgument.required(request.getObjectType(), "request.objectType");
		ValidateArgument.required(request.getCreatedBy(), "request.createdBy");
		// remove any existing binding.
		clearBoundSchema(request.getObjectId(), request.getObjectType());

		Timestamp now = new Timestamp(System.currentTimeMillis());
		long bindId = idGenerator.generateNewId(IdType.JSON_SCHEMA_BIND_OBJECT_ID);
		jdbcTemplate.update(
				"INSERT INTO " + TABLE_JSON_SCHEMA_OBJECT_BINDING + "(" + COL_JSON_SCHEMA_BINDING_BIND_ID + ","
						+ COL_JSON_SCHEMA_BINDING_SCHEMA_ID + "," + COL_JSON_SCHEMA_BINDING_VERSION_ID + ","
						+ COL_JONS_SCHEMA_BINDING_OBJECT_ID + "," + COL_JSON_SCHEMA_BINDING_OBJECT_TYPE + ","
						+ COL_JSON_SCHEMA_BINDING_CREATED_BY + "," + COL_JSON_SCHEMA_BINDING_CREATED_ON
						+ ") VALUES (?,?,?,?,?,?,?)",
				bindId, request.getSchemaId(), request.getVersionId(), request.getObjectId(),
				request.getObjectType().name(), request.getCreatedBy(), now);
		return getSchemaBindingForObject(request.getObjectId(), request.getObjectType());
	}

	@Override
	public JsonSchemaObjectBinding getSchemaBindingForObject(Long objectId, BoundObjectType objecType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objecType, "objecType");
		try {
			DBOJsonSchemaBindObject dbo = jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_JSON_SCHEMA_OBJECT_BINDING + " WHERE " + COL_JONS_SCHEMA_BINDING_OBJECT_ID
							+ " = ? AND " + COL_JSON_SCHEMA_BINDING_OBJECT_TYPE + " = ?",
					BIND_OBJECT_MAPPER, objectId, objecType.name());
			String versionId = null;
			if (dbo.getVersionId() == null) {
				versionId = getLatestVersionId(dbo.getSchemaId().toString());
			} else {
				versionId = dbo.getVersionId().toString();
			}
			JsonSchemaVersionInfo versionInfo = getVersionInfo(versionId);
			JsonSchemaObjectBinding result = new JsonSchemaObjectBinding();
			result.setJsonSchemaVersionInfo(versionInfo);
			result.setObjectId(dbo.getObjectId());
			result.setObjectType(BoundObjectType.valueOf(dbo.getObjectType()));
			result.setCreatedBy(dbo.getCreatedBy().toString());
			result.setCreatedOn(dbo.getCreatedOn());
			return result;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JSON Schema binding was not found for ObjectId: '" + objectId
					+ "' ObjectType: '" + objecType.name() + "'");
		}
	}

	@WriteTransaction
	@Override
	public void clearBoundSchema(Long objectId, BoundObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objecType");
		jdbcTemplate.update(
				"DELETE FROM " + TABLE_JSON_SCHEMA_OBJECT_BINDING + " WHERE " + COL_JONS_SCHEMA_BINDING_OBJECT_ID
						+ " = ? AND " + COL_JSON_SCHEMA_BINDING_OBJECT_TYPE + " = ?",
						objectId, objectType.name());
	}

}
