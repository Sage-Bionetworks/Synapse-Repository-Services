package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_ORG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_JSON_SCHEMA;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
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

	public static final RowMapper<SchemaInfo> SCHEMA_INFO_MAPPER = new RowMapper<SchemaInfo>() {

		@Override
		public SchemaInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			SchemaInfo info = new SchemaInfo();
			info.setNumericId(rs.getString(COL_JSON_SCHEMA_ID));
			info.setOrganizationId(rs.getString(COL_JSON_SCHEMA_ORG_ID));
			info.setName(rs.getString(COL_JSON_SCHEMA_NAME));
			info.setCreatedBy(rs.getString(COL_JSON_SCHEMA_CREATED_BY));
			info.setCreatedOn(rs.getTimestamp(COL_JSON_SCHEMA_CREATED_ON));
			return info;
		}
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
			return jdbcTemplate.queryForObject("SELECT * FROM " + TABLE_JSON_SCHEMA + " WHERE " + COL_JSON_SCHEMA_ORG_ID
					+ " = ? AND " + COL_JSON_SCHEMA_NAME + " = ? FOR UPDATE", SCHEMA_INFO_MAPPER, organizationId, schemaName);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("JsonSchema not found for organizationId: '" + organizationId + "' and schemaName: '" + schemaName + "'");
		}
	}

	@Override
	public void trunacteAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA);
	}

}
