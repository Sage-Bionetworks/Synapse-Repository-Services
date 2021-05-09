package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_REQUEST;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBORequestDAOImpl implements RequestDAO{

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

	private static final RowMapper<DBORequest> MAPPER = new DBORequest().getTableMapping();

	@WriteTransaction
	@Override
	public Request create(Request toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_REQUEST_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		return (Request) getUserOwnCurrentRequest(toCreate.getAccessRequirementId(), toCreate.getCreatedBy());
	}

	@Override
	public RequestInterface getUserOwnCurrentRequest(String accessRequirementId, String userId)
			throws NotFoundException {
		try {
			DBORequest dbo = jdbcTemplate.queryForObject(SQL_GET, MAPPER, accessRequirementId, userId);
			RequestInterface dto = RequestUtils.copyDboToDto(dbo);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransaction
	@Override
	public RequestInterface update(RequestInterface toUpdate) throws NotFoundException {
		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(toUpdate, dbo);
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
	public RequestInterface getForUpdate(String id) {
		try {
			DBORequest dbo = jdbcTemplate.queryForObject(SQL_GET_FOR_UPDATE, MAPPER, id);
			RequestInterface dto = RequestUtils.copyDboToDto(dbo);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public RequestInterface get(String id) {
		try {
			DBORequest dbo = jdbcTemplate.queryForObject(SQL_GET_BY_ID, MAPPER, id);
			RequestInterface dto = RequestUtils.copyDboToDto(dbo);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_DATA_ACCESS_REQUEST);
	}

}
