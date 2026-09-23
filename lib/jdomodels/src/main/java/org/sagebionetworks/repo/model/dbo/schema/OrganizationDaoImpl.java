package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ORGANIZATION;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

	/**
	 * Every foreign key that references ORGANIZATION with ON DELETE RESTRICT. Deleting an
	 * organization that still owns any of these child rows must surface as a bad request rather
	 * than a raw data integrity failure.
	 */
	private static final List<String> ORGANIZATION_CHILD_FKS = List.of("FK_SCHEMA_TO_ORGANIZATION", "SYNSET_ORG_FK",
			"TA_ORG_FK", "CAO_ORG_FK", "SC_ORG_FK");

	private static final String CHILD_FK_MESSAGE = "All resources defined under an organization must be deleted before the organization can be deleted.";

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
		return createOrganization(name, createdBy, idGenerator.generateNewId(IdType.ORGANIZATION_ID));
	}

	@WriteTransaction
	@Override
	public Organization createOrganization(String name, Long createdBy, Long id) {
		ValidateArgument.required(name, "name");
		ValidateArgument.required(createdBy, "createdBy");
		ValidateArgument.required(id, "id");
		DBOOrganization dbo = new DBOOrganization();
		dbo.setName(name);
		dbo.setCreatedBy(createdBy);
		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setId(id);

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

	@Override
	public Optional<Organization> getOrganizationById(String id) {
		ValidateArgument.required(id, "id");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_ORGANIZATION + " WHERE " + COL_ORGANIZATION_ID + " = ?", ROW_MAPPER,
					id));
		}
		catch (EmptyResultDataAccessException e) {
			return Optional.empty();
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
			String message = Objects.toString(e.getMessage(), "");
			if (ORGANIZATION_CHILD_FKS.stream().anyMatch(message::contains)) {
				throw new IllegalArgumentException(CHILD_FK_MESSAGE, e);
			} else {
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
