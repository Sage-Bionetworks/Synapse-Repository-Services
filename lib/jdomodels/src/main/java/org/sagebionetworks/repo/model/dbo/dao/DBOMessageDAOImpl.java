package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
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
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;


public class DBOMessageDAOImpl implements MessageDAO {

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String MESSAGE_ID_PARAM_NAME = "messageId";
	private static final String MESSAGE_SENT_PARAM_NAME = "sent";
	private static final String USER_ID_PARAM_NAME = "userId";
	private static final String ETAG_PARAM_NAME = "etag";
	private static final String ROOT_MESSAGE_ID_PARAM_NAME = "rootMessageId";
	private static final String INBOX_FILTER_PARAM_NAME = "inboxFilter";
	private static final String TIMESTAMP_PARAM_NAME = "timestamp";
	private static final String FILEHANDLE_PARAM_NAME = "filehandle";
	
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
	
	private static final String UPDATE_MESSAGE_SENT =
			"UPDATE " + SqlConstants.TABLE_MESSAGE_TO_USER+
			" SET " + SqlConstants.COL_MESSAGE_TO_USER_SENT+ "=:"+MESSAGE_SENT_PARAM_NAME+" WHERE "+ 
			SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID + "=:" + MESSAGE_ID_PARAM_NAME;
	
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
	
	private static final String COUNT_RECENTLY_CREATED_MESSAGES = 
			"SELECT COUNT(*) FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + 
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_CREATED_BY + "=:" + USER_ID_PARAM_NAME +
			" AND " + SqlConstants.COL_MESSAGE_CONTENT_CREATED_ON + ">:" + TIMESTAMP_PARAM_NAME;
	
	private static final String COUNT_VISIBLE_MESSAGES_BY_FILE_HANDLE = 
			"SELECT COUNT(*) FROM " + SqlConstants.TABLE_MESSAGE_CONTENT + "," + SqlConstants.TABLE_MESSAGE_RECIPIENT + 
			" WHERE " + SqlConstants.COL_MESSAGE_CONTENT_ID + "=" + SqlConstants.COL_MESSAGE_RECIPIENT_MESSAGE_ID + 
			" AND " + SqlConstants.COL_MESSAGE_CONTENT_FILE_HANDLE_ID + "=:" + FILEHANDLE_PARAM_NAME + 
			" AND " + SqlConstants.COL_MESSAGE_RECIPIENT_ID + " IN(:" + USER_ID_PARAM_NAME + ")";
	
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
		MessageToUser bundle;
		try {
			bundle = namedJdbcTemplate.queryForObject(SELECT_MESSAGE_BY_ID, params, messageRowMapper);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Message (" + messageId + ") not found");
		}
		
		// Then get the recipients
		List<DBOMessageRecipient> recipients = namedJdbcTemplate.query(SELECT_MESSAGE_RECIPIENTS_BY_ID, params, messageRecipientRowMapper);
		MessageUtils.copyDBOToDTO(recipients, bundle);
		return bundle;
	}

	@Override
	@WriteTransaction
	public MessageToUser createMessage(MessageToUser dto) {
		DBOMessageContent content = new DBOMessageContent();
		DBOMessageToUser info = new DBOMessageToUser();
		List<DBOMessageRecipient> recipients = new ArrayList<DBOMessageRecipient>();
		MessageUtils.copyDTOtoDBO(dto, content, info, recipients);
		
		// Generate an ID for all the new rows
		Long messageId = idGenerator.generateNewId(IdType.MESSAGE_ID);
		
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
				Long rootMessage = namedJdbcTemplate.queryForObject(SELECT_ROOT_MESSAGE_ID_BY_ID, params, Long.class);
				info.setRootMessageId(rootMessage);
			} catch (EmptyResultDataAccessException e) {
				throw new IllegalArgumentException("Cannot reply to a message (" + info.getInReplyTo() + ") that does not exist");
			}
		}
		info.setSent(false);
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
	@WriteTransaction
	public void touch(String messageId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(ETAG_PARAM_NAME, UUID.randomUUID().toString());
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		namedJdbcTemplate.update(UPDATE_ETAG_OF_MESSAGE, params);
	}
	
	@Override
	@WriteTransaction
	public void updateMessageTransmissionAsComplete(String messageId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(MESSAGE_ID_PARAM_NAME, messageId);
		params.addValue(MESSAGE_SENT_PARAM_NAME, true);
		namedJdbcTemplate.update(UPDATE_MESSAGE_SENT, params);
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
		List<DBOMessageRecipient> recipients = namedJdbcTemplate.query(SELECT_MESSAGE_RECIPIENTS_BY_ID, params, messageRecipientRowMapper);
		MessageUtils.copyDBOToDTO(recipients, messages);
	}

	@Override
	public List<MessageToUser> getConversation(String rootMessageId, String userId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_IN_CONVERSATION + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(ROOT_MESSAGE_ID_PARAM_NAME, rootMessageId);
		params.addValue(USER_ID_PARAM_NAME, userId);
		List<MessageToUser> messages = namedJdbcTemplate.query(sql, params, messageRowMapper);
		
		fillInMessageRecipients(messages);
		return messages;
	}

	@Override
	public long getConversationSize(String rootMessageId, String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(ROOT_MESSAGE_ID_PARAM_NAME, rootMessageId);
		params.addValue(USER_ID_PARAM_NAME, userId);
		return namedJdbcTemplate.queryForObject(COUNT_MESSAGES_IN_CONVERSATION, params, Long.class);
	}

	@Override
	public List<MessageBundle> getReceivedMessages(String userId, List<MessageStatusType> included, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		String sql = SELECT_MESSAGES_RECEIVED + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		params.addValue(INBOX_FILTER_PARAM_NAME, convertFilterToString(included));
		List<MessageBundle> bundles =  namedJdbcTemplate.query(sql, params, messageBundleRowMapper);

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
		return namedJdbcTemplate.queryForObject(COUNT_MESSAGES_RECEIVED, params, Long.class);
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
		List<MessageToUser> messages = namedJdbcTemplate.query(sql, params, messageRowMapper);
		
		fillInMessageRecipients(messages);
		return messages;
	}

	@Override
	public long getNumSentMessages(String userId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		return namedJdbcTemplate.queryForObject(COUNT_MESSAGES_SENT, params, Long.class);
	}

	@Override
	@NewWriteTransaction
	public void createMessageStatus_NewTransaction(String messageId, String userId, MessageStatusType status) {
		createMessageStatus(messageId, userId, status);
	}

	@Override
	@WriteTransaction
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
	@NewWriteTransaction
	public boolean updateMessageStatus_NewTransaction(MessageStatus status) {
		return updateMessageStatus(status);
	}
	
	@Override
	@WriteTransaction
	public boolean updateMessageStatus_SameTransaction(MessageStatus status) {
		return updateMessageStatus(status);
	}
	
	private boolean updateMessageStatus(MessageStatus status) {
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
		params.addValue(DBOMessageToUser.MESSAGE_ID_FIELD_NAME, messageId);
		basicDAO.deleteObjectByPrimaryKey(DBOMessageContent.class, params);
	}

	@Override
	public boolean getMessageSent(String messageId) throws NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(DBOMessageToUser.MESSAGE_ID_FIELD_NAME, messageId);
		DBOMessageToUser dbo = basicDAO.getObjectByPrimaryKey(DBOMessageToUser.class, params);
		return dbo.getSent();
	}

	@Override
	public boolean canCreateMessage(String userId, long maxNumberOfNewMessages,
			long messageCreationInterval) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, userId);
		params.addValue(TIMESTAMP_PARAM_NAME, new Date().getTime() - messageCreationInterval);
		long messages = namedJdbcTemplate.queryForObject(COUNT_RECENTLY_CREATED_MESSAGES, params, Long.class);
		return messages < maxNumberOfNewMessages;
	}
	
	@Override
	public boolean canSeeMessagesUsingFileHandle(Set<Long> groupIds, String fileHandleId) {
		if (groupIds.size() <= 0) {
			return false;
		}
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(USER_ID_PARAM_NAME, groupIds);
		params.addValue(FILEHANDLE_PARAM_NAME, fileHandleId);
		long messages = namedJdbcTemplate.queryForObject(COUNT_VISIBLE_MESSAGES_BY_FILE_HANDLE, params, Long.class);
		return messages > 0;
	}


}
