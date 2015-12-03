package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionThread;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DiscussionThreadUtils;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

public class DBODiscussionThreadDAOImpl implements DiscussionThreadDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	public static final Charset UTF8 = Charset.forName("UTF-8");
	private RowMapper<DiscussionThreadBundle> DISCUSSION_THREAD_BUNDLE_ROW_MAPPER = new RowMapper<DiscussionThreadBundle>(){

		@Override
		public DiscussionThreadBundle mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DiscussionThreadBundle dto = new DiscussionThreadBundle();
			dto.setId(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_ID)));
			dto.setForumId(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_FORUM_ID)));
			dto.setProjectId(KeyFactory.keyToString(rs.getLong(COL_FORUM_PROJECT_ID)));
			Blob titleBlob = rs.getBlob(COL_DISCUSSION_THREAD_TITLE);
			dto.setTitle(new String(titleBlob.getBytes(1, (int) titleBlob.length()), UTF8));
			dto.setCreatedOn(new Date(rs.getTimestamp(COL_DISCUSSION_THREAD_CREATED_ON).getTime()));
			dto.setCreatedBy(Long.toString(rs.getLong(COL_DISCUSSION_THREAD_CREATED_BY)));
			dto.setModifiedOn(new Date(rs.getTimestamp(COL_DISCUSSION_THREAD_MODIFIED_ON).getTime()));
			dto.setEtag(rs.getString(COL_DISCUSSION_THREAD_ETAG));
			dto.setMessageKey(rs.getString(COL_DISCUSSION_THREAD_MESSAGE_KEY));
			dto.setIsEdited(rs.getBoolean(COL_DISCUSSION_THREAD_IS_EDITED));
			dto.setIsDeleted(rs.getBoolean(COL_DISCUSSION_THREAD_IS_DELETED));
			long numberOfViews = rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS);
			if (rs.wasNull()) {
				dto.setNumberOfViews(0L);
			} else {
				dto.setNumberOfViews(numberOfViews);
			}
			long numberOfReplies = rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES);
			if (rs.wasNull()) {
				dto.setNumberOfReplies(0L);
			} else {
				dto.setNumberOfReplies(numberOfReplies);
			}
			Timestamp lastActivity = rs.getTimestamp(COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY);
			if (rs.wasNull()) {
				dto.setLastActivity(dto.getModifiedOn());
			} else {
				dto.setLastActivity(new Date(lastActivity.getTime()));
			}
			String listString = rs.getString(COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS);
			if (rs.wasNull()) {
				dto.setActiveAuthors(Arrays.asList(dto.getCreatedBy()));
			} else {
				dto.setActiveAuthors(DiscussionThreadUtils.toList(listString));
			}
			return dto;
		}
	};

	private static final String SQL_SELECT_ETAG_FOR_UPDATE = "SELECT "+COL_DISCUSSION_THREAD_ETAG
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ? FOR UPDATE";
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
			+" SET "+COL_DISCUSSION_THREAD_MESSAGE_KEY+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SELECT_THREAD_BUNDLE = "SELECT "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" AS ID, "
			+COL_DISCUSSION_THREAD_FORUM_ID+", "
			+COL_FORUM_PROJECT_ID+", "
			+COL_DISCUSSION_THREAD_TITLE+", "
			+COL_DISCUSSION_THREAD_CREATED_ON+", "
			+COL_DISCUSSION_THREAD_CREATED_BY+", "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+", "
			+COL_DISCUSSION_THREAD_ETAG+", "
			+COL_DISCUSSION_THREAD_MESSAGE_KEY+", "
			+COL_DISCUSSION_THREAD_IS_EDITED+", "
			+COL_DISCUSSION_THREAD_IS_DELETED+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+", "
			+"IFNULL("+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+", "+COL_DISCUSSION_THREAD_MODIFIED_ON+") AS "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+", "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" JOIN "+TABLE_FORUM
			+" ON "+COL_DISCUSSION_THREAD_FORUM_ID
			+" = "+TABLE_FORUM+"."+COL_FORUM_ID
			+" LEFT OUTER JOIN "+TABLE_DISCUSSION_THREAD_STATS
			+" ON "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID
			+" = "+COL_DISCUSSION_THREAD_STATS_THREAD_ID;
	private static final String SQL_SELECT_THREAD_BY_ID = SELECT_THREAD_BUNDLE
			+" WHERE "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_SELECT_THREAD_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String SQL_SELECT_THREADS_BY_FORUM_ID = SELECT_THREAD_BUNDLE
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String ORDER_BY_LAST_ACTIVITY = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY;
	private static final String ORDER_BY_NUMBER_OF_VIEWS = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS;
	private static final String ORDER_BY_NUMBER_OF_REPLIES = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES;
	private static final String DESC = " DESC ";
	public static final Long MAX_LIMIT = 100L;

	private static final String SQL_UPDATE_THREAD_VIEW_TABLE = "INSERT IGNORE INTO "
			+TABLE_DISCUSSION_THREAD_VIEW+" ("
			+COL_DISCUSSION_THREAD_VIEW_ID+","
			+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+","
			+COL_DISCUSSION_THREAD_VIEW_USER_ID
			+") VALUES (?,?,?)";
	private static final String SQL_SELECT_THREAD_VIEW_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_THREAD_VIEW
			+" WHERE "+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+" = ?";

	private static final String SQL_UPDATE_THREAD_STATS_VIEWS = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" ) VALUES (?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" = ? ";
	private static final String SQL_UPDATE_THREAD_STATS_REPLIES = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" ) VALUES (?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" = ? ";
	private static final String SQL_UPDATE_THREAD_STATS_LAST_ACTIVITY = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" ) VALUES (?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" = ? ";
	private static final String SQL_UPDATE_THREAD_STATS_ACTIVE_AUTHORS = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" ) VALUES (?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" = ? ";

	@WriteTransaction
	@Override
	public DiscussionThreadBundle createThread(String forumId, String threadId, String title, String messageKey, long userId) {
		ValidateArgument.requirement(forumId != null, "forumId cannot be null");
		ValidateArgument.requirement(title != null, "title cannot be null");
		ValidateArgument.requirement(messageKey != null, "messageUrl cannot be null");
		String etag = UUID.randomUUID().toString();
		DBODiscussionThread dbo = DiscussionThreadUtils.createDBO(forumId, title, messageKey, userId, threadId, etag);
		basicDao.createNew(dbo);
		return getThread(Long.parseLong(threadId));
	}

	@Override
	public DiscussionThreadBundle getThread(long threadId) {
		List<DiscussionThreadBundle> results = jdbcTemplate.query(SQL_SELECT_THREAD_BY_ID, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, threadId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@WriteTransaction
	@Override
	public void updateThreadView(long threadId, long userId) {
		long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_VIEW_ID);
		jdbcTemplate.update(SQL_UPDATE_THREAD_VIEW_TABLE, id, threadId, userId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending) {
		ValidateArgument.required(limit,"limit cannot be null");
		ValidateArgument.required(offset,"offset cannot be null");
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
					"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		ValidateArgument.required(ascending,"ascending cannot be null");

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
				default:
					break;
			}
		}
		if (!ascending) {
			query += DESC;
		}
		query += " LIMIT "+limit+" OFFSET "+offset;

		List<DiscussionThreadBundle> results = new ArrayList<DiscussionThreadBundle>();
		results = jdbcTemplate.query(query, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, forumId);
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(results);

		return threads;
	}

	@WriteTransaction
	@Override
	public void markThreadAsDeleted(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_MARK_THREAD_AS_DELETED, etag, threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessageKey(long threadId, String newMessageUrl) {
		if (newMessageUrl == null) {
			throw new IllegalArgumentException("Message Url cannot be null");
		}
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_URL, newMessageUrl, etag, threadId);
		return getThread(threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(long threadId, String title) {
		if (title == null) {
			throw new IllegalArgumentException("Title cannot be null");
		}
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_TITLE, title, etag, threadId);
		return getThread(threadId);
	}

	@Override
	public long getThreadCount(long forumId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_COUNT, forumId);
	}

	@WriteTransaction
	@Override
	public void setNumberOfViews(long threadId, long numberOfViews) {
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_VIEWS, threadId, numberOfViews, numberOfViews);
	}

	@WriteTransaction
	@Override
	public void setNumberOfReplies(long threadId, long numberOfReplies) {
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_REPLIES, threadId, numberOfReplies, numberOfReplies);
	}

	@WriteTransaction
	@Override
	public void setActiveAuthors(long threadId, List<String> activeAuthors) {
		String list = DiscussionThreadUtils.toString(activeAuthors);
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_ACTIVE_AUTHORS, threadId, list, list);
	}

	@WriteTransaction
	@Override
	public void setLastActivity(final long threadId, Date lastActivity) {
		final Timestamp timestamp = new Timestamp(lastActivity.getTime());
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_LAST_ACTIVITY, new PreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setLong(1, threadId);
				ps.setTimestamp(2, timestamp);
				ps.setTimestamp(3, timestamp);
			}
		});
	}

	@Override
	public long countThreadView(long threadId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_VIEW_COUNT, threadId);
	}

	@WriteTransaction
	@Override
	public String getEtagForUpdate(long threadId) {
		List<String> results = jdbcTemplate.query(SQL_SELECT_ETAG_FOR_UPDATE, new RowMapper<String>(){

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_DISCUSSION_THREAD_ETAG);
			}
		}, threadId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}
}
