package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessage;
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
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String THREAD_ID_PARAM_NAME = "threadId";
	
	private static final String SELECT_MESSAGES_IN_THREAD = 
			"SELECT * FROM "+SqlConstants.TABLE_MESSAGE + 
			" WHERE "+SqlConstants.COL_MESSAGE_THREAD_ID+"=:" + THREAD_ID_PARAM_NAME;
	
	private static final RowMapper<DBOMessage> messageRowMapper = new DBOMessage().getTableMapping();
	
	/**
	 * Builds up ordering and pagination keywords to append to various message select statements
	 */
	private static String constructSqlSuffix(MESSAGE_SORT_BY sortBy,
			boolean descending, long limit, long offset) {
		StringBuilder suffix = new StringBuilder();
		suffix.append(" ORDER BY ");
		switch (sortBy) {
		case SENDER:
			suffix.append(SqlConstants.COL_MESSAGE_CREATED_BY);
			break;
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
	public void saveMessage(Message dto) {
		// Fill in new IDs for the message
		dto.setMessageId(idGenerator.generateNewId(TYPE.MESSAGE_ID).toString());
		if (dto.getThreadId() == null) {
			dto.setThreadId(idGenerator.generateNewId(TYPE.MESSAGE_THREAD_ID).toString());
		}
		
		MessageUtils.validateDTO(dto);
		
		basicDAO.createNew(MessageUtils.convertDTO(dto));
	}

	@Override
	public List<Message> getThread(String threadId, MESSAGE_SORT_BY sortBy,
			boolean descending, long limit, long offset)
			throws NotFoundException {
		String sql = SELECT_MESSAGES_IN_THREAD + constructSqlSuffix(sortBy, descending, limit, offset);
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(THREAD_ID_PARAM_NAME, threadId);
		List<DBOMessage> messages = simpleJdbcTemplate.query(sql, messageRowMapper, params);
		return MessageUtils.convertDBOs(messages);
	}

	@Override
	public long getThreadSize(String threadId) throws NotFoundException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<MessageBundle> getReceivedMessages(String userId,
			MESSAGE_SORT_BY sortBy, boolean descending, long limit, long offset)
			throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getNumReceivedMessages(String userId) throws NotFoundException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Message> getSentMessages(String userId, MESSAGE_SORT_BY sortBy,
			boolean descending, long limit, long offset)
			throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getNumSentMessages(String userId) throws NotFoundException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void registerMessageRecipient(String messageId, String userId)
			throws NotFoundException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateMessageStatus(String messageId, String userId,
			MessageStatusType status) throws NotFoundException {
		// TODO Auto-generated method stub
		
	}
}
