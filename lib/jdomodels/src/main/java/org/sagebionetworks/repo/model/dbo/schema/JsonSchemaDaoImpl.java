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
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
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

		// By inserting a row without an ID we ensure concurrent calls lock on the new
		// row.
		int updateCount = jdbcTemplate.update(
				"INSERT IGNORE INTO " + TABLE_JSON_SCHEMA + " (" + COL_JSON_SCHEMA_ORG_ID + "," + COL_JSON_SCHEMA_NAME
						+ "," + COL_JSON_SCHEMA_CREATED_BY + "," + COL_JSON_SCHEMA_CREATED_ON + ") VALUES (?,?,?,?)",
				schemaRoot.getOrganizationId(), schemaRoot.getName(), schemaRoot.getCreatedBy(), schemaRoot.getCreatedOn());
		if (updateCount > 0) {
			// The row did not already exist so we need to issue an ID to this schema
			Long numericId = idGenerator.generateNewId(IdType.JSON_SCHEMA_ID);
			jdbcTemplate.update(
					"UPDATE " + TABLE_JSON_SCHEMA + " SET " + COL_JSON_SCHEMA_ID + " = ? WHERE "
							+ COL_JSON_SCHEMA_ORG_ID + " = ? AND " + COL_JSON_SCHEMA_NAME + " = ?",
					numericId, schemaRoot.getOrganizationId(), schemaRoot.getName());
		}
		return getSchemaInfo(schemaRoot.getOrganizationId(), schemaRoot.getName());
	}

	@Override
	public SchemaInfo getSchemaInfo(String organizationName, String schemaName) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		return jdbcTemplate.queryForObject("SELECT * FROM " + TABLE_JSON_SCHEMA + " WHERE " + COL_JSON_SCHEMA_ORG_ID
				+ " = ? AND " + COL_JSON_SCHEMA_NAME + " = ?", SCHEMA_INFO_MAPPER, organizationName, schemaName);
	}

	@Override
	public void trunacteAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_JSON_SCHEMA);
	}

}
