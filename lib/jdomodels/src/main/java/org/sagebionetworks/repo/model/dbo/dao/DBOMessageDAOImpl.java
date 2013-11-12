package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessage;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class DBOMessageDAOImpl implements MessageDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String THREAD_ID_PARAM_NAME = "threadId";
	private static final String USER_ID_PARAM_NAME = "userId";
	private static final String INBOX_FILTER_PARAM_NAME = "filterTypes";
	
	private static final String FROM_MESSAGES_IN_THREAD_NO_FILTER_CORE = 
			" FROM "+SqlConstants.TABLE_MESSAGE+
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_ID+"=:"+THREAD_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGES_IN_THREAD_NO_FILTER = 
			"SELECT *"+FROM_MESSAGES_IN_THREAD_NO_FILTER_CORE;
	
	private static final String COUNT_MESSAGES_IN_THREAD_NO_FILTER = 
			"SELECT COUNT(*)"+FROM_MESSAGES_IN_THREAD_NO_FILTER_CORE;
	
	private static final String FROM_MESSAGES_IN_THREAD_CORE = 
			" FROM "+SqlConstants.TABLE_MESSAGE+
				" LEFT OUTER JOIN "+SqlConstants.TABLE_MESSAGE_STATUS+
				" ON ("+SqlConstants.COL_MESSAGE_ID+"="+SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID+")"+
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_ID+"=:"+THREAD_ID_PARAM_NAME+
			" AND ("+SqlConstants.COL_MESSAGE_CREATED_BY+"=:"+USER_ID_PARAM_NAME+
				" OR "+SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID+"=:"+USER_ID_PARAM_NAME+")";
	
	private static final String SELECT_MESSAGES_IN_THREAD = 
			"SELECT DISTINCT("+SqlConstants.COL_MESSAGE_ID+"),"+
				 	SqlConstants.COL_MESSAGE_THREAD_ID+","+
					SqlConstants.COL_MESSAGE_CREATED_BY+","+
				 	SqlConstants.COL_MESSAGE_RECIPIENT_TYPE+","+
				 	SqlConstants.COL_MESSAGE_RECIPIENTS+","+
				 	SqlConstants.COL_MESSAGE_FILE_HANDLE_ID+","+
				 	SqlConstants.COL_MESSAGE_CREATED_ON+","+
				 	SqlConstants.COL_MESSAGE_SUBJECT+
			FROM_MESSAGES_IN_THREAD_CORE;
	
	private static final String COUNT_MESSAGES_IN_THREAD = 
			"SELECT COUNT(DISTINCT("+SqlConstants.COL_MESSAGE_ID+"))"+
			FROM_MESSAGES_IN_THREAD_CORE;
	
	private static final String FILTER_MESSAGES_RECEIVED = 
			SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID+"=:"+USER_ID_PARAM_NAME+
			" AND "+SqlConstants.COL_MESSAGE_STATUS+" IN (:"+INBOX_FILTER_PARAM_NAME+")";
	
	private static final String SELECT_MESSAGES_RECEIVED = 
			"SELECT * FROM "+SqlConstants.TABLE_MESSAGE+","+SqlConstants.TABLE_MESSAGE_STATUS+
			" WHERE "+SqlConstants.COL_MESSAGE_ID+"="+SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID+
			" AND "+FILTER_MESSAGES_RECEIVED;
	
	private static final String COUNT_MESSAGES_RECEIVED = 
			"SELECT COUNT(*) FROM "+SqlConstants.TABLE_MESSAGE_STATUS+
			" WHERE "+FILTER_MESSAGES_RECEIVED;
	
	private static final String FROM_MESSAGES_SENT_CORE = 
			" FROM "+SqlConstants.TABLE_MESSAGE+ 
			" WHERE "+SqlConstants.COL_MESSAGE_CREATED_BY+"=:" + USER_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGES_SENT = 
			"SELECT *" + FROM_MESSAGES_SENT_CORE;
	
	private static final String COUNT_MESSAGES_SENT = 
			"SELECT COUNT(*)" + FROM_MESSAGES_SENT_CORE;
	
	private static final RowMapper<DBOMessage> messageRowMapper = new DBOMessage().getTableMapping();
	
	private static final RowMapper<DBOMessageStatus> messageStatusRowMapper = new DBOMessageStatus().getTableMapping();
	
	private static final RowMapper<MessageBundle> messageBundleRowMapper = new RowMapper<MessageBundle>() {
		@Override
		public MessageBundle mapRow(ResultSet rs, int rowNum) throws SQLException {
			MessageBundle bundle = new MessageBundle();
			DBOMessage messageContent = messageRowMapper.mapRow(rs, rowNum);
			DBOMessageStatus messageStatus = messageStatusRowMapper.mapRow(rs, rowNum);
			bundle.setMessage(MessageUtils.convertDBO(messageContent));
			bundle.setStatus(MessageUtils.convertDBO(messageStatus));
			return bundle;
		}
	};
	
	/**
	 * Builds up ordering and pagination keywords to append to various message select statements
	 */
	private static String constructSqlSuffix(MessageSortBy sortBy,
			boolean descending, long limit, long offset) {
		StringBuilder suffix = new StringBuilder();
		suffix.append(" ORDER BY ");
		switch (sortBy) {
		case SEND_DATE:
			suffix.append(SqlConstants.COL_MESSAGE_CREATED_ON);
			break;
		case SUBJECT:
			suffix.append(SqlConstants.COL_MESSAGE_SUBJECT);
			break;
		default:
			throw new IllegalArgumentException("Unknown type: " + sortBy);
		}
		suffix.append(" ");
		suffix.append(descending ? "DESC" : "ASC");
		suffix.append(" LIMIT " + limit);
		suffix.append(" OFFSET " + offset);
		return suffix.toString();
	}

	@Override
	public Message getMessage(String messageId) throws NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("messageId", messageId);
		DBOMessage message = basicDAO.getObjectByPrimaryKey(DBOMessage.class, params);
		return MessageUtils.convertDBO(message);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Message createMessage(Message dto) {
		// Fill in new IDs for the message
		dto.setMessageId(idGenerator.generateNewId(TYPE.MESSAGE_ID).toString());
		if (dto.getThreadId() == null) {
			dto.setThreadId(idGenerator.generateNewId(TYPE.MESSAGE_THREAD_ID).toString());
		}
		dto.setCreatedOn(new Date());
		
		MessageUtils.validateDTO(dto);
		
		DBOMessage saved = basicDAO.createNew(MessageUtils.convertDTO(dto));
		return MessageUtils.convertDBO(saved);
	}
	
	@Override
	public List<Message> getThread(String threadId, MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_IN_THREAD_NO_FILTER + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		List<DBOMessage> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		return MessageUtils.convertDBOs(messages);
	}

	@Override
	public long getThreadSize(String threadId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_IN_THREAD_NO_FILTER, params);
	}

	@Override
	public List<Message> getThread(String threadId, String userId, MessageSortBy sortBy,
			boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_IN_THREAD + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		params.addValue(USER_ID_PARAM_NAME, userId);
		List<DBOMessage> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		return MessageUtils.convertDBOs(messages);
	}

	@Override
	public long getThreadSize(String threadId, String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		params.addValue(USER_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_IN_THREAD, params);
	}

	@Override
	public List<MessageBundle> getReceivedMessages(String userId, List<MessageStatusType> included, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_RECEIVED + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		params.addValue(INBOX_FILTER_PARAM_NAME, convertFilterToString(included));
		return simpleJdbcTemplate.query(sql, messageBundleRowMapper, params);
	}

	@Override
	public long getNumReceivedMessages(String userId, List<MessageStatusType> included) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		params.addValue(INBOX_FILTER_PARAM_NAME, convertFilterToString(included));
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_RECEIVED, params);
	}
	
	/**
	 * Before using enums in queries, use this to convert them into strings
	 */
	private List<String> convertFilterToString(List<MessageStatusType> filter) {
		List<String> converted = new ArrayList<String>();
		for (MessageStatusType layer : filter) {
			converted.add(layer.name());
		}
		return converted;
	}

	@Override
	public List<Message> getSentMessages(String userId, MessageSortBy sortBy,
			boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_SENT + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		List<DBOMessage> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		return MessageUtils.convertDBOs(messages);
	}

	@Override
	public long getNumSentMessages(String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_SENT, params);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void registerMessageRecipient(String messageId, String userId) {
		DBOMessageStatus status = new DBOMessageStatus();
		status.setMessageId(Long.parseLong(messageId));
		status.setRecipientId(Long.parseLong(userId));
		status.setStatus(MessageStatusType.UNREAD);
		basicDAO.createNew(status);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateMessageStatus(String messageId, String userId,
			MessageStatusType status) {
		DBOMessageStatus toUpdate = new DBOMessageStatus();
		toUpdate.setMessageId(Long.parseLong(messageId));
		toUpdate.setRecipientId(Long.parseLong(userId));
		toUpdate.setStatus(status);
		basicDAO.update(toUpdate);
	}
}
