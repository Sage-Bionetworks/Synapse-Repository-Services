package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


public class DBOMessageDAOImpl implements MessageDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	/**
	 * Used for testing
	 */
	public void setBasicDAO(DBOBasicDao basicDAO) {
		this.basicDAO = basicDAO;
	}
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String THREAD_ID_PARAM_NAME = "threadId";
	private static final String RECIPIENT_ID_PARAM_NAME = "recipientId";
	private static final String SENDER_ID_PARAM_NAME = "senderId";
	
	private static final String FROM_MESSAGES_IN_THREAD = 
			" FROM "+SqlConstants.TABLE_MESSAGE+ 
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_ID+"=:" + THREAD_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGES_IN_THREAD = 
			"SELECT *" + FROM_MESSAGES_IN_THREAD;
	
	private static final String COUNT_MESSAGES_IN_THREAD = 
			"SELECT COUNT(*)" + FROM_MESSAGES_IN_THREAD;
	
	private static final String SELECT_MESSAGES_RECEIVED = 
			"SELECT * FROM "+SqlConstants.TABLE_MESSAGE+","+SqlConstants.TABLE_MESSAGE_STATUS+
			" WHERE "+SqlConstants.COL_MESSAGE_ID+"="+SqlConstants.COL_MESSAGE_STATUS_ID+
			" AND "+SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID+"=:"+RECIPIENT_ID_PARAM_NAME;
	
	private static final String COUNT_MESSAGES_RECEIVED = 
			"SELECT COUNT(*) FROM "+SqlConstants.TABLE_MESSAGE_STATUS+
			" WHERE "+SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID+"=:"+RECIPIENT_ID_PARAM_NAME;
	
	private static final String FROM_MESSAGES_SENT = 
			" FROM "+SqlConstants.TABLE_MESSAGE+ 
			" WHERE "+SqlConstants.COL_MESSAGE_CREATED_BY+"=:" + SENDER_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGES_SENT = 
			"SELECT *" + FROM_MESSAGES_SENT;
	
	private static final String COUNT_MESSAGES_SENT = 
			"SELECT COUNT(*)" + FROM_MESSAGES_SENT;
	
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
	private static String constructSqlSuffix(MESSAGE_SORT_BY sortBy,
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
	public Message saveMessage(Message dto) {
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
	public List<Message> getThread(String threadId, MESSAGE_SORT_BY sortBy,
			boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_IN_THREAD + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		List<DBOMessage> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		return MessageUtils.convertDBOs(messages);
	}

	@Override
	public long getThreadSize(String threadId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_IN_THREAD, params);
	}

	@Override
	public List<MessageBundle> getReceivedMessages(String userId,
			MESSAGE_SORT_BY sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_RECEIVED + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(RECIPIENT_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.query(sql, messageBundleRowMapper, params);
	}

	@Override
	public long getNumReceivedMessages(String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(RECIPIENT_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_RECEIVED, params);
	}

	@Override
	public List<Message> getSentMessages(String userId, MESSAGE_SORT_BY sortBy,
			boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_SENT + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(SENDER_ID_PARAM_NAME, userId);
		List<DBOMessage> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		return MessageUtils.convertDBOs(messages);
	}

	@Override
	public long getNumSentMessages(String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(SENDER_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_SENT, params);
	}

	@Override
	public void registerMessageRecipient(String messageId, String userId) {
		DBOMessageStatus status = new DBOMessageStatus();
		status.setMessageId(Long.parseLong(messageId));
		status.setRecipientId(Long.parseLong(userId));
		status.setStatus(MessageStatusType.UNREAD);
		basicDAO.createNew(status);
	}

	@Override
	public void updateMessageStatus(String messageId, String userId,
			MessageStatusType status) {
		DBOMessageStatus toUpdate = new DBOMessageStatus();
		toUpdate.setMessageId(Long.parseLong(messageId));
		toUpdate.setRecipientId(Long.parseLong(userId));
		toUpdate.setStatus(status);
		basicDAO.update(toUpdate);
	}
}
