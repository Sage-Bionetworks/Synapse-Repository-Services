package org.sagebionetworks.repo.model.dbo.trash;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_PRIORITY_PURGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DBOTrashCanDaoImpl implements TrashCanDao {

	private static final String NUM_DAYS_PARAM_NAME = "num_days_param";
	private static final String IDS_PARAM_NAME = "ids_param";

	private static final String SELECT_COUNT = "SELECT COUNT(" + COL_TRASH_CAN_NODE_ID + ") FROM " + TABLE_TRASH_CAN;

	private static final String SELECT_EXISTS_BY_PARENT_ID = "SELECT EXISTS (SELECT 1 FROM " + TABLE_TRASH_CAN  +
			" WHERE " + COL_TRASH_CAN_PARENT_ID + " = ? LIMIT 1)";

	private static final String SELECT_TRASH_FOR_USER = "SELECT * FROM " + TABLE_TRASH_CAN + " WHERE " + COL_TRASH_CAN_DELETED_BY + " = ?"
			+ " AND " + COL_TRASH_CAN_PRIORITY_PURGE + " = FALSE ORDER BY " + COL_TRASH_CAN_DELETED_ON + " DESC LIMIT ? OFFSET ?";

	private static final String SELECT_TRASH_BY_NODE_ID = "SELECT * FROM " + TABLE_TRASH_CAN + " WHERE " + COL_TRASH_CAN_NODE_ID + " = ?"
			+ " AND " + COL_TRASH_CAN_PRIORITY_PURGE + " = FALSE";

	private static final String DELETE_TRASH_BY_IDS = "DELETE FROM " + TABLE_TRASH_CAN + " WHERE " + COL_TRASH_CAN_NODE_ID + " IN (:"
			+ IDS_PARAM_NAME + ")";

	/*
	 * SELECTs trash nodes that are more than NUM_DAYS days old with no children nodes
	 * 
	 * The use of NOT EXISTS instead of NOT IN is an optimization. mySQL is supposed to automatically do it but the query
	 * execution plan showed that it does not do it correctly
	 * http://dev.mysql.com/doc/refman/5.7/en/subquery-optimization.html
	 * 
	 * Also, the reason we only want children nodes is that mySQL's innoDB has a limit of 15 levels cascade deletes when the
	 * foreign key is self referential http://dev.mysql.com/doc/refman/5.7/en/innodb-foreign-key-constraints.html
	 */
	private static final String SELECT_TRASH_BEFORE_NUM_DAYS_LEAVES_ONLY = "SELECT " + COL_TRASH_CAN_NODE_ID + " FROM " + TABLE_TRASH_CAN
			+ " T1" + " WHERE (T1." + COL_TRASH_CAN_PRIORITY_PURGE + " = TRUE " + " OR T1." + COL_TRASH_CAN_DELETED_ON
			+ " <= (NOW() - INTERVAL :" + NUM_DAYS_PARAM_NAME + " DAY))" + " AND NOT EXISTS (SELECT 1 FROM " + TABLE_TRASH_CAN + " T2"
			+ " WHERE T2." + COL_TRASH_CAN_PARENT_ID + " = T1." + COL_TRASH_CAN_NODE_ID + ")" + " ORDER BY " + COL_TRASH_CAN_DELETED_ON
			+ " DESC LIMIT :" + LIMIT_PARAM_NAME;

	private static final String SQL_FLAG_NODES_FOR_PURGE = "UPDATE " + TABLE_TRASH_CAN + " SET " + COL_TRASH_CAN_PRIORITY_PURGE + " = TRUE"
			+ ", " + COL_TRASH_CAN_ETAG + " = UUID()" + " WHERE " + COL_TRASH_CAN_NODE_ID + " IN (:" + IDS_PARAM_NAME + ")";

	private static final RowMapper<DBOTrashedEntity> ROW_MAPPER = new DBOTrashedEntity().getTableMapping();

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@WriteTransaction
	@Override
	public void create(String userGroupId, String nodeId, String nodeName, String parentId, boolean priorityPurge)
			throws DatastoreException {

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
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setPriorityPurge(priorityPurge);

		this.basicDao.createNew(dbo);
	}

	@Override
	public boolean doesEntityHaveTrashedChildren(String entityId) {
		// Query either returns 0 or 1, cannot return null and thus cannot throw Null Pointer Exception.
		//noinspection ConstantConditions
		return jdbcTemplate.queryForObject(SELECT_EXISTS_BY_PARENT_ID, Boolean.class,
				KeyFactory.stringToKey(entityId));
	}

	@Override
	public int getCount() throws DatastoreException {
		// no parameters on this query but need to pass a SqlParameterSource anyways
		Long count = jdbcTemplate.queryForObject(SELECT_COUNT, Long.class);

		return count.intValue();
	}

	@Override
	public TrashedEntity getTrashedEntity(String nodeId) throws DatastoreException {
		ValidateArgument.required(nodeId, "Node id");

		DBOTrashedEntity trashedEntity = null;
		try {
			trashedEntity = jdbcTemplate.queryForObject(SELECT_TRASH_BY_NODE_ID, ROW_MAPPER, KeyFactory.stringToKey(nodeId));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}

		return TrashedEntityUtils.convertDboToDto(trashedEntity);
	}

	@Override
	public List<TrashedEntity> listTrashedEntities(String userGroupId, long offset, long limit) throws DatastoreException {

		if (userGroupId == null) {
			throw new IllegalArgumentException("userGroupId cannot be null.");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("offset " + offset + " is < 0.");
		}
		if (limit < 0) {
			throw new IllegalArgumentException("limit " + limit + " is < 0.");
		}
		
		Long deletedBy = KeyFactory.stringToKey(userGroupId);

		List<DBOTrashedEntity> trashList = jdbcTemplate.query(SELECT_TRASH_FOR_USER, ROW_MAPPER, deletedBy, limit, offset);

		return TrashedEntityUtils.convertDboToDto(trashList);
	}

	@Override
	public List<Long> getTrashLeavesIds(long numDays, long limit) throws DatastoreException {
		ValidateArgument.requirement(numDays >= 0, "numDays must not be negative");
		ValidateArgument.requirement(limit >= 0, "limit must not be negative");

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(NUM_DAYS_PARAM_NAME, numDays);
		paramMap.addValue(LIMIT_PARAM_NAME, limit);

		return namedParameterJdbcTemplate.queryForList(SELECT_TRASH_BEFORE_NUM_DAYS_LEAVES_ONLY, paramMap, Long.class);
	}

	@WriteTransaction
	@Override
	public void flagForPurge(List<Long> nodeIds) {
		ValidateArgument.required(nodeIds, "The list of node ids");

		if (nodeIds.isEmpty()) {
			return;
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource(IDS_PARAM_NAME, nodeIds);

		namedParameterJdbcTemplate.update(SQL_FLAG_NODES_FOR_PURGE, paramMap);

	}

	@WriteTransaction
	@Override
	public int delete(List<Long> nodeIds) throws DatastoreException, NotFoundException {
		ValidateArgument.required(nodeIds, "nodeIds");

		if (nodeIds.isEmpty()) {
			// no need to update database if not deleting anything
			return 0;
		}

		MapSqlParameterSource params = new MapSqlParameterSource(IDS_PARAM_NAME, nodeIds);

		return namedParameterJdbcTemplate.update(DELETE_TRASH_BY_IDS, params);

	}

	@WriteTransaction
	@Override
	public void truncate() {
		jdbcTemplate.update("DELETE FROM " + TABLE_TRASH_CAN);
	}
}
