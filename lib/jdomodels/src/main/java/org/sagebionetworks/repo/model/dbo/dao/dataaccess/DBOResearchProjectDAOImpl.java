package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject create(ResearchProject toCreate) {
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProjectUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		return get(toCreate.getAccessRequirementId(), toCreate.getOwnerId());
	}

	@Override
	public ResearchProject get(String accessRequirementId, String ownerId) throws NotFoundException {
		List<DBOResearchProject> dboList = jdbcTemplate.query(SQL_GET, new RowMapper<DBOResearchProject>(){
			@Override
			public DBOResearchProject mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOResearchProject dbo = new DBOResearchProject();
				dbo.setId(rs.getLong(RESEARCH_PROJECT_ID));
				dbo.setAccessRequirementId(rs.getLong(RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID));
				dbo.setCreatedBy(rs.getLong(RESEARCH_PROJECT_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(RESEARCH_PROJECT_CREATED_ON));
				dbo.setModifiedBy(rs.getLong(RESEARCH_PROJECT_MODIFIED_BY));
				dbo.setModifiedOn(rs.getLong(RESEARCH_PROJECT_MODIFIED_ON));
				dbo.setOwnerId(rs.getLong(RESEARCH_PROJECT_OWNER_ID));
				dbo.setEtag(rs.getString(RESEARCH_PROJECT_ETAG));
				dbo.setProjectLead(rs.getString(RESEARCH_PROJECT_PROJECT_LEAD));
				dbo.setInstitution(rs.getString(RESEARCH_PROJECT_INSTITUTION));
				Blob blob = rs.getBlob(RESEARCH_PROJECT_IDU);
				dbo.setIdu(blob.getBytes(1, (int) blob.length()));
				return dbo;
			}
		}, accessRequirementId, ownerId);
		if (dboList.isEmpty()) {
			throw new NotFoundException();
		}
		if (dboList.size() != 1) {
			throw new DatastoreException();
		}
		ResearchProject dto = new ResearchProject();
		ResearchProjectUtils.copyDboToDto(dboList.get(0), dto);
		return dto;
	}

	@WriteTransactionReadCommitted
	@Override
	public ResearchProject update(ResearchProject toUpdate) throws NotFoundException {
		DBOResearchProject dbo = new DBOResearchProject();
		ResearchProjectUtils.copyDtoToDbo(toUpdate, dbo);
		basicDao.update(dbo);
		return get(toUpdate.getAccessRequirementId(), toUpdate.getOwnerId());
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}
}
