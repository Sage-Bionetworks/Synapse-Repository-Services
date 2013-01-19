package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_TRASH_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTrash;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOTrashCanDaoImpl implements DBOTrashCanDao {

	private static final String SELECT_TRASH_FOR_USER =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ " LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_TRASH_ID =
			"SELECT " + COL_TRASH_CAN_TRASH_ID + " FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ " AND " + COL_TRASH_CAN_NODE_ID + " = :" + COL_TRASH_CAN_NODE_ID;

	private static final String SELECT_TRASH_ID_BY_NODE_ID =
			"SELECT " + COL_TRASH_CAN_TRASH_ID + " FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_NODE_ID + " = :" + COL_TRASH_CAN_NODE_ID;

	private static final RowMapper<DBOTrash> rowMapper = (new DBOTrash()).getTableMapping();
	private static final RowMapper<Long> idRowMapper = ParameterizedSingleColumnRowMapper.newInstance(Long.class);

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void create(Long userGroupId, Long nodeId) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		DBOTrash dbo = new DBOTrash();
		dbo.setNodeId(nodeId);
		dbo.setDeletedBy(userGroupId);
		DateTime dt = DateTime.now();
		// MySQL TIMESTAMP only keeps seconds (not ms) so for consistency we only write seconds
		long nowInSeconds = dt.getMillis() - dt.getMillisOfSecond();
		Timestamp ts = new Timestamp(nowInSeconds);
		dbo.setDeletedOn(ts);
		this.basicDao.createNew(dbo);
	}

	@Override
	public List<DBOTrash> getInRangeForUser(Long userGroupId, Long beginIncl,
			Long endExcl) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end [begin=" + beginIncl;
			msg += ", end=";
			msg += endExcl;
			msg += "]";
			throw new IllegalArgumentException(msg);
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(OFFSET_PARAM_NAME, beginIncl);
		paramMap.addValue(LIMIT_PARAM_NAME, endExcl - beginIncl);
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, userGroupId);
		List<DBOTrash> trashList = simpleJdbcTemplate.query(SELECT_TRASH_FOR_USER, rowMapper, paramMap);
		return Collections.unmodifiableList(trashList);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(Long userGroupId, Long nodeId)
			throws DatastoreException, NotFoundException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		// SELECT then DELETE avoid deadlocks caused by gap locks
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, userGroupId);
		paramMap.addValue(COL_TRASH_CAN_NODE_ID, nodeId);
		List<Long> idList = simpleJdbcTemplate.query(SELECT_TRASH_ID, idRowMapper, paramMap);
		for (Long trashId : idList) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("trashId", trashId);
			basicDao.deleteObjectById(DBOTrash.class, params);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteByNodeId(Long nodeId) throws DatastoreException {

		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		// SELECT then DELETE avoid deadlocks caused by gap locks
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_NODE_ID, nodeId);
		List<Long> idList = simpleJdbcTemplate.query(SELECT_TRASH_ID_BY_NODE_ID, idRowMapper, paramMap);
		for (Long trashId : idList) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("trashId", trashId);
			basicDao.deleteObjectById(DBOTrash.class, params);
		}
	}
}
