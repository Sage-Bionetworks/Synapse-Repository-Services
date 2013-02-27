package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTrashedEntity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOTrashCanDaoImpl implements DBOTrashCanDao {

	private static final String SELECT_COUNT_FOR_USER =
			"SELECT COUNT("+ COL_TRASH_CAN_NODE_ID + ") FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY;

	private static final String SELECT_TRASH_FOR_USER =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ " LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_TRASH_BY_NODE_ID_FOR_USER =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ " AND " + COL_TRASH_CAN_NODE_ID + " = :" + COL_TRASH_CAN_NODE_ID;

	private static final RowMapper<DBOTrashedEntity> rowMapper = (new DBOTrashedEntity()).getTableMapping();

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void create(String userGroupId, String nodeId, String nodeName, String parentId) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}
		if (nodeName == null || nodeName.isEmpty()) {
			throw new IllegalArgumentException("nodeName cannot be null or empty.");
		}
		if (parentId == null) {
			throw new IllegalArgumentException("parentId cannot be null.");
		}

		DBOTrashedEntity dbo = new DBOTrashedEntity();
		dbo.setNodeId(KeyFactory.stringToKey(nodeId));
		dbo.setNodeName(nodeName);
		dbo.setDeletedBy(KeyFactory.stringToKey(userGroupId));
		DateTime dt = DateTime.now();
		// MySQL TIMESTAMP only keeps seconds (not ms) so for consistency we only write seconds
		long nowInSeconds = dt.getMillis() - dt.getMillisOfSecond();
		Timestamp ts = new Timestamp(nowInSeconds);
		dbo.setDeletedOn(ts);
		dbo.setParentId(KeyFactory.stringToKey(parentId));
		this.basicDao.createNew(dbo);
	}

	@Override
	public int getCount(String userGroupId) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();;
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, KeyFactory.stringToKey(userGroupId));
		Long count = simpleJdbcTemplate.queryForLong(SELECT_COUNT_FOR_USER, paramMap);
		return count.intValue();
	}

	@Override
	public boolean exists(String userGroupId, String nodeId) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		List<TrashedEntity> trashList = getNodeList(KeyFactory.stringToKey(userGroupId), KeyFactory.stringToKey(nodeId));
		return (trashList != null && trashList.size() > 0);
	}

	@Override
	public TrashedEntity getTrashedEntity(String userGroupId, String nodeId)
			throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		List<TrashedEntity> trashList = getNodeList(KeyFactory.stringToKey(userGroupId), KeyFactory.stringToKey(nodeId));
		if (trashList == null || trashList.size() == 0) {
			return null;
		}
		return trashList.get(0);
	}

	@Override
	public List<TrashedEntity> getInRangeForUser(String userGroupId, long offset,
			long limit) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("offset " + offset + " is < 0.");
		}
		if (limit < 0) {
			throw new IllegalArgumentException("limit " + limit + " is < 0.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(OFFSET_PARAM_NAME, offset);
		paramMap.addValue(LIMIT_PARAM_NAME, limit);
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, KeyFactory.stringToKey(userGroupId));
		List<DBOTrashedEntity> trashList = simpleJdbcTemplate.query(SELECT_TRASH_FOR_USER, rowMapper, paramMap);
		return TrashedEntityUtils.convertDboToDto(trashList);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String userGroupId, String nodeId)
			throws DatastoreException, NotFoundException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		// SELECT then DELETE to avoid deadlocks caused by gap locks
		List<TrashedEntity> trashList = getNodeList(KeyFactory.stringToKey(userGroupId), KeyFactory.stringToKey(nodeId));
		for (TrashedEntity trash : trashList) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("nodeId", KeyFactory.stringToKey(trash.getEntityId()));
			basicDao.deleteObjectById(DBOTrashedEntity.class, params);
		}
	}

	private List<TrashedEntity> getNodeList(Long userGroupId, Long nodeId) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, userGroupId);
		paramMap.addValue(COL_TRASH_CAN_NODE_ID, nodeId);
		List<DBOTrashedEntity> trashList = simpleJdbcTemplate.query(SELECT_TRASH_BY_NODE_ID_FOR_USER, rowMapper, paramMap);
		if (trashList.size() > 1) {
			throw new DatastoreException("User " + userGroupId + ", node " + nodeId + " has more than 1 trash entry.");
		}
		return TrashedEntityUtils.convertDboToDto(trashList);
	}
}
