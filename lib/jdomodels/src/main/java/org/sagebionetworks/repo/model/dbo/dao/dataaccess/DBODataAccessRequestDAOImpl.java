package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBODataAccessRequestDAOImpl implements DataAccessRequestDAO{

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public static final String SQL_TRUNCATE = "TRUNCATE "+TABLE_DATA_ACCESS_REQUEST;

	public static final String SQL_GET = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_REQUEST
			+ " WHERE "+DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+DATA_ACCESS_REQUEST_CREATED_BY+" = ?";

	public static final String SQL_GET_FOR_UPDATE = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_REQUEST
			+ " WHERE "+DATA_ACCESS_REQUEST_ID+" = ? FOR UPDATE";

	private static final RowMapper<DBODataAccessRequest> MAPPER = new DBODataAccessRequest().getTableMapping();

	@WriteTransactionReadCommitted
	@Override
	public DataAccessRequestInterface create(DataAccessRequestInterface toCreate) {
		DBODataAccessRequest dbo = new DBODataAccessRequest();
		DataAccessRequestUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		return getCurrentRequest(toCreate.getAccessRequirementId(), toCreate.getCreatedBy());
	}

	@Override
	public DataAccessRequestInterface getCurrentRequest(String accessRequirementId, String userId)
			throws NotFoundException {
		try {
			DBODataAccessRequest dbo = jdbcTemplate.queryForObject(SQL_GET, MAPPER, accessRequirementId, userId);
			DataAccessRequestInterface dto = DataAccessRequestUtils.copyDboToDto(dbo);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessRequestInterface update(DataAccessRequestInterface toUpdate) throws NotFoundException {
		DBODataAccessRequest dbo = new DBODataAccessRequest();
		DataAccessRequestUtils.copyDtoToDbo(toUpdate, dbo);
		basicDao.update(dbo);
		return getCurrentRequest(toUpdate.getAccessRequirementId(), toUpdate.getCreatedBy());
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE);
	}

	@MandatoryWriteTransaction
	@Override
	public DataAccessRequestInterface getForUpdate(String id) {
		try {
			DBODataAccessRequest dbo = jdbcTemplate.queryForObject(SQL_GET_FOR_UPDATE, MAPPER, id);
			DataAccessRequestInterface dto = DataAccessRequestUtils.copyDboToDto(dbo);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}
}
