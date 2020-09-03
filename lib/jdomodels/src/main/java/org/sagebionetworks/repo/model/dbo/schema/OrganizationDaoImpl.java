package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ORGANIZATION;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationDaoImpl implements OrganizationDao {

	private static final String FK_SCHEMA_TO_ORGANIZATION = "FK_SCHEMA_TO_ORGANIZATION";
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	static final RowMapper<Organization> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		Organization organization = new Organization();
		organization.setId(rs.getString(COL_ORGANIZATION_ID));
		organization.setName(rs.getString(COL_ORGANIZATION_NAME));
		organization.setCreatedBy(rs.getString(COL_ORGANIZATION_CREATED_BY));
		organization.setCreatedOn(rs.getTimestamp(COL_ORGANIZATION_CREATED_ON));
		return organization;
	};

	@WriteTransaction
	@Override
	public Organization createOrganization(String name, Long createdBy) {
		ValidateArgument.required(name, "name");
		ValidateArgument.required(createdBy, "createdBy");
		DBOOrganization dbo = new DBOOrganization();
		dbo.setName(name);
		dbo.setCreatedBy(createdBy);
		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setId(idGenerator.generateNewId(IdType.ORGANIZATION_ID));

		try {
			jdbcTemplate.update(
					"INSERT INTO " + TABLE_ORGANIZATION + " (" + COL_ORGANIZATION_ID + "," + COL_ORGANIZATION_NAME + ","
							+ COL_ORGANIZATION_CREATED_BY + "," + COL_ORGANIZATION_CREATED_ON + ") VALUES (?,?,?,?)",
					dbo.getId(), dbo.getName(), dbo.getCreatedBy(), dbo.getCreatedOn());
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("An Organization with the name: '" + dbo.getName() + "' already exists",
					e);
		}

		return getOrganizationByName(dbo.getName());
	}

	@Override
	public Organization getOrganizationByName(String name) {
		ValidateArgument.required(name, "name");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_NAME + " = ?", ROW_MAPPER,
					name);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Organization with name: '" + name + "' not found");
		}
	}

	@WriteTransaction
	@Override
	public void deleteOrganization(String id) {
		ValidateArgument.required(id, "id");
		try {
			int count = jdbcTemplate.update("DELETE FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_ID + " = ?",
					id);
			if (count < 1) {
				throw new NotFoundException("Organization with id: '" + id + "' not found");
			}
		} catch (DataIntegrityViolationException e) {
			if(e.getMessage().contains(FK_SCHEMA_TO_ORGANIZATION)) {
				throw new IllegalArgumentException(
						"All schemas defined under an organization must be deleted before the organization can be deleted.",
						e);
			}else {
				throw e;
			}
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_ID + " > -1");
	}

	@Override
	public List<Organization> listOrganizations(long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM " + TABLE_ORGANIZATION + " ORDER BY " + COL_ORGANIZATION_NAME + " LIMIT ? OFFSET ?",
				ROW_MAPPER, limit, offset);
	}

}
