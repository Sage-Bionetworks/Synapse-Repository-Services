package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionReply;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DiscussionReplyUtils;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBODiscussionReplyDAOImpl implements DiscussionReplyDAO{

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private RowMapper<DiscussionReplyBundle> DISCUSSION_REPLY_BUNDLE_ROW_MAPPER = new RowMapper<DiscussionReplyBundle>(){

		@Override
		public DiscussionReplyBundle mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DiscussionReplyBundle dto = new DiscussionReplyBundle();
			dto.setId(Long.toString(rs.getLong(COL_DISCUSSION_REPLY_ID)));
			dto.setThreadId(Long.toString(rs.getLong(COL_DISCUSSION_REPLY_THREAD_ID)));
			dto.setMessageKey(rs.getString(COL_DISCUSSION_REPLY_MESSAGE_KEY));
			dto.setCreatedBy(Long.toString(rs.getLong(COL_DISCUSSION_REPLY_CREATED_BY)));
			dto.setCreatedOn(new Date(rs.getTimestamp(COL_DISCUSSION_REPLY_CREATED_ON).getTime()));
			dto.setModifiedOn(new Date(rs.getTimestamp(COL_DISCUSSION_REPLY_MODIFIED_ON).getTime()));
			dto.setEtag(rs.getString(COL_DISCUSSION_REPLY_ETAG));
			dto.setIsEdited(rs.getBoolean(COL_DISCUSSION_REPLY_IS_EDITED));
			dto.setIsDeleted(rs.getBoolean(COL_DISCUSSION_REPLY_IS_DELETED));
			return dto;
		}
	};

	private static final String SQL_SELECT_REPLY_BUNDLE = "SELECT "
			+COL_DISCUSSION_REPLY_ID+", "
			+COL_DISCUSSION_REPLY_THREAD_ID+", "
			+COL_DISCUSSION_REPLY_MESSAGE_KEY+", "
			+COL_DISCUSSION_REPLY_CREATED_BY+", "
			+COL_DISCUSSION_REPLY_CREATED_ON+", "
			+COL_DISCUSSION_REPLY_MODIFIED_ON+", "
			+COL_DISCUSSION_REPLY_ETAG+", "
			+COL_DISCUSSION_REPLY_IS_EDITED+", "
			+COL_DISCUSSION_REPLY_IS_DELETED
			+" FROM "+TABLE_DISCUSSION_REPLY;
	private static final String SQL_GET_REPLY_BY_ID = SQL_SELECT_REPLY_BUNDLE
			+" WHERE "+COL_DISCUSSION_REPLY_ID+" = ?";

	@Override
	public DiscussionReplyBundle createReply(String threadId, String messageKey, Long userId) {
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(messageKey, "messageKey cannot be null");
		ValidateArgument.required(userId, "userId cannot be null");
		Long id = idGenerator.generateNewId(TYPE.DISCUSSION_REPLY_ID);
		String etag = UUID.randomUUID().toString();
		DBODiscussionReply dbo = DiscussionReplyUtils.createDBO(threadId, messageKey, userId, id, etag);
		basicDao.createNew(dbo);
		return getReply(id);
	}

	@Override
	public DiscussionReplyBundle getReply(long replyId) {
		List<DiscussionReplyBundle> results = jdbcTemplate.query(SQL_GET_REPLY_BY_ID, DISCUSSION_REPLY_BUNDLE_ROW_MAPPER, replyId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			long threadId, Long limit, Long offset, DiscussionReplyOrder order,
			Boolean ascending) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getReplyCount(long threadId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void markReplyAsDeleted(long replyId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DiscussionReplyBundle updateMessageKey(long replyId, String newKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEtagForUpdate(long replyId) {
		// TODO Auto-generated method stub
		return null;
	}

}
