package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_EDITED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_MESSAGE_URL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBODiscussionThreadDAOImpl implements DiscussionThreadDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private RowMapper<DiscussionThreadBundle> BUNDLE_ROW_MAPPER = new RowMapper<DiscussionThreadBundle>(){

		@Override
		public DiscussionThreadBundle mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}
	};

	private static final String SQL_MARK_THREAD_AS_DELETED = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_IS_DELETED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ?, "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+" = ?"
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_TITLE = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_TITLE+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ?, "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+" = ?"
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_MESSAGE_URL = "UPDATE "+TABLE_DISCUSSION_THREAD
			+" SET "+COL_DISCUSSION_THREAD_MESSAGE_URL+" = ?, "
			+COL_DISCUSSION_THREAD_IS_EDITED+" = TRUE, "
			+COL_DISCUSSION_THREAD_ETAG+" = ?, "
			+COL_DISCUSSION_THREAD_MODIFIED_ON+" = ?"
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";

	private static final String SQL_SELECT_THREAD_BY_ID = "SELECT * FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_ID+" = ?";
	private static final String SQL_SELECT_THREAD_COUNT = "SELECT COUNT(*) FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String SQL_SELECT_THREADS_BY_FORUM_ID = "SELECT * FROM "+TABLE_DISCUSSION_THREAD
			+" WHERE "+COL_DISCUSSION_THREAD_FORUM_ID+" = ?";
	private static final String ORDER_BY_LAST_ACTIVITY = " ORDER BY "+COL_DISCUSSION_THREAD_MODIFIED_ON+" DESC";
	public static final Integer MAX_LIMIT = 100;
	private static final String DEFAULT_LIMIT = " LIMIT "+MAX_LIMIT+" OFFSET 0";

	@WriteTransaction
	@Override
	public DiscussionThreadBundle createThread(String forumId, String title, String messageUrl, Long userId) {
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
		List<DiscussionThreadBundle> results = jdbcTemplate.query(SQL_SELECT_THREAD_BY_ID, BUNDLE_ROW_MAPPER, threadId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			DiscussionOrder order, Integer limit, Integer offset) {
		if (order != null && order != DiscussionOrder.LAST_ACTIVITY) {
			throw new IllegalArgumentException("Does not support order type: "+ order);
		}
		if (limit != null && offset != null ) {
			ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
					"limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		} else {
			ValidateArgument.requirement(limit == null && offset == null,
					"Both limit and offset must be null or not null");
		}

		String query = SQL_SELECT_THREADS_BY_FORUM_ID;
		if (order != null) query += ORDER_BY_LAST_ACTIVITY;
		if (limit != null && offset != null) {
			query += " LIMIT "+limit+" OFFSET "+offset;
		} else {
			query += DEFAULT_LIMIT;
		}

		List<DiscussionThreadBundle> results = new ArrayList<DiscussionThreadBundle>();
		results = jdbcTemplate.query(query, BUNDLE_ROW_MAPPER, forumId);
		PaginatedResults<DiscussionThreadBundle> threads = new PaginatedResults<DiscussionThreadBundle>();
		threads.setResults(results);
		return threads;
	}

	@WriteTransaction
	@Override
	public void deleteThread(long threadId) {
		String etag = UUID.randomUUID().toString();
		Long modifiedOn = new Date().getTime();
		jdbcTemplate.update(SQL_MARK_THREAD_AS_DELETED, etag, modifiedOn, threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessageUrl(long threadId, String newMessageUrl) {
		if (newMessageUrl == null) throw new IllegalArgumentException("Message Url cannot be null");
		String etag = UUID.randomUUID().toString();
		Long modifiedOn = new Date().getTime();
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_URL, newMessageUrl, etag, modifiedOn, threadId);
		return getThread(threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(long threadId, String title) {
		if (title == null) throw new IllegalArgumentException("Title cannot be null");
		String etag = UUID.randomUUID().toString();
		Long modifiedOn = new Date().getTime();
		jdbcTemplate.update(SQL_UPDATE_TITLE, title, etag, modifiedOn, threadId);
		return getThread(threadId);
	}

	@Override
	public long getThreadCount(long forumId) {
		return jdbcTemplate.queryForLong(SQL_SELECT_THREAD_COUNT, forumId);
	}
}
