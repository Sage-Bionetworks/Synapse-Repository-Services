package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOComment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageRecipient;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sun.tools.javac.util.Pair;


public class DBOMessageDAOImpl implements MessageDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String MESSAGE_ID_PARAM_NAME = "messageId";
	private static final String USER_ID_PARAM_NAME = "userId";
	
	private static final String SELECT_MESSAGE_BY_ID = 
			"SELECT * FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + "," + SqlConstants.TABLE_MESSAGE_TO_USER +
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=" + SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID + 
			" AND " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=:" + MESSAGE_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGE_RECIPIENTS_BY_ID = 
			"SELECT " + SqlConstants.COL_MESSAGE_RECIPIENT_ID + " FROM " + SqlConstants.TABLE_MESSAGE_RECIPIENT + 
			" WHERE " + SqlConstants.COL_MESSAGE_RECIPIENT_MESSAGE_ID + "=:" + MESSAGE_ID_PARAM_NAME;
			
	private static final String INSERT_MESSAGE_RECIPIENTS = 
			"INSERT INTO " + SqlConstants.TABLE_MESSAGE_RECIPIENT + 
			" VALUES (:" + MESSAGE_ID_PARAM_NAME + ",:" + USER_ID_PARAM_NAME + ")";
	
	private static final String FROM_MESSAGES_IN_THREAD_NO_FILTER_CORE = 
			" FROM "+SqlConstants.TABLE_MESSAGE+","+SqlConstants.TABLE_MESSAGE_THREAD+
			" WHERE "+SqlConstants.COL_MESSAGE_ID+"="+SqlConstants.COL_MESSAGE_THREAD_CHILD_ID+
			" AND "+SqlConstants.COL_MESSAGE_THREAD_PARENT_ID+"=:"+THREAD_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGES_IN_THREAD_NO_FILTER = 
			"SELECT *"+FROM_MESSAGES_IN_THREAD_NO_FILTER_CORE;
	
	private static final String COUNT_MESSAGES_IN_THREAD_NO_FILTER = 
			"SELECT COUNT(*)"+FROM_MESSAGES_IN_THREAD_NO_FILTER_CORE;
	
	private static final String FROM_MESSAGES_IN_THREAD_CORE = 
			" FROM "+SqlConstants.TABLE_MESSAGE+
				" LEFT OUTER JOIN "+SqlConstants.TABLE_MESSAGE_STATUS+
					" ON ("+SqlConstants.COL_MESSAGE_ID+"="+SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID+")"+
				" INNER JOIN "+SqlConstants.TABLE_MESSAGE_THREAD+
					" ON ("+SqlConstants.COL_MESSAGE_ID+"="+SqlConstants.COL_MESSAGE_THREAD_CHILD_ID+")"+
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_PARENT_ID+"=:"+THREAD_ID_PARAM_NAME+
			" AND ("+SqlConstants.COL_MESSAGE_CREATED_BY+"=:"+USER_ID_PARAM_NAME+
				" OR "+SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID+"=:"+USER_ID_PARAM_NAME+")";
	
	private static final String SELECT_MESSAGES_IN_THREAD = 
			"SELECT DISTINCT("+SqlConstants.COL_MESSAGE_ID+"),"+
					SqlConstants.COL_MESSAGE_CREATED_BY+","+
				 	SqlConstants.COL_MESSAGE_RECIPIENT_TYPE+","+
				 	SqlConstants.COL_MESSAGE_RECIPIENTS+","+
				 	SqlConstants.COL_MESSAGE_FILE_HANDLE_ID+","+
				 	SqlConstants.COL_MESSAGE_CREATED_ON+","+
				 	SqlConstants.COL_MESSAGE_SUBJECT+","+
				 	SqlConstants.COL_MESSAGE_REPLY_TO+
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
	
	private static final String SELECT_THREAD_ID_OF_MESSAGE = 
			"SELECT "+SqlConstants.COL_MESSAGE_THREAD_PARENT_ID+" FROM "+SqlConstants.TABLE_MESSAGE_THREAD+
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_CHILD_ID+"=:"+MESSAGE_ID_PARAM_NAME;
	
	private static final String SELECT_THREADS_OF_OBJECT = 
			"SELECT "+SqlConstants.COL_MESSAGE_THREAD_OBJECT_THREAD_ID+" FROM "+SqlConstants.TABLE_MESSAGE_THREAD_OBJECT+
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_OBJECT_TYPE+"=:"+OBJECT_TYPE_PARAM_NAME+
			" AND "+SqlConstants.COL_MESSAGE_THREAD_OBJECT_ID+"=:"+OBJECT_ID_PARAM_NAME;
	
	private static final String SELECT_OBJECT_OF_THREAD = 
			"SELECT * FROM "+SqlConstants.TABLE_MESSAGE_THREAD_OBJECT+
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_OBJECT_THREAD_ID+"=:"+THREAD_ID_PARAM_NAME;
	
	private static final RowMapper<DBOMessageContent> messageContentRowMapper = new DBOMessageContent().getTableMapping();
	private static final RowMapper<DBOMessageToUser> messageToUserRowMapper = new DBOMessageToUser().getTableMapping();
	private static final RowMapper<DBOMessageStatus> messageStatusRowMapper = new DBOMessageStatus().getTableMapping();
	
	private static final RowMapper<MessageBundle> messageBundleRowMapper = new RowMapper<MessageBundle>() {
		@Override
		public MessageBundle mapRow(ResultSet rs, int rowNum) throws SQLException {
			MessageBundle bundle = new MessageBundle();
			DBOMessageContent messageContent = messageRowMapper.mapRow(rs, rowNum);
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
	
	private static final RowMapper<MessageToUser> messageRowMapper = new RowMapper<MessageToUser>() {
		@Override
		public MessageToUser mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOMessageContent messageContent = messageContentRowMapper.mapRow(rs, rowNum);
			DBOMessageToUser messageToUser = messageToUserRowMapper.mapRow(rs, rowNum);

			MessageToUser bundle = new MessageToUser();
			MessageUtils.copyDBOToDTO(messageContent, messageToUser, bundle);
			return bundle;
		}
	};

	@Override
	public MessageToUser getMessage(String messageId) throws NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		return simpleJdbcTemplate.queryForObject(SELECT_MESSAGE_BY_ID, messageRowMapper, params);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MessageToUser createMessage(MessageToUser dto) {
		DBOMessageContent content = new DBOMessageContent();
		DBOMessageToUser info = new DBOMessageToUser();
		List<DBOMessageRecipient> recipients = new ArrayList<DBOMessageRecipient>();
		MessageUtils.copyDTOtoDBO(dto, content, info, recipients);
		
		// Generate an ID for all the new rows
		Long messageId = idGenerator.generateNewId(TYPE.MESSAGE_ID);
		
		// Insert the message content
		content.setMessageId(messageId);
		content.setCreatedOn(new Date().getTime());
		content.setEtag(UUID.randomUUID().toString());
		MessageUtils.validateDBO(content);
		basicDAO.createNew(content);
		
		// Insert the message info
		info.setMessageId(messageId);
		MessageUtils.validateDBO(info);
		basicDAO.createNew(info);
		
		// Insert the message recipients
		MapSqlParameterSource[] params = new MapSqlParameterSource[recipients.size()];
		for (int i = 0; i < recipients.size(); i++) {
			recipients.get(i).setMessageId(messageId);
			MessageUtils.validateDBO(recipients.get(i));
			
			params[i] = new MapSqlParameterSource();
			params[i].addValue(MESSAGE_ID_PARAM_NAME, recipients.get(i).getMessageId());
			params[i].addValue(USER_ID_PARAM_NAME, recipients.get(i).getRecipientId());
		}
		simpleJdbcTemplate.batchUpdate(INSERT_MESSAGE_RECIPIENTS, params);
		
		MessageToUser bundle = new MessageToUser();
		MessageUtils.copyDBOToDTO(content, info, recipients, bundle);
		return bundle;
	}
	
	@Override
	public List<Message> getThread(String threadId, MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_IN_THREAD_NO_FILTER + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		List<DBOMessageContent> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
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
		List<DBOMessageContent> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
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
		List<DBOMessageContent> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
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
	
	@Override
	public String getMessageThread(String messageId) throws NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		try {
			return simpleJdbcTemplate.queryForObject(SELECT_THREAD_ID_OF_MESSAGE, String.class, params);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No thread associated with message (" + messageId + ")");
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String registerMessageToThread(String messageId, String threadId)
			throws IllegalArgumentException {
		DBOMessageInReplyToRoot dbo = new DBOMessageInReplyToRoot();
		dbo.setChildMessageId(Long.parseLong(messageId));
		if (threadId == null) {
			dbo.setParentMessageId(dbo.getChildMessageId());
		} else {
			dbo.setParentMessageId(Long.parseLong(threadId));
		}
		dbo = basicDAO.createNew(dbo);
		return dbo.getParentMessageId().toString();
	}

	@Override
	public List<String> getThreadsOfObject(ObjectType objectType,
			String objectId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OBJECT_TYPE_PARAM_NAME, objectType.name());
		params.addValue(OBJECT_ID_PARAM_NAME, objectId);
		return simpleJdbcTemplate.query(SELECT_THREADS_OF_OBJECT, threadIdRowMapper, params);
	}

	@Override
	public Pair<ObjectType, String> getObjectOfThread(String threadId)
			throws NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		DBOComment dbo;
		try {
			 dbo = simpleJdbcTemplate.queryForObject(SELECT_OBJECT_OF_THREAD, messageThreadObjectRowMapper, params);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No object associated with thread (" + threadId + ")");
		}
		return new Pair<ObjectType, String>(ObjectType.valueOf(dbo.getObjectType()), dbo.getObjectId().toString());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void registerThreadToObject(String threadId, ObjectType objectType,
			String objectId) throws IllegalArgumentException {
		DBOComment dbo = new DBOComment();
		dbo.setThreadId(Long.parseLong(threadId));
		dbo.setObjectType(objectType.name());
		dbo.setObjectId(Long.parseLong(objectId));
		basicDAO.createNew(dbo);
	}
}
