package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
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

	public static final String SQL_DELETE = "DELETE FROM "+TABLE_RESEARCH_PROJECT
			+" WHERE "+RESEARCH_PROJECT_ID+" = ?";

	public static final String SQL_GET = "SELECT *"
			+ " FROM "+TABLE_RESEARCH_PROJECT
			+ " WHERE "+RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+RESEARCH_PROJECT_OWNER_ID+" = ?";

	public static final String SQL_GET_USING_ID = "SELECT *"
			+ " FROM "+TABLE_RESEARCH_PROJECT
			+ " WHERE "+RESEARCH_PROJECT_ID+" = ?";

	public static final String SQL_CHANGE_OWNERSHIP = "UPDATE "+TABLE_RESEARCH_PROJECT
			+ " SET "+RESEARCH_PROJECT_OWNER_ID+" = ?, "
			+ RESEARCH_PROJECT_MODIFIED_BY+" = ?, "
			+ RESEARCH_PROJECT_MODIFIED_ON+" = ?, "
			+ RESEARCH_PROJECT_ETAG+" = ? "
			+ " WHERE "+RESEARCH_PROJECT_ID+" = ?";

	private final RowMapper<DBOResearchProject> MAPPER = new DBOResearchProject().getTableMapping();

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject create(ResearchProject toCreate) {
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProjectUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		return get(toCreate.getId());
	}

	@Override
	public ResearchProject get(String accessRequirementId, String ownerId) throws NotFoundException {
		try {
			DBOResearchProject dbo = jdbcTemplate.queryForObject(SQL_GET, MAPPER, accessRequirementId, ownerId);
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
		basicDao.update(dbo);
		return get(toUpdate.getId());
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject changeOwnership(String researchProjectId, String newOwnerId,
			String modifiedBy, Long modifiedOn, String etag) throws NotFoundException {
		jdbcTemplate.update(SQL_CHANGE_OWNERSHIP, newOwnerId, modifiedBy, modifiedOn, etag, researchProjectId);
		return get(researchProjectId);
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
}
