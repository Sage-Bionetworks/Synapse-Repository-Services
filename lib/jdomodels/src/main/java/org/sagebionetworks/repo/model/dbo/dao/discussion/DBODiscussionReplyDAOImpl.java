package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionReply;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DiscussionReplyUtils;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
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

	private RowMapper<DiscussionReplyBundle> DISCUSSION_REPLY_BUNDLE_ROW_MAPPER = new RowMapper<DiscussionReplyBundle>(){

		@Override
		public DiscussionReplyBundle mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DiscussionReplyBundle dto = new DiscussionReplyBundle();
			dto.setId(Long.toString(rs.getLong(COL_DISCUSSION_REPLY_ID)));
			dto.setThreadId(Long.toString(rs.getLong(COL_DISCUSSION_REPLY_THREAD_ID)));
			dto.setForumId(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_FORUM_ID)));
			dto.setProjectId(KeyFactory.keyToString(rs.getLong(COL_FORUM_PROJECT_ID)));
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

	private RowMapper<DiscussionThreadReplyStat> DISCUSSION_THREAD_REPLY_STAT_ROW_MAPPER = new RowMapper<DiscussionThreadReplyStat>(){

		@Override
		public DiscussionThreadReplyStat mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DiscussionThreadReplyStat dto = new DiscussionThreadReplyStat();
			dto.setThreadId(rs.getLong(COL_DISCUSSION_REPLY_THREAD_ID));
			dto.setNumberOfReplies(rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES));
			dto.setLastActivity(rs.getTimestamp(COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY).getTime());
			return dto;
		}
		
	};

	private static final String SQL_SELECT_THREAD_REPLY_STAT = "SELECT "
			+COL_DISCUSSION_REPLY_THREAD_ID+", "
			+"COUNT(*) AS "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+", "
			+"MAX("+COL_DISCUSSION_REPLY_MODIFIED_ON+") AS "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" "
			+"FROM "+TABLE_DISCUSSION_REPLY+" "
			+"GROUP BY "+COL_DISCUSSION_REPLY_THREAD_ID+" "
			+"ORDER BY "+COL_DISCUSSION_REPLY_THREAD_ID+" "
			+"LIMIT ? OFFSET ?";

	private static final String SQL_SELECT_THREAD_AUTHOR_STAT = "SELECT "+COL_DISCUSSION_REPLY_CREATED_BY
			+" FROM "+TABLE_DISCUSSION_REPLY
			+" WHERE "+COL_DISCUSSION_REPLY_THREAD_ID+" = ?"
			+" GROUP BY "+COL_DISCUSSION_REPLY_CREATED_BY
			+" ORDER BY COUNT(*) DESC"
			+" LIMIT 5";

	private static final String SQL_SELECT_REPLY_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_REPLY
			+" WHERE "+COL_DISCUSSION_REPLY_THREAD_ID+" = ?";
	private static final String SQL_SELECT_REPLY_BUNDLE = "SELECT "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_ID+" AS "+COL_DISCUSSION_REPLY_ID+" , "
			+COL_DISCUSSION_REPLY_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_FORUM_ID+", "
			+COL_FORUM_PROJECT_ID+", "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_MESSAGE_KEY+" AS "+COL_DISCUSSION_REPLY_MESSAGE_KEY+" , "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_CREATED_BY+" AS "+COL_DISCUSSION_REPLY_CREATED_BY+", "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_CREATED_ON+" AS "+COL_DISCUSSION_REPLY_CREATED_ON+", "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_MODIFIED_ON+" AS "+COL_DISCUSSION_REPLY_MODIFIED_ON+", "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_ETAG+" AS "+COL_DISCUSSION_REPLY_ETAG+", "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_IS_EDITED+" AS "+COL_DISCUSSION_REPLY_IS_EDITED+", "
			+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_IS_DELETED+" AS "+COL_DISCUSSION_REPLY_IS_DELETED
			+" FROM "+TABLE_DISCUSSION_REPLY+", "+TABLE_DISCUSSION_THREAD+", "+TABLE_FORUM
			+" WHERE "+COL_DISCUSSION_REPLY_THREAD_ID+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID
			+" AND "+COL_DISCUSSION_THREAD_FORUM_ID+" = "+TABLE_FORUM+"."+COL_FORUM_ID;
	private static final String SQL_GET_REPLY_BY_ID = SQL_SELECT_REPLY_BUNDLE
			+" AND "+TABLE_DISCUSSION_REPLY+"."+COL_DISCUSSION_REPLY_ID+" = ?";
	private static final String SQL_GET_REPLIES_BY_THREAD_ID = SQL_SELECT_REPLY_BUNDLE
			+" AND "+COL_DISCUSSION_REPLY_THREAD_ID+" = ?";
	private static final String ORDER_BY_CREATED_ON = " ORDER BY "+COL_DISCUSSION_REPLY_CREATED_ON;
	private static final String DESC = " DESC";
	private static final String LIMIT = " LIMIT ";
	private static final String OFFSET = " OFFSET ";
	public static final Long MAX_LIMIT = 100L;

	private static final String SQL_SELECT_ETAG_FOR_UPDATE = "SELECT "+COL_DISCUSSION_REPLY_ETAG
			+" FROM "+TABLE_DISCUSSION_REPLY
			+" WHERE "+COL_DISCUSSION_REPLY_ID+" = ? FOR UPDATE";
	private static final String SQL_MARK_REPLY_AS_DELETED = "UPDATE "+TABLE_DISCUSSION_REPLY
			+" SET "+COL_DISCUSSION_REPLY_IS_DELETED+" = TRUE, "
			+COL_DISCUSSION_REPLY_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_REPLY_ID+" = ?";
	private static final String SQL_UPDATE_MESSAGE_KEY = "UPDATE "+TABLE_DISCUSSION_REPLY
			+" SET "+COL_DISCUSSION_REPLY_MESSAGE_KEY+" = ?, "
			+COL_DISCUSSION_REPLY_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_REPLY_ETAG+" = ?, "
			+COL_DISCUSSION_REPLY_MODIFIED_ON+" =? "
			+" WHERE "+COL_DISCUSSION_REPLY_ID+" = ?";

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle createReply(String threadId, String replyId, String messageKey, Long userId) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(replyId, "replyId");
		ValidateArgument.required(messageKey, "messageKey");
		ValidateArgument.required(userId, "userId");
		Long id = Long.parseLong(replyId);
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
			Long threadId, Long limit, Long offset, DiscussionReplyOrder order,
			Boolean ascending) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
				"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		ValidateArgument.requirement((order == null && ascending == null)
			|| (order != null && ascending != null),"order and ascending must be both null or not null");

		PaginatedResults<DiscussionReplyBundle> results = new PaginatedResults<DiscussionReplyBundle>();
		List<DiscussionReplyBundle> replies = new ArrayList<DiscussionReplyBundle>();
		long replyCount = getReplyCount(threadId);
		results.setTotalNumberOfResults(replyCount);

		if (replyCount > 0) {
			String query = SQL_GET_REPLIES_BY_THREAD_ID;
			if (order != null) {
				switch (order) {
					case CREATED_ON:
						query += ORDER_BY_CREATED_ON;
						break;
					default:
						throw new IllegalArgumentException("Unsupported order "+order);
				}
				if (!ascending) {
					query += DESC;
				}
			}
			query += LIMIT+limit+OFFSET+offset;
			replies = jdbcTemplate.query(query,  DISCUSSION_REPLY_BUNDLE_ROW_MAPPER, threadId);
		}

		results.setResults(replies);
		return results;
	}

	@Override
	public long getReplyCount(long threadId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_REPLY_COUNT, threadId);
	}

	@WriteTransactionReadCommitted
	@Override
	public void markReplyAsDeleted(long replyId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_MARK_REPLY_AS_DELETED, etag, replyId);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle updateMessageKey(long replyId, String newKey) {
		ValidateArgument.required(newKey, "newKey");
		String etag = UUID.randomUUID().toString();
		Timestamp modifiedOn = new Timestamp(new Date().getTime());
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_KEY, newKey, etag, modifiedOn, replyId);
		return getReply(replyId);
	}

	@WriteTransactionReadCommitted
	@Override
	public String getEtagForUpdate(long replyId) {
		List<String> results = jdbcTemplate.query(SQL_SELECT_ETAG_FOR_UPDATE, new RowMapper<String>(){

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_DISCUSSION_REPLY_ETAG);
			}
		}, replyId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@Override
	public List<DiscussionThreadReplyStat> getThreadReplyStat(Long limit,
			Long offset) {
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
				"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		return jdbcTemplate.query(SQL_SELECT_THREAD_REPLY_STAT, DISCUSSION_THREAD_REPLY_STAT_ROW_MAPPER, limit, offset);
	}

	@Override
	public DiscussionThreadAuthorStat getDiscussionThreadAuthorStat(long threadId) {
		DiscussionThreadAuthorStat dto = new DiscussionThreadAuthorStat();
		dto.setThreadId(threadId);
		List<String> authors = jdbcTemplate.query(SQL_SELECT_THREAD_AUTHOR_STAT, new RowMapper<String>(){

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_DISCUSSION_REPLY_CREATED_BY);
			}
		}, threadId);
		dto.setActiveAuthors(authors);
		return dto;
	}

}
