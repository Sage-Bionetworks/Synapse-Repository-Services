package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_QUEUE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROCESSED_MESSAGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SENT_MESSAGES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * The implementation of the change DBOChangeDAO
 * @author John
 *
 */
public class DBOChangeDAOImpl implements DBOChangeDAO {
	
	static private Logger log = LogManager.getLogger(DBOChangeDAOImpl.class);
	
	private static final String SQL_INSERT_SENT_ON_DUPLICATE_UPDATE = 
			"INSERT INTO "+TABLE_SENT_MESSAGES+" ( "+COL_SENT_MESSAGES_CHANGE_NUM+", "+COL_SENT_MESSAGES_TIME_STAMP+")"+
			" VALUES ( ?, ?) ON DUPLICATE KEY UPDATE "+COL_SENT_MESSAGES_TIME_STAMP+" = ?";
	
	private static final String SQL_CHANGES_NOT_SENT_PREFIX = 
			"SELECT C.* FROM "+TABLE_CHANGES+
			" C LEFT OUTER JOIN "+TABLE_SENT_MESSAGES+" S ON (C."+COL_CHANGES_CHANGE_NUM+" = S."+COL_SENT_MESSAGES_CHANGE_NUM+")"+
			"WHERE S."+COL_SENT_MESSAGES_CHANGE_NUM+" IS NULL";
	
	private static final String SQL_SELECT_CHANGES_NOT_SENT = 
			SQL_CHANGES_NOT_SENT_PREFIX + " LIMIT ?";
	
	private static final String SQL_SELECT_CHANGES_NOT_SENT_IN_RANGE = 
			SQL_CHANGES_NOT_SENT_PREFIX+
			" AND C."+COL_SENT_MESSAGES_CHANGE_NUM+" >= ?"+
			" AND C."+COL_SENT_MESSAGES_CHANGE_NUM+" <= ?";
	
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

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private TableMapping<DBOChange> rowMapper = new DBOChange().getTableMapping();

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChangeMessage replaceChange(ChangeMessage change) {
		if(change == null) throw new IllegalArgumentException("DBOChange cannot be null");
		if(change.getObjectId() == null) throw new IllegalArgumentException("change.getObjectId() cannot be null");
		if(change.getChangeType() == null) throw new IllegalArgumentException("change.getChangeTypeEnum() cannot be null");
		if(change.getObjectType() == null) throw new IllegalArgumentException("change.getObjectTypeEnum() cannot be null");
		if(ChangeType.CREATE == change.getChangeType() && change.getObjectEtag() == null) throw new IllegalArgumentException("Etag cannot be null for ChangeType: "+change.getChangeType());
		if(ChangeType.UPDATE == change.getChangeType() && change.getObjectEtag() == null) throw new IllegalArgumentException("Etag cannot be null for ChangeType: "+change.getChangeType());
		DBOChange dbo = ChangeMessageUtils.createDBO(change);
		// First delete the change.
		deleteChange(dbo.getObjectId(), dbo.getObjectTypeEnum());
		// Clear the time stamp so that we get a new one automatically.
		// Note: Mysql TIMESTAMP only keeps seconds (not MS) so for consistency we only write second accuracy.
		// We are using (System.currentTimeMillis()/1000)*1000; to convert all MS to zeros.
		long nowMs = (System.currentTimeMillis()/1000)*1000;
		dbo.setChangeNumber(idGenerator.generateNewId(TYPE.CHANGE_ID));
		dbo.setTimeStamp(new Timestamp(nowMs));
		// Now insert the row with the current value
		dbo = basicDao.createNew(dbo);
		return ChangeMessageUtils.createDTO(dbo);
	}

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<ChangeMessage> replaceChange(List<ChangeMessage> batchDTO) {
		if(batchDTO == null) throw new IllegalArgumentException("Batch cannot be null");
		// To prevent deadlock we sort by object id to guarantee a consistent update order.
		batchDTO = ChangeMessageUtils.sortByObjectId(batchDTO);
		// Send each replace them in order.
		List<ChangeMessage> resutls = new ArrayList<ChangeMessage>();
		for(ChangeMessage change: batchDTO){
			resutls.add(replaceChange(change));
		}
		return resutls;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteChange(Long objectId, ObjectType type) {
		if(objectId == null) throw new IllegalArgumentException("ObjectId cannot be null");
		if(type == null) throw new IllegalArgumentException("ObjectType cannot be null");
		// To avoid gap locking we do not delete by the ID.  Rather we get the primary key, then delete by the primary key.
		List<Long> list = simpleJdbcTemplate.query(SELECT_CHANGE_NUMBER_FOR_OBJECT_ID_AND_TYPE, new RowMapper<Long>(){
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
			simpleJdbcTemplate.update(SQL_DELETE_BY_CHANGE_NUM, primaryKey);
		}
	}

	@Override
	public long getMinimumChangeNumber() {
		return simpleJdbcTemplate.queryForLong(SQL_SELECT_MIN_CHANGE_NUMBER);
	}
	
	@Override
	public long getCurrentChangeNumber() {
		return simpleJdbcTemplate.queryForLong(SQL_SELECT_MAX_CHANGE_NUMBER);
	}
	
	@Override
	public long getCount() {
		return simpleJdbcTemplate.queryForLong(SQL_SELECT_COUNT_CHANGE_NUMBER);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAllChanges() {
		simpleJdbcTemplate.update("DELETE FROM  "+TABLE_CHANGES+" WHERE "+COL_CHANGES_CHANGE_NUM+" > -1");
	}

	@Override
	public List<ChangeMessage> listChanges(long greaterOrEqualChangeNumber, ObjectType type, long limit) {
		if(limit < 0) throw new IllegalArgumentException("Limit cannot be less than zero");
		List<DBOChange> dboList = null;
		if(type == null){
			// There is no type filter.
			dboList = simpleJdbcTemplate.query(SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER, new DBOChange().getTableMapping(), greaterOrEqualChangeNumber, limit);
		}else{
			// Filter by object type.
			dboList = simpleJdbcTemplate.query(SQL_SELECT_ALL_GREATER_THAN_OR_EQUAL_TO_CHANGE_NUMBER_FILTER_BY_OBJECT_TYPE, rowMapper, greaterOrEqualChangeNumber, type.name(), limit);
		}
		return ChangeMessageUtils.createDTOList(dboList);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public void registerMessageSent(long changeNumber) {
		simpleJdbcTemplate.update(SQL_INSERT_SENT_ON_DUPLICATE_UPDATE, changeNumber, null, null);
	}

	
	@Override
	public List<ChangeMessage> listUnsentMessages(long limit) {
		List<DBOChange> dboList = simpleJdbcTemplate.query(SQL_SELECT_CHANGES_NOT_SENT, rowMapper, limit);
		return ChangeMessageUtils.createDTOList(dboList);
	}


	@Override
	public List<ChangeMessage> listUnsentMessages(long lowerBound,
			long upperBound) {
		List<DBOChange> dboList = simpleJdbcTemplate.query(SQL_SELECT_CHANGES_NOT_SENT_IN_RANGE, rowMapper, lowerBound, upperBound);
		return ChangeMessageUtils.createDTOList(dboList);
	}

	@Override
	public void registerMessageProcessed(long changeNumber, String queueName) {
		simpleJdbcTemplate.update(SQL_INSERT_PROCESSED_ON_DUPLICATE_UPDATE, changeNumber, queueName, null, null);
		
	}

	@Override
	public List<ChangeMessage> listNotProcessedMessages(String queueName, long limit) {
		List<DBOChange> l = simpleJdbcTemplate.query(SQL_SELECT_CHANGES_NOT_PROCESSED, new DBOChange().getTableMapping(), queueName, limit);
		return ChangeMessageUtils.createDTOList(l);
	}

}
