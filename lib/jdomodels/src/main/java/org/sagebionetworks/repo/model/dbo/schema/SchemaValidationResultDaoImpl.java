package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_ALL_ERRORS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_EXCEPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_IS_VALID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_VALIDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SCHEMA_VALIDATION_RESULTS;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SchemaValidationResultDaoImpl implements SchemaValidationResultDao {

	public static final String VALIDATION_EXCEPTION = "validationException";
	public static final String ALL_VALIDATION_MESSAGES = "allValidationMessages";
	private JdbcTemplate jdbcTemplate;
	private DBOBasicDao basicDao;

	@Autowired
	public SchemaValidationResultDaoImpl(JdbcTemplate jdbcTemplate, DBOBasicDao basicDao) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.basicDao = basicDao;
	}

	public static RowMapper<ValidationResults> VALIDATION_RESULT_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		ValidationResults dto = new ValidationResults();
		String allErrorsJson = rs.getString(COL_JSON_SCHEMA_VALIDATION_ALL_ERRORS);
		String validationExceptionJson = rs.getString(COL_JSON_SCHEMA_VALIDATION_EXCEPTION);
		JSONObject json = new JSONObject();
		if (allErrorsJson != null) {
			json.put(ALL_VALIDATION_MESSAGES, new JSONArray(allErrorsJson));
		}
		if (validationExceptionJson != null) {
			json.put(VALIDATION_EXCEPTION, new JSONObject(validationExceptionJson));
		}
		try {
			dto.initializeFromJSONObject(new JSONObjectAdapterImpl(json));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		dto.setObjectType(ObjectType.valueOf(rs.getString(COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE)));
		Long objectIdLong = rs.getLong(COL_JSON_SCHEMA_VALIDATION_OBJECT_ID);
		if (ObjectType.entity.equals(dto.getObjectType())) {
			dto.setObjectId(KeyFactory.keyToString(objectIdLong));
		} else {
			dto.setObjectId(objectIdLong.toString());
		}
		dto.setObjectEtag(rs.getString(COL_JSON_SCHEMA_VALIDATION_OBJECT_ETAG));
		dto.setSchema$id(rs.getString(COL_JSON_SCHEMA_VALIDATION_SCHEMA_ID));
		dto.setIsValid(rs.getBoolean(COL_JSON_SCHEMA_VALIDATION_IS_VALID));
		dto.setValidatedOn(rs.getTimestamp(COL_JSON_SCHEMA_VALIDATION_VALIDATED_ON));
		dto.setValidationErrorMessage(rs.getString(COL_JSON_SCHEMA_VALIDATION_ERROR_MESSAGE));
		return dto;
	};

	@WriteTransaction
	@Override
	public void clearResults(String objectid, ObjectType objectType) {
		ValidateArgument.required(objectid, "objectId");
		ValidateArgument.required(objectType, "objectType");
		jdbcTemplate.update(
				"DELETE FROM " + TABLE_SCHEMA_VALIDATION_RESULTS + " WHERE " + COL_JSON_SCHEMA_VALIDATION_OBJECT_ID
						+ " = ? AND " + COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE + " = ?",
				KeyFactory.stringToKey(objectid), objectType.name());
	}

	@WriteTransaction
	@Override
	public void createOrUpdateResults(ValidationResults results) {
		DBOSchemaValidationResults dbo = translateDTOtoDBO(results);
		basicDao.createOrUpdate(dbo);
	}

	/**
	 * Translate from the given DTO to a DBO.
	 * 
	 * @param results
	 * @return
	 */
	static DBOSchemaValidationResults translateDTOtoDBO(ValidationResults results) {
		ValidateArgument.required(results, "results");
		ValidateArgument.required(results.getObjectId(), "results.objectId");
		ValidateArgument.required(results.getObjectType(), "results.objectType");
		ValidateArgument.required(results.getObjectEtag(), "results.objectEtag");
		ValidateArgument.required(results.getSchema$id(), "results.schema$id");
		ValidateArgument.required(results.getIsValid(), "results.isValid");
		ValidateArgument.required(results.getValidatedOn(), "results.validateOn");

		JSONObject json = new JSONObject();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(json);
		try {
			results.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
		DBOSchemaValidationResults dbo = new DBOSchemaValidationResults();
		dbo.setObjectId(KeyFactory.stringToKey(results.getObjectId()));
		dbo.setObjectType(results.getObjectType().toString());
		dbo.setObjectEtag(results.getObjectEtag());
		dbo.setSchema$id(results.getSchema$id());
		dbo.setIsValid(results.getIsValid());
		dbo.setValidatedOn(new Timestamp(results.getValidatedOn().getTime()));
		dbo.setErrorMessage(results.getValidationErrorMessage());
		if (json.has(ALL_VALIDATION_MESSAGES)) {
			JSONArray allMessages = json.getJSONArray(ALL_VALIDATION_MESSAGES);
			dbo.setAllErrorMessages(allMessages.toString());
		}
		if (json.has(VALIDATION_EXCEPTION)) {
			JSONObject exception = json.getJSONObject(VALIDATION_EXCEPTION);
			dbo.setValidationException(exception.toString());
		}
		return dbo;
	}

	@Override
	public ValidationResults getValidationResults(String objectid, ObjectType objectType) {
		ValidateArgument.required(objectid, "objectId");
		ValidateArgument.required(objectType, "objectType");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_SCHEMA_VALIDATION_RESULTS + " WHERE "
							+ COL_JSON_SCHEMA_VALIDATION_OBJECT_ID + " = ? AND "
							+ COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE + " = ?",
					VALIDATION_RESULT_ROW_MAPPER, KeyFactory.stringToKey(objectid), objectType.name());
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("ValidationResults do not exist for objectId: '" + objectid
					+ "' and objectType: '" + objectType + "'");
		}
	}

	@Override
	public void clearAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_SCHEMA_VALIDATION_RESULTS + " WHERE "
				+ COL_JSON_SCHEMA_VALIDATION_OBJECT_ID + "  > -1");
	}

}
