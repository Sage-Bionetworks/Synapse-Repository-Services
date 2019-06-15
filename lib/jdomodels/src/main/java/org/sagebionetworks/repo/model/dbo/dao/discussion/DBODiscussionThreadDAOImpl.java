package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_EDITED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_PINNED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_MESSAGE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_VIEW_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_VIEW_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD_STATS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD_VIEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;

import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionThread;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DiscussionThreadUtils;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadStat;
import org.sagebionetworks.repo.model.discussion.EntityThreadCount;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBODiscussionThreadDAOImpl implements DiscussionThreadDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedTemplate;
	@Autowired
	private DBOBasicDao basicDao;

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
			dto.setIsPinned(rs.getBoolean(COL_DISCUSSION_THREAD_IS_PINNED));
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
				dto.setActiveAuthors(new ArrayList<String>());
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
	private static final String SQL_MARK_THREAD_AS_NOT_DELETED = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_IS_DELETED+" = FALSE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_PIN_THREAD = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_IS_PINNED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UNPIN_THREAD = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_IS_PINNED+" = FALSE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_TITLE = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_TITLE+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ?, "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_MESSAGE_KEY = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_MESSAGE_KEY+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ?, "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+" = ? "
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_THREAD_ETAG = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_ETAG+" = ?"
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SELECT_PROJECT_ID = "SELECT "
			+TABLE_FORUM+"."+COL_FORUM_PROJECT_ID
			+" FROM "+TABLE_DISCUSSION_THREAD+", "+TABLE_FORUM
			+" WHERE "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_FORUM_ID
			+" = "+TABLE_FORUM+"."+COL_FORUM_ID
			+ " AND "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SELECT_AUTHOR = "SELECT "+COL_DISCUSSION_THREAD_CREATED_BY
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SELECT_IS_DELETED = "SELECT "+COL_DISCUSSION_THREAD_IS_DELETED
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SELECT_THREAD_BUNDLE = "SELECT "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" AS "+COL_DISCUSSION_THREAD_ID+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_FORUM_ID+" AS "+COL_DISCUSSION_THREAD_FORUM_ID+", "
			+TABLE_FORUM+"."+COL_FORUM_PROJECT_ID+" AS "+COL_FORUM_PROJECT_ID+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_TITLE+" AS "+COL_DISCUSSION_THREAD_TITLE+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_CREATED_ON+" AS "+COL_DISCUSSION_THREAD_CREATED_ON+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_CREATED_BY+" AS "+COL_DISCUSSION_THREAD_CREATED_BY+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_MODIFIED_ON+" AS "+COL_DISCUSSION_THREAD_MODIFIED_ON+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ETAG+" AS "+COL_DISCUSSION_THREAD_ETAG+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_MESSAGE_KEY+" AS "+COL_DISCUSSION_THREAD_MESSAGE_KEY+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_IS_EDITED+" AS "+COL_DISCUSSION_THREAD_IS_EDITED+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_IS_DELETED+" AS "+COL_DISCUSSION_THREAD_IS_DELETED+", "
			+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_IS_PINNED+" AS "+COL_DISCUSSION_THREAD_IS_PINNED+", "
			+TABLE_DISCUSSION_THREAD_STATS+"."+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" AS "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+", "
			+TABLE_DISCUSSION_THREAD_STATS+"."+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" AS "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+", "
			+"IFNULL("+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+", "+COL_DISCUSSION_THREAD_MODIFIED_ON+") AS "+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+", "
			+TABLE_DISCUSSION_THREAD_STATS+"."+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" AS "+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" JOIN "+TABLE_FORUM
			+" ON "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_FORUM_ID
			+" = "+TABLE_FORUM+"."+COL_FORUM_ID
			+" LEFT OUTER JOIN "+TABLE_DISCUSSION_THREAD_STATS
			+" ON "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID
			+" = "+TABLE_DISCUSSION_THREAD_STATS+"."+COL_DISCUSSION_THREAD_STATS_THREAD_ID;
	public static final String NOT_DELETED_CONDITION = " AND "+COL_DISCUSSION_THREAD_IS_DELETED+" = FALSE";
	public static final String DELETED_CONDITION = " AND "+COL_DISCUSSION_THREAD_IS_DELETED+" = TRUE";
	private static final String SQL_SELECT_THREAD_BY_ID = SELECT_THREAD_BUNDLE
			+" WHERE "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_SELECT_THREAD_COUNT_FOR_FORUM = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	public static final String SQL_SELECT_THREADS_BY_FORUM_ID = SELECT_THREAD_BUNDLE
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String ORDER_BY_PINNED_AND_LAST_ACTIVITY = " ORDER BY "+COL_DISCUSSION_THREAD_IS_PINNED+" DESC, "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY;
	private static final String ORDER_BY_NUMBER_OF_VIEWS = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS;
	private static final String ORDER_BY_NUMBER_OF_REPLIES = " ORDER BY "+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES;
	private static final String ORDER_BY_THREAD_TITLE = " ORDER BY LOWER(CONVERT ("+COL_DISCUSSION_THREAD_TITLE+" USING UTF8))";
	private static final String DESC = " DESC ";

	// for entity references
	private static final String ENTITY_IDS = "entityIds";
	private static final String ID = "id";
	private static final String PROJECT_IDS = "projectIds";

	public static final String FORUM_THREAD_ENTITY_REF_JOIN = 
			" FROM "+TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE+", "
					+TABLE_DISCUSSION_THREAD+", "
					+TABLE_FORUM
			+" WHERE "+TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE+"."+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID
					+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID
			+" AND "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_FORUM_ID
					+" = "+TABLE_FORUM+"."+COL_FORUM_ID;
	public static final String ENTITY_ID_CONDITION = COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID+" = :"+ID;
	public static final String PROJECT_CONDITION = COL_FORUM_PROJECT_ID+" IN (:"+PROJECT_IDS+")";
	public static final String ENTITY_LIST_CONDITION = COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID+" IN (:"+ENTITY_IDS+")";

	// Count the unique threads that mentioned the given entity and belong to a project in the given list
	public static final String SQL_SELECT_THREAD_COUNT_FOR_ENTITY =
			"SELECT COUNT(DISTINCT "+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID+")"
			+FORUM_THREAD_ENTITY_REF_JOIN
			+" AND "+ENTITY_ID_CONDITION
			+" AND "+PROJECT_CONDITION;

	// Get thread bundles that mentioned the given entity and belong to a project in the given list
	public static final String SQL_SELECT_THREADS_BY_ENTITY_ID = SELECT_THREAD_BUNDLE
			+" JOIN "+TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE
			+" ON "+TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE+"."+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID
			+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID
			+" WHERE "+ENTITY_ID_CONDITION
			+" AND "+PROJECT_CONDITION;

	public static final String SQL_INSERT_IGNORE_ENTITY_REFERENCE = "INSERT IGNORE INTO "
			+TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE+"("
			+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID+") "
			+ "VALUES (?,?)";

	public static final String THREAD_COUNT = "THREAD_COUNT";

	// get a list of (entityId, count distinct threads) for a given list of entity,
	// with threads belong to a project in a given list
	public static final String SQL_GET_THREAD_COUNTS = "SELECT "
			+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID+", "
			+"COUNT(DISTINCT "+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID+") AS "+THREAD_COUNT
			+ FORUM_THREAD_ENTITY_REF_JOIN
			+ " AND "+ENTITY_LIST_CONDITION
			+ " AND "+PROJECT_CONDITION
			+" GROUP BY "+COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID;

	// Get a list of project that contains threads that mentioned an entity in the given list
	public static final String SQL_GET_PROJECTS = "SELECT DISTINCT "+COL_FORUM_PROJECT_ID
			+FORUM_THREAD_ENTITY_REF_JOIN
			+" AND "+ENTITY_LIST_CONDITION;

	// This query is used by the stats worker. It's critical to keep the order by threadId to prevent deadlock.
	public static final String SQL_SELECT_ALL_THREAD_ID = "SELECT "+COL_DISCUSSION_THREAD_ID
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" ORDER BY "+COL_DISCUSSION_THREAD_ID
			+" LIMIT ? OFFSET ?";

	public static final String SQL_SELECT_ALL_THREAD_ID_FOR_FORUM = "SELECT "+COL_DISCUSSION_THREAD_ID
			+" FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";

	private static final String SQL_UPDATE_THREAD_VIEW_TABLE = "INSERT IGNORE INTO "
			+TABLE_DISCUSSION_THREAD_VIEW+" ("
			+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+","
			+COL_DISCUSSION_THREAD_VIEW_USER_ID
			+") VALUES (?,?)";
	private static final String SQL_SELECT_THREAD_VIEW_COUNT = "SELECT COUNT(*)"
			+" FROM "+TABLE_DISCUSSION_THREAD_VIEW
			+" WHERE "+COL_DISCUSSION_THREAD_VIEW_THREAD_ID+" = ?";

	private static final String SQL_UPDATE_THREAD_STATS = "INSERT INTO "
			+TABLE_DISCUSSION_THREAD_STATS+" ("
			+COL_DISCUSSION_THREAD_STATS_THREAD_ID+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+", "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+", "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+", "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" ) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS+" = ?, "
			+COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS+" = ?, "
			+COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES+" = ?, "
			+COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY+" = ? ";
	public static final DiscussionFilter DEFAULT_FILTER = DiscussionFilter.NO_FILTER;

	@WriteTransaction
	@Override
	public DiscussionThreadBundle createThread(String forumId, String threadId, String title, String messageKey, long userId) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(title, "title");
		ValidateArgument.required(messageKey, "messageUrl");
		String etag = UUID.randomUUID().toString();
		DBODiscussionThread dbo = DiscussionThreadUtils.createDBO(forumId, title, messageKey, userId, threadId, etag);
		basicDao.createNew(dbo);
		return getThread(Long.parseLong(threadId), DEFAULT_FILTER);
	}

	@Override
	public DiscussionThreadBundle getThread(long threadId, DiscussionFilter filter) {
		String query = addCondition(SQL_SELECT_THREAD_BY_ID, filter);
		List<DiscussionThreadBundle> results = jdbcTemplate.query(query, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, threadId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@WriteTransaction
	@Override
	public void updateThreadView(long threadId, long userId) {
		jdbcTemplate.update(SQL_UPDATE_THREAD_VIEW_TABLE, threadId, userId);
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UPDATE_THREAD_ETAG, etag, threadId);
	}

	@Override
	public List<DiscussionThreadBundle> getThreadsForForum(long forumId,
			Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending,
			DiscussionFilter filter) {
		ValidateArgument.required(limit,"limit");
		ValidateArgument.required(offset,"offset");
		ValidateArgument.required(filter, "filter");
		ValidateArgument.requirement((order == null && ascending == null)
				|| (order != null && ascending != null),"order and ascending must be both null or not null");
		String query = buildGetQuery(SQL_SELECT_THREADS_BY_FORUM_ID, limit, offset, order, ascending, filter);
		return jdbcTemplate.query(query, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER, forumId);
	}

	protected static String buildGetQuery(String query, Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter) {
		query = addCondition(query, filter);
		if (order != null) {
			switch (order) {
				case NUMBER_OF_REPLIES:
					query += ORDER_BY_NUMBER_OF_REPLIES;
					break;
				case NUMBER_OF_VIEWS:
					query += ORDER_BY_NUMBER_OF_VIEWS;
					break;
				case PINNED_AND_LAST_ACTIVITY:
					query += ORDER_BY_PINNED_AND_LAST_ACTIVITY;
					break;
				case THREAD_TITLE:
					query += ORDER_BY_THREAD_TITLE;
					break;
				default:
					throw new IllegalArgumentException("Unsupported order "+order);
			}
			if (!ascending) {
				query += DESC;
			}
		}
		query += " LIMIT "+limit+" OFFSET "+offset;
		return query;
	}

	/**
	 * Add condition part to the input query based on the filter.
	 * 
	 * @param query
	 * @param filter
	 * @return
	 */
	protected static String addCondition(String query, DiscussionFilter filter) {
		switch (filter) {
			case NO_FILTER:
				break;
			case DELETED_ONLY:
				query += DELETED_CONDITION;
				break;
			case EXCLUDE_DELETED:
				query += NOT_DELETED_CONDITION;
				break;
		}
		return query;
	}

	@WriteTransaction
	@Override
	public void markThreadAsDeleted(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_MARK_THREAD_AS_DELETED, etag, threadId);
	}

	@WriteTransaction
	@Override
	public void pinThread(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_PIN_THREAD, etag, threadId);
	}

	@WriteTransaction
	@Override
	public void unpinThread(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_UNPIN_THREAD, etag, threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessageKey(long threadId, String newMessageKey) {
		if (newMessageKey == null) {
			throw new IllegalArgumentException("newMessageKey");
		}
		String etag = UUID.randomUUID().toString();
		Timestamp modifiedOn = new Timestamp(new Date().getTime());
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_KEY, newMessageKey, etag, modifiedOn, threadId);
		return getThread(threadId, DEFAULT_FILTER);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(long threadId, String title) {
		if (title == null) {
			throw new IllegalArgumentException("title");
		}
		String etag = UUID.randomUUID().toString();
		Timestamp modifiedOn = new Timestamp(new Date().getTime());
		jdbcTemplate.update(SQL_UPDATE_TITLE, title, etag, modifiedOn, threadId);
		return getThread(threadId, DEFAULT_FILTER);
	}


	@Override
	public long getThreadCountForForum(long forumId, DiscussionFilter filter) {
		ValidateArgument.required(filter, "filter");
		return jdbcTemplate.queryForObject(addCondition(SQL_SELECT_THREAD_COUNT_FOR_FORUM, filter), Long.class, forumId);
	}

	@Override
	public long countThreadView(long threadId) {
		return jdbcTemplate.queryForObject(SQL_SELECT_THREAD_VIEW_COUNT, Long.class, threadId);
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

	@Override
	public List<Long> getAllThreadId(Long limit, Long offset) {
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return jdbcTemplate.query(SQL_SELECT_ALL_THREAD_ID, new RowMapper<Long>(){

			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(COL_DISCUSSION_THREAD_ID);
			}
		}, limit, offset);
	}

	@Override
	public String getProjectId(String threadId) {
		List<String> queryResult = jdbcTemplate.query(SELECT_PROJECT_ID, new RowMapper<String>(){
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return KeyFactory.keyToString(rs.getLong(COL_FORUM_PROJECT_ID));
			}
		}, threadId);
		if (queryResult.size() != 1) {
			throw new NotFoundException();
		}
		return queryResult.get(0);
	}

	@Override
	public String getAuthorForUpdate(String threadId) {
		String query = addCondition(SELECT_AUTHOR, DiscussionFilter.EXCLUDE_DELETED);
		List<String> queryResult = jdbcTemplate.query(query, new RowMapper<String>(){
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_DISCUSSION_THREAD_CREATED_BY);
			}
		}, threadId);
		if (queryResult.size() != 1) {
			throw new NotFoundException();
		}
		return queryResult.get(0);
	}

	@Override
	public boolean isThreadDeleted(String threadId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_IS_DELETED, Boolean.class, threadId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public long getThreadCountForEntity(long entityId, DiscussionFilter filter, Set<Long> projectIds) {
		ValidateArgument.required(filter, "filter");
		ValidateArgument.required(projectIds, "projectIds");
		if (projectIds.isEmpty()) {
			return 0L;
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource(ID, entityId);
		parameters.addValue(PROJECT_IDS, projectIds);
		String countQuery = addCondition(SQL_SELECT_THREAD_COUNT_FOR_ENTITY, filter);
		return namedTemplate.queryForObject(countQuery, parameters, Long.class);
	}

	@Override
	public List<DiscussionThreadBundle> getThreadsForEntity(long entityId, Long limit, Long offset,
			DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter, Set<Long> projectIds) {
		ValidateArgument.required(limit,"limit");
		ValidateArgument.required(offset,"offset");
		ValidateArgument.required(filter, "filter");
		ValidateArgument.required(projectIds, "projectIds");
		ValidateArgument.requirement((order == null && ascending == null)
				|| (order != null && ascending != null),"order and ascending must be both null or not null");

		if (projectIds.isEmpty()) {
			return new ArrayList<DiscussionThreadBundle>();
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource(ID, entityId);
		parameters.addValue(PROJECT_IDS, projectIds);
		String query = buildGetQuery(SQL_SELECT_THREADS_BY_ENTITY_ID, limit, offset, order, ascending, filter);
		return namedTemplate.query(query, parameters, DISCUSSION_THREAD_BUNDLE_ROW_MAPPER);
	}


	@WriteTransaction
	@Override
	public void insertEntityReference(final List<DiscussionThreadEntityReference> refs) {
		jdbcTemplate.batchUpdate(SQL_INSERT_IGNORE_ENTITY_REFERENCE, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ps.setLong(1, Long.parseLong(refs.get(i).getThreadId()));
				ps.setLong(2, KeyFactory.stringToKey(refs.get(i).getEntityId()));
			}

			@Override
			public int getBatchSize() {
				return refs.size();
			}
		});
	}

	@Override
	public EntityThreadCounts getThreadCounts(List<Long> entityIds, Set<Long> projectIds) {
		ValidateArgument.required(entityIds, "entityIds");
		ValidateArgument.required(projectIds, "projectIds");
		EntityThreadCounts threadCounts = new EntityThreadCounts();
		List<EntityThreadCount> queryResult = new ArrayList<EntityThreadCount>();
		threadCounts.setList(queryResult);
		if (entityIds.isEmpty() || projectIds.isEmpty()) {
			return threadCounts;
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource(ENTITY_IDS, entityIds);
		parameters.addValue(PROJECT_IDS, projectIds);
		threadCounts.setList(namedTemplate.query(SQL_GET_THREAD_COUNTS, parameters, new RowMapper<EntityThreadCount>(){

			@Override
			public EntityThreadCount mapRow(ResultSet rs, int rowNum) throws SQLException {
				EntityThreadCount threadCount = new EntityThreadCount();
				threadCount.setCount(rs.getLong(THREAD_COUNT));
				threadCount.setEntityId(KeyFactory.keyToString(rs.getLong(COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID)));
				return threadCount;
			}
		}));
		return threadCounts;
	}

	@Override
	public Set<Long> getDistinctProjectIdsOfThreadsReferencesEntityIds(List<Long> entityIds) {
		ValidateArgument.required(entityIds, "entityIds");
		final Set<Long> result = new HashSet<Long>();
		if (entityIds.isEmpty()) {
			return result;
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource(ENTITY_IDS, entityIds);
		namedTemplate.query(SQL_GET_PROJECTS, parameters, new RowMapper<Void>(){
			@Override
			public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
				result.add(rs.getLong(COL_FORUM_PROJECT_ID));
				return null;
			}
		});
		return result;
	}

	@Override
	public void updateThreadStats(final List<DiscussionThreadStat> stats) {
		jdbcTemplate.batchUpdate(SQL_UPDATE_THREAD_STATS, new BatchPreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement ps, int i)
					throws SQLException {
				ps.setLong(1, stats.get(i).getThreadId());
				if (stats.get(i).getNumberOfViews() == null) {
					ps.setNull(2, Types.BIGINT);
					ps.setNull(6, Types.BIGINT);
				} else {
					ps.setLong(2, stats.get(i).getNumberOfViews());
					ps.setLong(6, stats.get(i).getNumberOfViews());
				}
				if (stats.get(i).getActiveAuthors() == null) {
					ps.setNull(3, Types.VARCHAR);
					ps.setNull(7, Types.VARCHAR);
				} else {
					ps.setString(3, DiscussionThreadUtils.toCsvString(stats.get(i).getActiveAuthors()));
					ps.setString(7, DiscussionThreadUtils.toCsvString(stats.get(i).getActiveAuthors()));
				}
				if (stats.get(i).getNumberOfReplies() == null) {
					ps.setNull(4, Types.BIGINT);
					ps.setNull(8, Types.BIGINT);
				} else {
					ps.setLong(4, stats.get(i).getNumberOfReplies());
					ps.setLong(8, stats.get(i).getNumberOfReplies());
				}
				if (stats.get(i).getLastActivity() == null) {
					ps.setNull(5, Types.TIMESTAMP);
					ps.setNull(9, Types.TIMESTAMP);
				} else {
					ps.setTimestamp(5, new Timestamp(stats.get(i).getLastActivity()));
					ps.setTimestamp(9, new Timestamp(stats.get(i).getLastActivity()));
				}
			}

			@Override
			public int getBatchSize() {
				return stats.size();
			}
		});
	}

	@Override
	public void markThreadAsNotDeleted(long threadId) {
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_MARK_THREAD_AS_NOT_DELETED, etag, threadId);
	}
}
