package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_QUEUE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROCESSED_MESSAGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SENT_MESSAGES;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSentMessage;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


/**
 * The implementation of the change DBOChangeDAO
 * @author John
 *
 */
public class DBOChangeDAOImpl implements DBOChangeDAO {
	
	private static final String BIND_TYPE = "bType";
	private static final String BIND_OBJECT_IDS = "bObjectids";
	
	private static final String SQL_SELECT_CHANGES_BY_OBJECT_IDS_AND_TYPE = 
			"SELECT *"
			+ " FROM "+TABLE_CHANGES
			+" WHERE "+COL_CHANGES_OBJECT_TYPE+" = :"+BIND_TYPE
			+" AND "+COL_CHANGES_OBJECT_ID+" IN (:"+BIND_OBJECT_IDS+")";
	
	public static final String SQL_COUNT_CHANGE_NUMBER = "SELECT COUNT("+COL_CHANGES_CHANGE_NUM+") FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" = ?";

	private static final String SQL_CHANGE_CHECK_SUM_FOR_RANGE = "SELECT SUM(CRC32("+COL_CHANGES_CHANGE_NUM+")) FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" >= ? AND "+COL_CHANGES_CHANGE_NUM+" <= ?";

	private static final String SQL_SENT_CHECK_SUM_FOR_RANGE = "SELECT SUM(CRC32("+COL_SENT_MESSAGES_CHANGE_NUM+")) FROM "+TABLE_SENT_MESSAGES+" WHERE "+COL_SENT_MESSAGES_CHANGE_NUM+" >= ? AND "+COL_SENT_MESSAGES_CHANGE_NUM+" <= ?";

	private static final String SQL_SELECT_MAX_SENT_CHANGE_NUMBER_LESS_THAN_OR_EQUAL = "SELECT MAX("+COL_SENT_MESSAGES_CHANGE_NUM+") FROM "+TABLE_SENT_MESSAGES+" WHERE "+COL_SENT_MESSAGES_CHANGE_NUM+" <= ?";

	static private Logger log = LogManager.getLogger(DBOChangeDAOImpl.class);
	
	private static final String SQL_CHANGES_NOT_SENT_PREFIX = 
			"SELECT C.* FROM "+TABLE_CHANGES+
			" C LEFT OUTER JOIN "+TABLE_SENT_MESSAGES+" S ON (C."+COL_CHANGES_CHANGE_NUM+" = S."+COL_SENT_MESSAGES_CHANGE_NUM+")"+
			"WHERE S."+COL_SENT_MESSAGES_CHANGE_NUM+" IS NULL";
	
	private static final String SQL_SELECT_CHANGES_NOT_SENT = 
			SQL_CHANGES_NOT_SENT_PREFIX + " LIMIT ?";
	
	private static final String SQL_SELECT_CHANGES_NOT_SENT_IN_RANGE = 
			SQL_CHANGES_NOT_SENT_PREFIX+
			" AND C."+COL_SENT_MESSAGES_CHANGE_NUM+" >= ?"+
			" AND C."+COL_SENT_MESSAGES_CHANGE_NUM+" <= ?"+
			" AND C."+COL_CHANGES_TIME_STAMP+" <= ?"+
			" ORDER BY "+COL_SENT_MESSAGES_CHANGE_NUM;
	
	private static final String SELECT_CHANGE_NUMBER_FOR_OBJECT_ID_AND_TYPE = 
			"SELECT "+COL_CHANGES_CHANGE_NUM+" FROM "+TABLE_CHANGES+
			" WHERE "+COL_CHANGES_OBJECT_ID+" = ? AND "+COL_CHANGES_OBJECT_TYPE+" = ?";

	private static final String SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER_FILTER_BY_OBJECT_TYPE =
			"SELECT * FROM "+TABLE_CHANGES+
			" WHERE "+COL_CHANGES_CHANGE_NUM+" >= ? AND "+COL_CHANGES_OBJECT_TYPE+" = ?"+
			" ORDER BY "+COL_CHANGES_CHANGE_NUM+" ASC LIMIT ?";

	private static final String SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER = 
			"SELECT * FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" >= ? ORDER BY "+COL_CHANGES_CHANGE_NUM+" ASC LIMIT ?";

	private static final String SQL_SELECT_MIN_CHANGE_NUMBER = 
			"SELECT MIN("+COL_CHANGES_CHANGE_NUM+") FROM "+TABLE_CHANGES;
	
	private static final String SQL_SELECT_MAX_CHANGE_NUMBER = 
			"SELECT MAX("+COL_CHANGES_CHANGE_NUM+") FROM "+TABLE_CHANGES;
	
	private static final String SQL_SELECT_COUNT_CHANGE_NUMBER = 
			"SELECT COUNT("+COL_CHANGES_CHANGE_NUM+") FROM "+TABLE_CHANGES;

	private static final String SQL_DELETE_BY_CHANGE_NUM = 
			"DELETE FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" = ?";

	private static final String SQL_INSERT_PROCESSED_ON_DUPLICATE_UPDATE =
			"INSERT INTO "+TABLE_PROCESSED_MESSAGES+" ( "+COL_PROCESSED_MESSAGES_CHANGE_NUM+", "+COL_PROCESSED_MESSAGES_QUEUE_NAME+", "+COL_PROCESSED_MESSAGES_TIME_STAMP+") VALUES ( ?, ?, ?) ON DUPLICATE KEY UPDATE "+COL_SENT_MESSAGES_TIME_STAMP+" = ?";

	private static final String SQL_SELECT_CHANGES_NOT_PROCESSED =
			"select c.* from " + TABLE_CHANGES + " c join " + TABLE_SENT_MESSAGES + " s on s." + COL_SENT_MESSAGES_CHANGE_NUM + " = c." + COL_CHANGES_CHANGE_NUM + " left join (select * from " + TABLE_PROCESSED_MESSAGES + " where " + COL_PROCESSED_MESSAGES_QUEUE_NAME + " = ?) p on p." + COL_PROCESSED_MESSAGES_CHANGE_NUM + " = s." + COL_SENT_MESSAGES_CHANGE_NUM + " where p." + COL_PROCESSED_MESSAGES_CHANGE_NUM + " is null limit ?";
	
	private static final String SQL_SENT_CHANGE_NUMBER_FOR_UPDATE = "SELECT "+COL_SENT_MESSAGES_CHANGE_NUM+" FROM "+TABLE_SENT_MESSAGES+" WHERE "+COL_SENT_MESSAGES_OBJECT_ID+" = ? AND "+COL_SENT_MESSAGES_OBJECT_TYPE+" = ? FOR UPDATE";
	private static final String SQL_CHANGE_NUMBER_FOR_UPDATE = "SELECT "+COL_CHANGES_CHANGE_NUM+" FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_OBJECT_ID+" = ? AND "+COL_CHANGES_OBJECT_TYPE+" = ? FOR UPDATE";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;
		
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	Clock clock;
	
	private TableMapping<DBOChange> rowMapper = new DBOChange().getTableMapping();

	@WriteTransaction
	@Override
	public ChangeMessage replaceChange(ChangeMessage change) {
		if(change == null) throw new IllegalArgumentException("DBOChange cannot be null");
		if(change.getObjectId() == null) throw new IllegalArgumentException("change.getObjectId() cannot be null");
		if(change.getChangeType() == null) throw new IllegalArgumentException("change.getChangeTypeEnum() cannot be null");
		if(change.getObjectType() == null) throw new IllegalArgumentException("change.getObjectTypeEnum() cannot be null");
		return attemptReplaceChange(change);
	}

	/**
	 * @param change
	 * @return
	 */
	private ChangeMessage attemptReplaceChange(ChangeMessage change) {
		DBOChange changeDbo = ChangeMessageUtils.createDBO(change);
		/*
		 * Clear the time stamp so that we get a new one automatically. Note:
		 * Mysql TIMESTAMP only keeps seconds (not MS) so for consistency we
		 * only write second accuracy. We are using
		 * (System.currentTimeMillis()/1000)*1000; to convert all MS to zeros.
		 */
		long nowMs = (System.currentTimeMillis() / 1000) * 1000;
		changeDbo.setChangeNumber(idGenerator.generateNewId(IdType.CHANGE_ID));
		changeDbo.setTimeStamp(new Timestamp(nowMs));
		// create or update the change.
		basicDao.createOrUpdate(changeDbo);
		// Setup and clear the sent message
		DBOSentMessage sentDBO = ChangeMessageUtils.createSentDBO(change, null);
		sentDBO.setChangeNumber(null);
		basicDao.createOrUpdate(sentDBO);
		return ChangeMessageUtils.createDTO(changeDbo);
	}

	
	@WriteTransaction
	@Override
	public List<ChangeMessage> replaceChange(List<ChangeMessage> batchDTO) throws TransientDataAccessException {
		if(batchDTO == null) throw new IllegalArgumentException("Batch cannot be null");
		// To prevent deadlock we sort by object id to guarantee a consistent update order.
		batchDTO = ChangeMessageUtils.sortByObjectId(batchDTO);
		// Send each replace them in order.
		List<ChangeMessage> results = new ArrayList<>();
		for(ChangeMessage change: batchDTO){
			results.add(replaceChange(change));
		}
		return results;
	}

	@WriteTransaction
	@Override
	public void deleteChange(Long objectId, ObjectType type) {
		if(objectId == null) throw new IllegalArgumentException("ObjectId cannot be null");
		if(type == null) throw new IllegalArgumentException("ObjectType cannot be null");
		// To avoid gap locking we do not delete by the ID.  Rather we get the primary key, then delete by the primary key.
		List<Long> list = jdbcTemplate.query(SELECT_CHANGE_NUMBER_FOR_OBJECT_ID_AND_TYPE, new RowMapper<Long>(){
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(COL_CHANGES_CHANGE_NUM);
			}},objectId, type.name());
		// Log the error case where multiple are found.
		if(list.size() > 1){
			log.error("Found multiple rows with ObjectId: "+objectId+" and ObjectType type "+type);
		}
		// Even if there are multiple, delete them all
		for(Long primaryKey: list){
			// Unless there is an error there will only be one here.
			jdbcTemplate.update(SQL_DELETE_BY_CHANGE_NUM, primaryKey);
		}
	}

	@Override
	public long getMinimumChangeNumber() {
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_MIN_CHANGE_NUMBER, Long.class);
		} catch (NullPointerException e) {
			return 0L;
		}
	}
	
	@Override
	public long getCurrentChangeNumber() {
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_MAX_CHANGE_NUMBER, Long.class);
		} catch (NullPointerException e) {
			return 0L;
		}
	}
	
	@Override
	public long getCount() {
		return jdbcTemplate.queryForObject(SQL_SELECT_COUNT_CHANGE_NUMBER, Long.class);
	}

	@WriteTransaction
	@Override
	public void deleteAllChanges() {
		jdbcTemplate.update("DELETE FROM  "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" > -1");
	}

	@Override
	public List<ChangeMessage> listChanges(long greaterOrEqualChangeNumber, ObjectType type, long limit) {
		if(limit < 0) throw new IllegalArgumentException("Limit cannot be less than zero");
		List<DBOChange> dboList = null;
		if(type == null){
			// There is no type filter.
			dboList = jdbcTemplate.query(SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER, new DBOChange().getTableMapping(), greaterOrEqualChangeNumber, limit);
		}else{
			// Filter by object type.
			dboList = jdbcTemplate.query(SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER_FILTER_BY_OBJECT_TYPE, rowMapper, greaterOrEqualChangeNumber, type.name(), limit);
		}
		return ChangeMessageUtils.createDTOList(dboList);
	}


	@WriteTransaction
	@Override
	public void registerMessageSent(ChangeMessage message) {
		registerMessageSent(message.getObjectType(), Arrays.asList(message));
	}
	
	@WriteTransaction
	@Override
	public void registerMessageSent(ObjectType type, List<ChangeMessage> batch) {
		if(type == null){
			throw new IllegalArgumentException("Type cannot be null");
		}
		if(batch == null){
			throw new IllegalArgumentException("Batch cannot be null");
		}
		Timestamp now = new Timestamp(System.currentTimeMillis());
		// Create the dto batch
		List<DBOSentMessage> dboBatch = new LinkedList<DBOSentMessage>();
		for(ChangeMessage message: batch){
			if(!type.equals(message.getObjectType())){
				throw new IllegalArgumentException("All ChangeMessages in the batch must have an ObjectType of: "+type+" but found: "+message.getObjectType());
			}
			if(message.getChangeNumber() == null){
				throw new IllegalArgumentException("Change.changeNumber cannot be null");
			}
			DBOSentMessage sent = ChangeMessageUtils.createSentDBO(message, now);
			dboBatch.add(sent);
		}
		// batch insert all.
		basicDao.createOrUpdateBatch(dboBatch);
	}
	
	


	/**
	 * Select the current change number of a sent message for update.
	 * @param sent
	 * @return
	 */
	public Long selectSentChangeNumberForUpdate(Long objectId, ObjectType type) {
		try {
			return this.jdbcTemplate.queryForObject(SQL_SENT_CHANGE_NUMBER_FOR_UPDATE, new SingleColumnRowMapper<Long>(),  objectId, type.name());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	/**
	 * Select the current change number from changes for update.
	 * @param sent
	 * @return
	 */
	public Long selectChangeNumberForUpdate(Long objectId, ObjectType type) {
		try {
			return this.jdbcTemplate.queryForObject(SQL_CHANGE_NUMBER_FOR_UPDATE, new SingleColumnRowMapper<Long>(), objectId, type.name());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	
	@Override
	public List<ChangeMessage> listUnsentMessages(long limit) {
		List<DBOChange> dboList = jdbcTemplate.query(SQL_SELECT_CHANGES_NOT_SENT, rowMapper, limit);
		return ChangeMessageUtils.createDTOList(dboList);
	}


	@Override
	public List<ChangeMessage> listUnsentMessages(long lowerBound,
			long upperBound, Timestamp olderThan) {
		List<DBOChange> dboList = jdbcTemplate.query(SQL_SELECT_CHANGES_NOT_SENT_IN_RANGE, rowMapper, lowerBound, upperBound, olderThan);
		return ChangeMessageUtils.createDTOList(dboList);
	}

	@Override
	public void registerMessageProcessed(long changeNumber, String queueName) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		jdbcTemplate.update(SQL_INSERT_PROCESSED_ON_DUPLICATE_UPDATE, changeNumber, queueName, now, now);
	}

	@Override
	public List<ChangeMessage> listNotProcessedMessages(String queueName, long limit) {
		List<DBOChange> l = jdbcTemplate.query(SQL_SELECT_CHANGES_NOT_PROCESSED, new DBOChange().getTableMapping(), queueName, limit);
		return ChangeMessageUtils.createDTOList(l);
	}

	@Override
	public Long getMaxSentChangeNumber(Long lessThanOrEqual) {
		Long max = jdbcTemplate.queryForObject(SQL_SELECT_MAX_SENT_CHANGE_NUMBER_LESS_THAN_OR_EQUAL, new SingleColumnRowMapper<Long>(), lessThanOrEqual);
		if(max == null){
			max = new Long(-1);
		}
		return max;
	}


	@Override
	public boolean checkUnsentMessageByCheckSumForRange(long lowerBound,
			long upperBound) {
		BigDecimal sentCheckSum = jdbcTemplate.queryForObject(SQL_SENT_CHECK_SUM_FOR_RANGE,new SingleColumnRowMapper<BigDecimal>(), lowerBound, upperBound);
		BigDecimal changeCheckSum = jdbcTemplate.queryForObject(SQL_CHANGE_CHECK_SUM_FOR_RANGE,new SingleColumnRowMapper<BigDecimal>(), lowerBound, upperBound);
		if(sentCheckSum == null){
			if(changeCheckSum == null){
				return true;
			}else{
				return false;
			}
		}
		return sentCheckSum.equals(changeCheckSum);
	}

	@Override
	public boolean doesChangeNumberExist(Long changeNumber) {
		long count = jdbcTemplate.queryForObject(SQL_COUNT_CHANGE_NUMBER, Long.class, changeNumber);
		return count == 1;
	}

	@Override
	public List<ChangeMessage> getChangesForObjectIds(ObjectType objectType,
			Set<Long> objectIds) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(objectIds, "objectIds");
		List<DBOChange> dboList;
		if(objectIds.isEmpty()){
			dboList = new LinkedList<>();
		}else{
			Map<String, Object> params = new HashMap<String, Object>(2);
			params.put(BIND_TYPE, objectType.name());
			params.put(BIND_OBJECT_IDS, objectIds);
			dboList =  namedParameterJdbcTemplate.query(SQL_SELECT_CHANGES_BY_OBJECT_IDS_AND_TYPE, params, rowMapper);
		}
		return ChangeMessageUtils.createDTOList(dboList);
	}

	@Override
	public DBOSentMessage getSentMessage(String objectId, Long objectVersion, ObjectType objectType) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("objectId", objectId);
		params.addValue("objectType", objectType.name());
		params.addValue("objectVersion", objectVersion);
		return basicDao.getObjectByPrimaryKey(DBOSentMessage.class, params);
	}

}
