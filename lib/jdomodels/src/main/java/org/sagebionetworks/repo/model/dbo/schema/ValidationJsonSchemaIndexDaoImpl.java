package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VALIDATION_JSON_SCHEMA_INDEX;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ValidationJsonSchemaIndexDaoImpl implements ValidationJsonSchemaIndexDao {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public JsonSchema getValidationSchema(String versionId) {
		DBOValidationJsonSchemaIndex dbo = basicDao
				.getObjectByPrimaryKey(DBOValidationJsonSchemaIndex.class,
						new SinglePrimaryKeySqlParameterSource(versionId))
				.orElseThrow(() -> new NotFoundException(String.format("Validation schema for version: '%s' does not exist", versionId)));
		JsonSchema dto = convertFromDBOtoDTO(dbo);
		return dto;

	}
	
	@WriteTransaction
	@Override
	public void createOrUpdate(String versionId, JsonSchema schema) {
		DBOValidationJsonSchemaIndex dbo = convertFromDTOtoDBO(versionId, schema);
		basicDao.createOrUpdate(dbo);
	}
	
	@Override
	@WriteTransaction
	public void delete(String versionId) {
		basicDao.deleteObjectByPrimaryKey(DBOValidationJsonSchemaIndex.class, 
				new SinglePrimaryKeySqlParameterSource(versionId));
	}

	@Override
	@WriteTransaction
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_VALIDATION_JSON_SCHEMA_INDEX);
	}
	
	DBOValidationJsonSchemaIndex convertFromDTOtoDBO(String versionId, JsonSchema schema) {
		DBOValidationJsonSchemaIndex dbo = new DBOValidationJsonSchemaIndex();
		dbo.setVersionId(Long.parseLong(versionId));
		try {
			dbo.setValidationSchema(EntityFactory.createJSONStringForEntity(schema));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		return dbo;
	}
	
	JsonSchema convertFromDBOtoDTO(DBOValidationJsonSchemaIndex dbo) {
		String schemaString = dbo.getValidationSchema();
		try {
			JsonSchema dto = EntityFactory.createEntityFromJSONString(schemaString, JsonSchema.class);
			return dto;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
}
