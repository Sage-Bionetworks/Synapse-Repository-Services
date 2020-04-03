package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ORGANIZATION;

import java.sql.Timestamp;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.schema.Organization;
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
public class OrganizationDaoImpl implements OrganizationDao {

	@Autowired
	DBOBasicDao basicDao;
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	JdbcTemplate jdbcTemplate;

	static final RowMapper<DBOOrganization> ROW_MAPPER = new DBOOrganization().getTableMapping();

	@WriteTransaction
	@Override
	public Organization createOrganization(String name, Long createdBy) {
		ValidateArgument.required(name, "name");
		ValidateArgument.required(createdBy, "createdBy");
		DBOOrganization dbo = new DBOOrganization();
		dbo.setName(name.toLowerCase());
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
			DBOOrganization dbo = jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_NAME + " = ?", ROW_MAPPER,
					name.toLowerCase());
			return createDtoFromDbo(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Organization with name: '" + name.toLowerCase() + "' not found");
		}
	}

	/**
	 * Create a DTO from the DBO.
	 * 
	 * @param dbo
	 * @return
	 */
	public static Organization createDtoFromDbo(DBOOrganization dbo) {
		Organization dto = new Organization();
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setId(dbo.getId().toString());
		dto.setName(dbo.getName());
		return dto;
	}

	@WriteTransaction
	@Override
	public void deleteOrganization(String id) {
		ValidateArgument.required(id, "id");
		int count = jdbcTemplate.update(
				"DELETE FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_ID + " = ?", id);
		if (count < 1) {
			throw new NotFoundException("Organization with id: '" + id + "' not found");
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_ID + " > -1");
	}

}
