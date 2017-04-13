package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
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

	@Autowired
	private IdGenerator idGenerator;

	public static final String SQL_DELETE = "DELETE FROM "+TABLE_DATA_ACCESS_REQUEST
			+ " WHERE "+COL_DATA_ACCESS_REQUEST_ID+" = ?";

	public static final String SQL_GET = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_REQUEST
			+ " WHERE "+COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+COL_DATA_ACCESS_REQUEST_CREATED_BY+" = ?";

	public static final String SQL_GET_BY_ID = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_REQUEST
			+ " WHERE "+COL_DATA_ACCESS_REQUEST_ID+" = ?";

	public static final String SQL_GET_FOR_UPDATE = SQL_GET_BY_ID+" FOR UPDATE";

	private static final RowMapper<DBODataAccessRequest> MAPPER = new DBODataAccessRequest().getTableMapping();

	@WriteTransactionReadCommitted
	@Override
	public DataAccessRequest create(DataAccessRequest toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_REQUEST_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		DBODataAccessRequest dbo = new DBODataAccessRequest();
		DataAccessRequestUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		return (DataAccessRequest) getUserOwnCurrentRequest(toCreate.getAccessRequirementId(), toCreate.getCreatedBy());
	}

	@Override
	public DataAccessRequestInterface getUserOwnCurrentRequest(String accessRequirementId, String userId)
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
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.update(dbo);
		return getUserOwnCurrentRequest(toUpdate.getAccessRequirementId(), toUpdate.getCreatedBy());
	}

	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
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

	@Override
	public DataAccessRequestInterface get(String id) {
		try {
			DBODataAccessRequest dbo = jdbcTemplate.queryForObject(SQL_GET_BY_ID, MAPPER, id);
			DataAccessRequestInterface dto = DataAccessRequestUtils.copyDboToDto(dbo);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}
}
