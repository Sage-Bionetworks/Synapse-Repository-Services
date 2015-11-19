package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionThread;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DiscussionThreadUtils;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBODiscussionThreadDAOImpl implements DiscussionThreadDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private RowMapper<DiscussionThreadBundle> DISCUSSION_THREAD_BUNDLE_ROW_MAPPER = new RowMapper<DiscussionThreadBundle>(){

		@Override
		public DiscussionThreadBundle mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DiscussionThreadBundle dbo = new DiscussionThreadBundle();
			dbo.setId(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_ID)));
			dbo.setForumId(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_FORUM_ID)));
			Blob titleBlob = rs.getBlob(COL_DISCUSSION_THREAD_TITLE);
			dbo.setTitle(DiscussionThreadUtils.decompressUTF8(titleBlob.getBytes(1, (int) titleBlob.length())));
			dbo.setCreatedOn(new Date(rs.getTimestamp(COL_DISCUSSION_THREAD_CREATED_ON).getTime()));
			dbo.setCreatedBy(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_CREATED_BY)));
			dbo.setModifiedOn(new Date(rs.getTimestamp(COL_DISCUSSION_THREAD_MODIFIED_ON).getTime()));
			dbo.setMessageUrl(rs.getString(COL_DISCUSSION_THREAD_MESSAGE_URL));
			dbo.setIsEdited(rs.getBoolean(COL_DISCUSSION_THREAD_IS_EDITED));
			dbo.setIsDeleted(rs.getBoolean(COL_DISCUSSION_THREAD_IS_DELETED));
			long numberOfViews = rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS);
			if (rs.wasNull()) {
				dbo.setNumberOfViews(0L);
			} else {
				dbo.setNumberOfViews(numberOfViews);
			}
			long numberOfReplies = rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES);
			if (rs.wasNull()) {
				dbo.setNumberOfReplies(0L);
			} else {
				dbo.setNumberOfReplies(numberOfReplies);
			}
			long lastActivity = rs.getLong(COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY);
			if (rs.wasNull()) {
				dbo.setLastActivity(dbo.getModifiedOn());
			} else {
				dbo.setLastActivity(new Date(lastActivity));
			}
			String listString = rs.getString(COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS);
			if (rs.wasNull()) {
				dbo.setActiveAuthors(Arrays.asList(dbo.getCreatedBy()));
			} else {
				dbo.setActiveAuthors(DiscussionThreadUtils.createList(listString));
			}
			return dbo;
		}
	};

	private static final String SQL_MARK_THREAD_AS_DELETED = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_IS_DELETED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_TITLE = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_TITLE+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_MESSAGE_URL = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_MESSAGE_URL+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SELECT_THREAD_BUNDLE = "SELECT "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" AS ID, "
			+COL_DISCUSSION_THREAD_FORUM_ID+", "
			+COL_DISCUSSION_THREAD_TITLE+", "
			+COL_DISCUSSION_THREAD_CREATED_ON+", "
			+COL_DISCUSSION_THREAD_CREATED_BY+", "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+", "
			+COL_DISCUSSION_THREAD_MESSAGE_URL+", "
			+COL_DISCUSSION_THREAD_IS_EDITED+", "
			+COL_DISCUSSION_THREAD_IS_DELETED+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+", "
			+"IFNULL("+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+", "+COL_DISCUSSION_THREAD_MODIFIED_ON+") AS "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+", "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" LEFT OUTER JOIN "+TABLE_DISCUSSION_THREAD_STATS
			+" ON "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID
			+" = "+TABLE_DISCUSSION_THREAD_STATS+"."+COL_DISCUSSION_THREAD_STATS_THREAD_ID;
	private static final String SQL_SELECT_THREAD_BY_ID = SELECT_THREAD_BUNDLE
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_SELECT_THREAD_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String SQL_SELECT_THREADS_BY_FORUM_ID = SELECT_THREAD_BUNDLE
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String ORDER_BY_LAST_ACTIVITY = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" DESC";
	private static final String ORDER_BY_NUMBER_OF_VIEWS = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" DESC";
	private static final String ORDER_BY_NUMBER_OF_REPLIES = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" DESC";
	public static final Integer MAX_LIMIT = 100;
	private static final String DEFAULT_LIMIT = " LIMIT "+MAX_LIMIT+" OFFSET 0";

	private static final String SQL_UPDATE_THREAD_VIEW_TABLE = "INSERT IGNORE INTO "
			+TABLE_DISCUSSION_THREAD_VIEW+"("
			+COL_DISCUSSION_THREAD_VIEW_ID+","
			+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+","
			+COL_DISCUSSION_THREAD_VIEW_USER_ID
			+") VALUES (?,?,?)";
	private static final String SQL_SELECT_THREAD_VIEW_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_THREAD_VIEW
			+" WHERE "+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+" = ?";

	private static final String SQL_UPDATE_THREAD_STATS_VIEWS = "UPDATE "+TABLE_DISCUSSION_THREAD_STATS
			+" SET "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_STATS_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_THREAD_STATS_REPLIES = "UPDATE "+TABLE_DISCUSSION_THREAD_STATS
			+" SET "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_STATS_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_THREAD_STATS_LAST_ACTIVITY = "UPDATE "+TABLE_DISCUSSION_THREAD_STATS
			+" SET "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_STATS_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_THREAD_STATS_ACTIVE_AUTHORS = "UPDATE "+TABLE_DISCUSSION_THREAD_STATS
			+" SET "+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_STATS_THREAD_ID+" = ?";

	@WriteTransaction
	@Override
	public DiscussionThreadBundle createThread(String forumId, String title, String messageUrl, long userId) {
		if (forumId == null || title == null || messageUrl == null) 
			throw new IllegalArgumentException("All parameters must be initialized.");
		Long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		String etag = UUID.randomUUID().toString();
		DBODiscussionThread dbo = DiscussionThreadUtils.createDBO(forumId, title, messageUrl, userId, id.toString(), etag);
		basicDao.createNew(dbo);
		return getThread(id);
	}

	@Override
	public DiscussionThreadBundle getThread(long threadId) {
		List<DiscussionThreadBundle> results = jdbcTemplate.query(SQL_SELECT_THREAD_BY_ID, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, threadId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@Override
	public void updateThreadView(long threadId, long userId) {
		long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_VIEW_ID);
		jdbcTemplate.update(SQL_UPDATE_THREAD_VIEW_TABLE, id, threadId, userId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			DiscussionOrder order, Integer limit, Integer offset) {
		if (limit != null && offset != null ) {
			ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
					"limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		} else {
			ValidateArgument.requirement(limit == null && offset == null,
					"Both limit and offset must be null or not null");
		}

		String query = SQL_SELECT_THREADS_BY_FORUM_ID;
		if (order != null) {
			switch (order) {
				case NUMBER_OF_REPLIES:
					query += ORDER_BY_NUMBER_OF_REPLIES;
					break;
				case NUMBER_OF_VIEWS:
					query += ORDER_BY_NUMBER_OF_VIEWS;
					break;
				case LAST_ACTIVITY:
					query += ORDER_BY_LAST_ACTIVITY;
					break;
			}
		}
		if (limit != null && offset != null) {
			query += " LIMIT "+limit+" OFFSET "+offset;
		} else {
			query += DEFAULT_LIMIT;
		}

		List<DiscussionThreadBundle> results = new ArrayList<DiscussionThreadBundle>();
		results = jdbcTemplate.query(query, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, forumId);
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(results);

		return threads;
	}

	@Override
	public void updateThreadView(final List<DiscussionThreadBundle> threads, final long userId) {
		jdbcTemplate.batchUpdate(SQL_UPDATE_THREAD_VIEW_TABLE, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ps.setLong(1, idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_VIEW_ID));
				ps.setLong(2, Long.parseLong(threads.get(i).getId()));
				ps.setLong(3, userId);
			}

			@Override
			public int getBatchSize() {
				return threads.size();
			}
		});
	}

	@WriteTransaction
	@Override
	public void deleteThread(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_MARK_THREAD_AS_DELETED, etag, threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessageUrl(long threadId, String newMessageUrl) {
		if (newMessageUrl == null) throw new IllegalArgumentException("Message Url cannot be null");
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_URL, newMessageUrl, etag, threadId);
		return getThread(threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(long threadId, String title) {
		if (title == null) throw new IllegalArgumentException("Title cannot be null");
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_TITLE, title, etag, threadId);
		return getThread(threadId);
	}

	@Override
	public long getThreadCount(long forumId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_COUNT, forumId);
	}

	@Override
	public void setNumberOfViews(long threadId, long numberOfViews) {
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_VIEWS, numberOfViews, threadId);
	}

	@Override
	public void setNumberOfReplies(long threadId, long numberOfReplies) {
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_REPLIES, numberOfReplies, threadId);
	}

	@Override
	public void setActiveAuthors(long threadId, List<Long> activeAuthors) {
		byte[] bytes = DiscussionThreadUtils.compressUTF8(activeAuthors.toString());
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_ACTIVE_AUTHORS, bytes, threadId);
	}

	@Override
	public void setLastActivity(long threadId, long lastActivity) {
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_LAST_ACTIVITY, lastActivity, threadId);
	}

	@Override
	public long countThreadView(long threadId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_VIEW_COUNT, threadId);
	}
}
