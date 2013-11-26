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
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageRecipient;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
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
	private TransactionalMessenger transactionalMessenger;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String MESSAGE_ID_PARAM_NAME = "messageId";
	private static final String USER_ID_PARAM_NAME = "userId";
	private static final String ETAG_PARAM_NAME = "etag";
	private static final String ROOT_MESSAGE_ID_PARAM_NAME = "rootMessageId";
	private static final String INBOX_FILTER_PARAM_NAME = "inboxFilter";
	
	private static final String SELECT_MESSAGE_BY_ID = 
			"SELECT * FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + "," + SqlConstants.TABLE_MESSAGE_TO_USER +
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=" + SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID + 
			" AND " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=:" + MESSAGE_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGE_RECIPIENTS_BY_ID = 
			"SELECT * FROM " + SqlConstants.TABLE_MESSAGE_RECIPIENT + 
			" WHERE " + SqlConstants.COL_MESSAGE_RECIPIENT_MESSAGE_ID + " IN(:" + MESSAGE_ID_PARAM_NAME + ")";
	
	private static final String SELECT_ROOT_MESSAGE_ID_BY_ID = 
			"SELECT " + SqlConstants.COL_MESSAGE_TO_USER_ROOT_ID + " FROM " + SqlConstants.TABLE_MESSAGE_TO_USER + 
			" WHERE " + SqlConstants.COL_MESSAGE_RECIPIENT_MESSAGE_ID + "=:" + MESSAGE_ID_PARAM_NAME;
	
	private static final String UPDATE_ETAG_OF_MESSAGE = 
			"UPDATE " + SqlConstants.TABLE_MESSAGE_CONTENT+
			" SET " + SqlConstants.COL_MESSAGE_CONTENT_ETAG + "=:" + ETAG_PARAM_NAME + 
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=:" + MESSAGE_ID_PARAM_NAME;
	
	private static final String FROM_MESSAGES_IN_CONVERSATION_CORE = 
			" FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + 
				" LEFT OUTER JOIN "+SqlConstants.TABLE_MESSAGE_STATUS + " status" + 
					" ON (" + SqlConstants.COL_MESSAGE_CONTENT_ID + "=status." + SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID + ")" + 
				" INNER JOIN " + SqlConstants.TABLE_MESSAGE_TO_USER + " info" + 
					" ON (" + SqlConstants.COL_MESSAGE_CONTENT_ID + "=info." + SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID + ")" + 
			" WHERE " + SqlConstants.COL_MESSAGE_TO_USER_ROOT_ID + "=:" + ROOT_MESSAGE_ID_PARAM_NAME+
			" AND (" + SqlConstants.COL_MESSAGE_CONTENT_CREATED_BY + "=:" + USER_ID_PARAM_NAME + 
				" OR status." + SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID + "=:" + USER_ID_PARAM_NAME + ")";
	
	private static final String SELECT_MESSAGES_IN_CONVERSATION = 
			"SELECT DISTINCT(" + SqlConstants.COL_MESSAGE_CONTENT_ID + ") AS EXTRA_ID_COLUMN," + 
					SqlConstants.TABLE_MESSAGE_CONTENT + ".*," + "info.*" + 
			FROM_MESSAGES_IN_CONVERSATION_CORE;
	
	private static final String COUNT_MESSAGES_IN_CONVERSATION = 
			"SELECT COUNT(DISTINCT(" + SqlConstants.COL_MESSAGE_CONTENT_ID + "))" + 
			FROM_MESSAGES_IN_CONVERSATION_CORE;
	
	private static final String FILTER_MESSAGES_RECEIVED = 
			SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID + "=:" + USER_ID_PARAM_NAME + 
			" AND " + SqlConstants.COL_MESSAGE_STATUS + " IN (:" + INBOX_FILTER_PARAM_NAME + ")";
	
	private static final String SELECT_MESSAGES_RECEIVED = 
			"SELECT * FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + "," +
					SqlConstants.TABLE_MESSAGE_TO_USER + " info," + 
					SqlConstants.TABLE_MESSAGE_STATUS + " status" + 
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=info." + SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID + 
			" AND " +  SqlConstants.COL_MESSAGE_CONTENT_ID + "=status." + SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID + 
			" AND " + FILTER_MESSAGES_RECEIVED;
	
	private static final String COUNT_MESSAGES_RECEIVED = 
			"SELECT COUNT(*) FROM " + SqlConstants.TABLE_MESSAGE_STATUS+
			" WHERE " + FILTER_MESSAGES_RECEIVED;
	
	private static final String FROM_MESSAGES_SENT_CORE = 
			" FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + "," + SqlConstants.TABLE_MESSAGE_TO_USER +  
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=" + SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID +  
			" AND " + SqlConstants.COL_MESSAGE_CONTENT_CREATED_BY + "=:" + USER_ID_PARAM_NAME;
	
	private static final String SELECT_MESSAGES_SENT = 
			"SELECT *" + FROM_MESSAGES_SENT_CORE;
	
	private static final String COUNT_MESSAGES_SENT = 
			"SELECT COUNT(*)" + FROM_MESSAGES_SENT_CORE;
	
	private static final String COUNT_ACTUAL_RECIPIENTS_OF_MESSAGE =
			"SELECT COUNT(*) FROM " + SqlConstants.TABLE_MESSAGE_STATUS + 
			" WHERE " +SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID + "=:" + MESSAGE_ID_PARAM_NAME;
	
	private static final RowMapper<DBOMessageContent> messageContentRowMapper = new DBOMessageContent().getTableMapping();
	private static final RowMapper<DBOMessageToUser> messageToUserRowMapper = new DBOMessageToUser().getTableMapping();
	private static final RowMapper<DBOMessageRecipient> messageRecipientRowMapper = new DBOMessageRecipient().getTableMapping();
	private static final RowMapper<DBOMessageStatus> messageStatusRowMapper = new DBOMessageStatus().getTableMapping();
	
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
	
	private static final RowMapper<MessageBundle> messageBundleRowMapper = new RowMapper<MessageBundle>() {
		@Override
		public MessageBundle mapRow(ResultSet rs, int rowNum) throws SQLException {
			MessageBundle bundle = new MessageBundle();
			bundle.setMessage(messageRowMapper.mapRow(rs, rowNum));
			
			DBOMessageStatus messageStatus = messageStatusRowMapper.mapRow(rs, rowNum);
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
			suffix.append(SqlConstants.COL_MESSAGE_CONTENT_CREATED_ON);
			break;
		case SUBJECT:
			suffix.append(SqlConstants.COL_MESSAGE_TO_USER_SUBJECT);
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
	public MessageToUser getMessage(String messageId) throws NotFoundException {
		// Get the message content and info 
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		MessageToUser bundle = simpleJdbcTemplate.queryForObject(SELECT_MESSAGE_BY_ID, messageRowMapper, params);
		
		// Then get the recipients
		List<DBOMessageRecipient> recipients = simpleJdbcTemplate.query(SELECT_MESSAGE_RECIPIENTS_BY_ID, messageRecipientRowMapper, params);
		MessageUtils.copyDBOToDTO(recipients, bundle);
		return bundle;
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
		
		// Send a CREATE message
		ChangeMessage change = new ChangeMessage();
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.MESSAGE);
		change.setObjectId(messageId.toString());
		change.setObjectEtag(content.getEtag());
		transactionalMessenger.sendMessageAfterCommit(change);
		
		// Insert the message info
		info.setMessageId(messageId);
		if (info.getInReplyTo() == null) {
			info.setRootMessageId(messageId);
		} else {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue(MESSAGE_ID_PARAM_NAME, info.getInReplyTo());
			try {
				Long rootMessage = simpleJdbcTemplate.queryForLong(SELECT_ROOT_MESSAGE_ID_BY_ID, params);
				info.setRootMessageId(rootMessage);
			} catch (EmptyResultDataAccessException e) {
				throw new IllegalArgumentException("Cannot reply to a message (" + info.getInReplyTo() + ") that does not exist");
			}
		}
		MessageUtils.validateDBO(info);
		basicDAO.createNew(info);
		
		// Insert the message recipients
		for (int i = 0; i < recipients.size(); i++) {
			recipients.get(i).setMessageId(messageId);
			MessageUtils.validateDBO(recipients.get(i));
		}
		basicDAO.createBatch(recipients);
		
		MessageToUser bundle = new MessageToUser();
		MessageUtils.copyDBOToDTO(content, info, recipients, bundle);
		return bundle;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void touch(String messageId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(ETAG_PARAM_NAME, UUID.randomUUID().toString());
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		simpleJdbcTemplate.update(UPDATE_ETAG_OF_MESSAGE, params);
	}
	
	/**
	 * Fills in the intended recipients of all supplied messages 
	 */
	private void fillInMessageRecipients(List<MessageToUser> messages) {
		if (messages.size() <= 0) {
			return;
		}
		
		List<String> messageIds = new ArrayList<String>();
		for (MessageToUser message : messages) {
			messageIds.add(message.getId());
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageIds);
		List<DBOMessageRecipient> recipients = simpleJdbcTemplate.query(SELECT_MESSAGE_RECIPIENTS_BY_ID, messageRecipientRowMapper, params);
		MessageUtils.copyDBOToDTO(recipients, messages);
	}

	@Override
	public List<MessageToUser> getConversation(String rootMessageId, String userId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_IN_CONVERSATION + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(ROOT_MESSAGE_ID_PARAM_NAME, rootMessageId);
		params.addValue(USER_ID_PARAM_NAME, userId);
		List<MessageToUser> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		
		fillInMessageRecipients(messages);
		return messages;
	}

	@Override
	public long getConversationSize(String rootMessageId, String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(ROOT_MESSAGE_ID_PARAM_NAME, rootMessageId);
		params.addValue(USER_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_IN_CONVERSATION, params);
	}

	@Override
	public List<MessageBundle> getReceivedMessages(String userId, List<MessageStatusType> included, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_RECEIVED + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		params.addValue(INBOX_FILTER_PARAM_NAME, convertFilterToString(included));
		List<MessageBundle> bundles =  simpleJdbcTemplate.query(sql, messageBundleRowMapper, params);

		List<MessageToUser> messages = new ArrayList<MessageToUser>();
		for (MessageBundle bundle : bundles) {
			messages.add(bundle.getMessage());
		}
		fillInMessageRecipients(messages);
		return bundles;
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
	public List<MessageToUser> getSentMessages(String userId, MessageSortBy sortBy,
			boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_SENT + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		List<MessageToUser> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		
		fillInMessageRecipients(messages);
		return messages;
	}

	@Override
	public long getNumSentMessages(String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_MESSAGES_SENT, params);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	public void createMessageStatus_NewTransaction(String messageId, String userId, MessageStatusType status) {
		createMessageStatus(messageId, userId, status);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void createMessageStatus_SameTransaction(String messageId, String userId, MessageStatusType status) {
		createMessageStatus(messageId, userId, status);
	}
	
	/**
	 * Helper method for both the exposed methods that create a message status
	 */
	private void createMessageStatus(String messageId, String userId, MessageStatusType status) {
		if (status == null) {
			status = MessageStatusType.UNREAD;
		}
		
		DBOMessageStatus dbo = new DBOMessageStatus();
		dbo.setMessageId(Long.parseLong(messageId));
		dbo.setRecipientId(Long.parseLong(userId));
		dbo.setStatus(status);
		MessageUtils.validateDBO(dbo);
		basicDAO.createNew(dbo);
		
		touch(messageId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean updateMessageStatus(MessageStatus status) {
		DBOMessageStatus toUpdate = MessageUtils.convertDTO(status);
		MessageUtils.validateDBO(toUpdate);
		boolean success = basicDAO.update(toUpdate);
		
		if (success) {
		touch(status.getMessageId());
	}
		return success;
	}

	@Override
	public void deleteMessage(String messageId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		basicDAO.deleteObjectByPrimaryKey(DBOMessageContent.class, params);
	}

	@Override
	public boolean hasMessageBeenSent(String messageId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		long recipients = simpleJdbcTemplate.queryForLong(COUNT_ACTUAL_RECIPIENTS_OF_MESSAGE, params);
		return recipients > 0;
	}
}
