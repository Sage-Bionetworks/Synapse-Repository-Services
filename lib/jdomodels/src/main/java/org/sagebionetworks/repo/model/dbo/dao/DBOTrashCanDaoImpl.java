package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTrashedEntity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOTrashCanDaoImpl implements TrashCanDao {
	private static final String NUM_DAYS_PARAM_NAME = "num_days_param";
	private static final String IDS_PARAM_NAME = "ids_param";
	
	private static final String SELECT_COUNT =
			"SELECT COUNT("+ COL_TRASH_CAN_NODE_ID + ") FROM " + TABLE_TRASH_CAN;

	private static final String SELECT_COUNT_FOR_USER =
			"SELECT COUNT("+ COL_TRASH_CAN_NODE_ID + ") FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY;

	private static final String LIMIT_OFFSET = " LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_TRASH =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ LIMIT_OFFSET;

	private static final String SELECT_TRASH_ORDER_BY_ID =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " ORDER BY " + COL_TRASH_CAN_NODE_ID
			+ LIMIT_OFFSET;

	private static final String SELECT_TRASH_FOR_USER =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ LIMIT_OFFSET;

	private static final String SELECT_TRASH_FOR_USER_ORDER_BY_ID =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ " ORDER BY " + COL_TRASH_CAN_NODE_ID
			+ LIMIT_OFFSET;

	private static final String SELECT_TRASH_BY_NODE_ID_FOR_USER =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_DELETED_BY + " = :" + COL_TRASH_CAN_DELETED_BY
			+ " AND " + COL_TRASH_CAN_NODE_ID + " = :" + COL_TRASH_CAN_NODE_ID;

	private static final String SELECT_TRASH_BY_NODE_ID =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_NODE_ID + " = :" + COL_TRASH_CAN_NODE_ID;

	private static final String SELECT_TRASH_BEFORE_TIMESTAMP =
			"SELECT * FROM " + TABLE_TRASH_CAN +
			" WHERE " + COL_TRASH_CAN_DELETED_ON + " < :" + COL_TRASH_CAN_DELETED_ON +
			" ORDER BY " + COL_TRASH_CAN_NODE_ID;
	
	private static final String DELETE_TRASH_BY_IDS = 
			"DELETE FROM " + TABLE_TRASH_CAN + 
			" WHERE " + COL_TRASH_CAN_NODE_ID + " IN (:"+IDS_PARAM_NAME+")";
	
	/*
	  SELECTs trash nodes that are more than NUM_DAYS days old with no children nodes
   	 
   	  The use of NOT EXISTS instead of NOT IN is an optimization. 
	  mySQL is supposed to automatically do it but the query execution plan showed that it does not do it correctly
	  http://dev.mysql.com/doc/refman/5.7/en/subquery-optimization.html
	  
	  Also, the reason we only want children nodes is that mySQL's innoDB 
	  has a limit of 15 levels cascade deletes when the foreign key is self referential
	  http://dev.mysql.com/doc/refman/5.7/en/innodb-foreign-key-constraints.html
	*/
	private static final String SELECT_TRASH_BEFORE_NUM_DAYS_LEAVES_ONLY =
			"SELECT " + COL_TRASH_CAN_NODE_ID +
			" FROM " + TABLE_TRASH_CAN + " T1" +
			" WHERE T1." + COL_TRASH_CAN_DELETED_ON + " <= (NOW() - INTERVAL :" + NUM_DAYS_PARAM_NAME +" DAY)" +
			" AND NOT EXISTS (SELECT 1 FROM " + TABLE_TRASH_CAN+" T2"+
							" WHERE T2." +COL_TRASH_CAN_PARENT_ID + " = T1." + COL_TRASH_CAN_NODE_ID + ")"+
			" ORDER BY " + COL_TRASH_CAN_NODE_ID + 
			" LIMIT :" + LIMIT_PARAM_NAME;
	
	private static final RowMapper<DBOTrashedEntity> rowMapper = (new DBOTrashedEntity()).getTableMapping();
	
	//rowMapper used in getTrashNumDaysOldNoChildren()
	private static final RowMapper<Long> rowMapperNodeId = new RowMapper<Long>(){
		@Override
		public
		Long mapRow(ResultSet rs, int rowNum) throws SQLException{
			return rs.getLong(COL_TRASH_CAN_NODE_ID);
		}
	};
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@WriteTransaction
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

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, KeyFactory.stringToKey(userGroupId));
		Long count = namedParameterJdbcTemplate.queryForObject(SELECT_COUNT_FOR_USER, paramMap, Long.class);
		return count.intValue();
	}

	@Override
	public int getCount() throws DatastoreException {
		//no parameters on this query but need to pass a SqlParameterSource anyways
		Long count = namedParameterJdbcTemplate.queryForObject(SELECT_COUNT, new MapSqlParameterSource(), Long.class); 
		
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
	public TrashedEntity getTrashedEntity(String nodeId) throws DatastoreException {

		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_NODE_ID, KeyFactory.stringToKey(nodeId));
		List<DBOTrashedEntity> trashList = namedParameterJdbcTemplate.query(SELECT_TRASH_BY_NODE_ID, paramMap, rowMapper);
		if (trashList == null || trashList.size() == 0) {
			return null;
		}
		if (trashList.size() > 1) {
			throw new DatastoreException("Node " + nodeId + " has more than 1 trash entry.");
		}
		return TrashedEntityUtils.convertDboToDto(trashList.get(0));
	}

	@Override
	public List<TrashedEntity> getInRangeForUser(String userGroupId, boolean sortById, long offset, long limit) throws DatastoreException {

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
		List<DBOTrashedEntity> trashList = namedParameterJdbcTemplate.query(sortById ? SELECT_TRASH_FOR_USER_ORDER_BY_ID : SELECT_TRASH_FOR_USER,
				paramMap, rowMapper);
		return TrashedEntityUtils.convertDboToDto(trashList);
	}

	@Override
	public List<TrashedEntity> getInRange(boolean sortById, long offset, long limit) throws DatastoreException {

		if (offset < 0) {
			throw new IllegalArgumentException("offset " + offset + " is < 0.");
		}
		if (limit < 0) {
			throw new IllegalArgumentException("limit " + limit + " is < 0.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(OFFSET_PARAM_NAME, offset);
		paramMap.addValue(LIMIT_PARAM_NAME, limit);
		List<DBOTrashedEntity> trashList = namedParameterJdbcTemplate.query(sortById ? SELECT_TRASH_ORDER_BY_ID : SELECT_TRASH, paramMap, rowMapper);
		return TrashedEntityUtils.convertDboToDto(trashList);
	}
	
	@Deprecated
	@Override
	public List<TrashedEntity> getTrashBefore(Timestamp timestamp) throws DatastoreException {

		if(timestamp == null){
			throw new IllegalArgumentException("Time stamp cannot be null.");
		}
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_DELETED_ON, timestamp);
		List<DBOTrashedEntity> trashList = namedParameterJdbcTemplate.query(
				SELECT_TRASH_BEFORE_TIMESTAMP, paramMap, rowMapper);

		return TrashedEntityUtils.convertDboToDto(trashList);
	}
	
	@Override
	public List<Long> getTrashLeaves(long numDays, long limit) throws DatastoreException{
		ValidateArgument.requirement(numDays >= 0, "numDays must not be negative");
		ValidateArgument.requirement(limit >= 0, "limit must not be negative");
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(NUM_DAYS_PARAM_NAME, numDays);
		paramMap.addValue(LIMIT_PARAM_NAME, limit);
		
		return namedParameterJdbcTemplate.query(SELECT_TRASH_BEFORE_NUM_DAYS_LEAVES_ONLY, paramMap, rowMapperNodeId);
	}
	
	
	
	@WriteTransaction
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
			basicDao.deleteObjectByPrimaryKey(DBOTrashedEntity.class, params);
		}
	}
	
	
	@WriteTransaction
	@Override
	public int delete(List<Long> nodeIds) throws DatastoreException, NotFoundException {
		ValidateArgument.required(nodeIds, "nodeIds");
		if(nodeIds.isEmpty()){ 
			//no need to update database if not deleting anything
			return 0;
		}
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(IDS_PARAM_NAME, nodeIds); 
		return namedParameterJdbcTemplate.update(DELETE_TRASH_BY_IDS, params);
		
	}

	private List<TrashedEntity> getNodeList(Long userGroupId, Long nodeId) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_TRASH_CAN_DELETED_BY, userGroupId);
		paramMap.addValue(COL_TRASH_CAN_NODE_ID, nodeId);
		List<DBOTrashedEntity> trashList = namedParameterJdbcTemplate.query(SELECT_TRASH_BY_NODE_ID_FOR_USER, paramMap, rowMapper);
		if (trashList.size() > 1) {
			throw new DatastoreException("User " + userGroupId + ", node " + nodeId + " has more than 1 trash entry.");
		}
		return TrashedEntityUtils.convertDboToDto(trashList);
	}
}
