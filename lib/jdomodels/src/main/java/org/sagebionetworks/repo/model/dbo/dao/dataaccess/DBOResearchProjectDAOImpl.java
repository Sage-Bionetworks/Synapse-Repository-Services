package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESEARCH_PROJECT;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DBOResearchProjectDAOImpl implements ResearchProjectDAO{

	public static final String RESEARCH_PROJECT_DOES_NOT_EXIST = "Research project: '%s' does not exist";

	private final DBOBasicDao basicDao;
	private final JdbcTemplate jdbcTemplate;
	private final IdGenerator idGenerator;

	public DBOResearchProjectDAOImpl(DBOBasicDao basicDao, JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	public static final String SQL_DELETE = "DELETE FROM "+TABLE_RESEARCH_PROJECT
			+" WHERE "+COL_RESEARCH_PROJECT_ID+" = ?";

	public static final String SQL_GET = "SELECT *"
			+ " FROM "+TABLE_RESEARCH_PROJECT
			+ " WHERE "+COL_RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+COL_RESEARCH_PROJECT_CREATED_BY+" = ?";

	public static final String SQL_GET_USING_ID = "SELECT *"
			+ " FROM "+TABLE_RESEARCH_PROJECT
			+ " WHERE "+COL_RESEARCH_PROJECT_ID+" = ?";

	public static final String SQL_GET_USING_ID_FOR_UPDATE = SQL_GET_USING_ID + " FOR UPDATE";

	private final RowMapper<DBOResearchProject> MAPPER = new DBOResearchProject().getTableMapping();

	@WriteTransaction
	@Override
	public ResearchProject create(ResearchProject toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.RESEARCH_PROJECT_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProjectUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		return get(toCreate.getId());
	}

	@Override
	public ResearchProject getUserOwnResearchProject(String accessRequirementId, String createdBy)
			throws NotFoundException {
		try {
			DBOResearchProject dbo = jdbcTemplate.queryForObject(SQL_GET, MAPPER, accessRequirementId, createdBy);
			ResearchProject dto = new ResearchProject();
			ResearchProjectUtils.copyDboToDto(dbo, dto);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(
					String.format("Research project does not exist for access requirement: '%s' and created by: '%s'",
							accessRequirementId, createdBy));
		}
	}

	@WriteTransaction
	@Override
	public ResearchProject update(ResearchProject toUpdate) throws NotFoundException {
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProjectUtils.copyDtoToDbo(toUpdate, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.update(dbo);
		return get(toUpdate.getId());
	}

	@WriteTransaction
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}

	@Override
	public ResearchProject get(String researchProjectId) {
		try {
			DBOResearchProject dbo = jdbcTemplate.queryForObject(SQL_GET_USING_ID, MAPPER, researchProjectId);
			ResearchProject dto = new ResearchProject();
			ResearchProjectUtils.copyDboToDto(dbo, dto);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(RESEARCH_PROJECT_DOES_NOT_EXIST, researchProjectId));
		}
	}

	@MandatoryWriteTransaction
	@Override
	public ResearchProject getForUpdate(String researchProjectId) {
		try {
			DBOResearchProject dbo = jdbcTemplate.queryForObject(SQL_GET_USING_ID_FOR_UPDATE, MAPPER, researchProjectId);
			ResearchProject dto = new ResearchProject();
			ResearchProjectUtils.copyDboToDto(dbo, dto);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(RESEARCH_PROJECT_DOES_NOT_EXIST, researchProjectId));
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_RESEARCH_PROJECT);
	}

}
