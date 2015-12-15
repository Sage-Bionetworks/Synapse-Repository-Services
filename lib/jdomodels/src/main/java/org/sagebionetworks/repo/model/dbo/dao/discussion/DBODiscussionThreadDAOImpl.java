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
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadViewStat;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
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

	private RowMapper<DiscussionThreadViewStat> DISCUSSION_THREAD_VIEW_STAT_ROW_MAPPER = new RowMapper<DiscussionThreadViewStat>(){

		@Override
		public DiscussionThreadViewStat mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DiscussionThreadViewStat dto = new DiscussionThreadViewStat();
			dto.setThreadId(rs.getLong(COL_DISCUSSION_THREAD_VIEW_THREAD_ID));
			dto.setNumberOfViews(rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS));
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
	private static final String SQL_UPDATE_MESSAGE_KEY = "UPDATE "+TABLE_DISCUSSION_THREAD
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
	private static final String SQL_SELECT_THREAD_VIEW_STAT = "SELECT "
			+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+", "
			+"COUNT(*) AS "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" "
			+"FROM "+TABLE_DISCUSSION_THREAD_VIEW+" "
			+"GROUP BY "+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+" "
			+"ORDER BY "+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+" "
			+"LIMIT ? OFFSET ?";

	private static final String SQL_UPDATE_THREAD_STATS_VIEWS = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" ) VALUES (?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" = ? ";
	private static final String SQL_UPDATE_THREAD_STATS_ACTIVE_AUTHORS = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" ) VALUES (?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" = ? ";
	private static final String SQL_UPDATE_THREAD_REPLY_STATS =  "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+", "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" ) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" = ?, "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" = ? ";

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle createThread(String forumId, String threadId, String title, String messageKey, long userId) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(title, "title");
		ValidateArgument.required(messageKey, "messageUrl");
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

	@WriteTransactionReadCommitted
	@Override
	public void updateThreadView(long threadId, long userId) {
		long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_VIEW_ID);
		jdbcTemplate.update(SQL_UPDATE_THREAD_VIEW_TABLE, id, threadId, userId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending) {
		ValidateArgument.required(limit,"limit");
		ValidateArgument.required(offset,"offset");
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
					"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		ValidateArgument.requirement((order == null && ascending == null)
				|| (order != null && ascending != null),"order and ascending must be both null or not null");

		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		List<DiscussionThreadBundle> results = new ArrayList<DiscussionThreadBundle>();
		long threadCount = getThreadCount(forumId);
		threads.setTotalNumberOfResults(threadCount);

		if (threadCount > 0) {
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
						throw new IllegalArgumentException("Unsupported order "+order);
				}
				if (!ascending) {
					query += DESC;
				}
			}

			query += " LIMIT "+limit+" OFFSET "+offset;
			results = jdbcTemplate.query(query, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, forumId);
		}

		threads.setResults(results);
		return threads;
	}

	@WriteTransactionReadCommitted
	@Override
	public void markThreadAsDeleted(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_MARK_THREAD_AS_DELETED, etag, threadId);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateMessageKey(long threadId, String newMessageKey) {
		if (newMessageKey == null) {
			throw new IllegalArgumentException("newMessageKey");
		}
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_KEY, newMessageKey, etag, threadId);
		return getThread(threadId);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateTitle(long threadId, String title) {
		if (title == null) {
			throw new IllegalArgumentException("title");
		}
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_TITLE, title, etag, threadId);
		return getThread(threadId);
	}

	@Override
	public long getThreadCount(long forumId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_COUNT, forumId);
	}

	@WriteTransactionReadCommitted
	@Override
	public void updateThreadViewStat(final List<DiscussionThreadViewStat> stats) {
		jdbcTemplate.batchUpdate(SQL_UPDATE_THREAD_STATS_VIEWS, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ps.setLong(1, stats.get(i).getThreadId());
				ps.setLong(2, stats.get(i).getNumberOfViews());
				ps.setLong(3, stats.get(i).getNumberOfViews());
			}

			@Override
			public int getBatchSize() {
				return stats.size();
			}
		});
	}

	@WriteTransactionReadCommitted
	@Override
	public void updateThreadAuthorStat(DiscussionThreadAuthorStat stat) {
		List<String> authors = new ArrayList<String>();
		authors.addAll(stat.getActiveAuthors());
		String list = DiscussionThreadUtils.toString(authors);
		jdbcTemplate.update(SQL_UPDATE_THREAD_STATS_ACTIVE_AUTHORS, stat.getThreadId(), list, list);
	}

	@Override
	public long countThreadView(long threadId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_VIEW_COUNT, threadId);
	}

	@WriteTransactionReadCommitted
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

	@Override
	public List<DiscussionThreadViewStat> getThreadViewStat(Long limit, Long offset) {
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
				"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		return jdbcTemplate.query(SQL_SELECT_THREAD_VIEW_STAT, DISCUSSION_THREAD_VIEW_STAT_ROW_MAPPER, limit, offset);
	}

	@WriteTransactionReadCommitted
	@Override
	public void updateThreadReplyStat(final List<DiscussionThreadReplyStat> stats) {
		jdbcTemplate.batchUpdate(SQL_UPDATE_THREAD_REPLY_STATS, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ps.setLong(1, stats.get(i).getThreadId());
				ps.setLong(2, stats.get(i).getNumberOfReplies());
				ps.setTimestamp(3, new Timestamp(stats.get(i).getLastActivity()));
				ps.setLong(4, stats.get(i).getNumberOfReplies());
				ps.setTimestamp(5, new Timestamp(stats.get(i).getLastActivity()));
			}

			@Override
			public int getBatchSize() {
				return stats.size();
			}
		});
	}
}
