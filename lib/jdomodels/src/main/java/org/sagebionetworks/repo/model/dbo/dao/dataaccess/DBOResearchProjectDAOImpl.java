package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOResearchProjectDAOImpl implements ResearchProjectDAO{

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

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

	@WriteTransactionReadCommitted
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
	public ResearchProject getUserOwnResearchProject(String accessRequirementId, String createdBy) throws NotFoundException {
		try {
			DBOResearchProject dbo = jdbcTemplate.queryForObject(SQL_GET, MAPPER, accessRequirementId, createdBy);
			ResearchProject dto = new ResearchProject();
			ResearchProjectUtils.copyDboToDto(dbo, dto);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject update(ResearchProject toUpdate) throws NotFoundException {
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProjectUtils.copyDtoToDbo(toUpdate, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.update(dbo);
		return get(toUpdate.getId());
	}

	@WriteTransactionReadCommitted
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
			throw new NotFoundException();
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
			throw new NotFoundException();
		}
	}
}
