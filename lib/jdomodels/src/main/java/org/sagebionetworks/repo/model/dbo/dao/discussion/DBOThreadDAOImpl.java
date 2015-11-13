package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_IS_EDITED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_MESSAGE_URL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THREAD_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_THREAD;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ThreadDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBOThread;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.ThreadUtils;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThread;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOThreadDAOImpl implements ThreadDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private static RowMapper<DBOThread> ROW_MAPPER = new DBOThread().getTableMapping();

	private static final String SQL_MARK_THREAD_AS_DELETED = "UPDATE "+TABLE_THREAD
			+" SET "+COL_THREAD_IS_DELETED+" = TRUE, "
			+COL_THREAD_ETAG+" = ?, "
			+COL_THREAD_MODIFIED_ON+" = ?"
			+" WHERE "+COL_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_TITLE = "UPDATE "+TABLE_THREAD
			+" SET "+COL_THREAD_TITLE+" = ?, "
			+COL_THREAD_IS_EDITED+" = TRUE, "
			+COL_THREAD_ETAG+" = ?, "
			+COL_THREAD_MODIFIED_ON+" = ?"
			+" WHERE "+COL_THREAD_ID+" = ?";
	private static final String SQL_UPDATE_MESSAGE_URL = "UPDATE "+TABLE_THREAD
			+" SET "+COL_THREAD_MESSAGE_URL+" = ?, "
			+COL_THREAD_IS_EDITED+" = TRUE, "
			+COL_THREAD_ETAG+" = ?, "
			+COL_THREAD_MODIFIED_ON+" = ?"
			+" WHERE "+COL_THREAD_ID+" = ?";

	private static final String SQL_SELECT_THREAD_BY_ID = "SELECT * FROM "+TABLE_THREAD
			+" WHERE "+COL_THREAD_ID+" = ?";
	private static final String SQL_SELECT_THREAD_COUNT = "SELECT COUNT(*) FROM "+TABLE_THREAD
			+" WHERE "+COL_THREAD_FORUM_ID+" = ?";
	private static final String SQL_SELECT_THREADS_BY_FORUM_ID = "SELECT * FROM "+TABLE_THREAD
			+" WHERE "+COL_THREAD_FORUM_ID+" = ?";
	private static final String ORDER_BY_LAST_ACTIVITY = " ORDER BY "+COL_THREAD_MODIFIED_ON+" DESC";
	public static final Integer MAX_LIMIT = 100;
	private static final String DEFAULT_LIMIT = " LIMIT "+MAX_LIMIT+" OFFSET 0";

	@WriteTransaction
	@Override
	public DiscussionThread createThread(DiscussionThread dto) {
		if (dto == null) throw new IllegalArgumentException("Thread cannot be null");
		ThreadUtils.validateCreateDTOAndThrowException(dto);
		Long id = idGenerator.generateNewId(TYPE.THREAD_ID);
		dto.setId(id.toString());
		DBOThread dbo = ThreadUtils.createDBOFromDTO(dto);
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.createNew(dbo);
		return getThread(id);
	}

	@Override
	public DiscussionThread getThread(long threadId) {
		List<DBOThread> results = jdbcTemplate.query(SQL_SELECT_THREAD_BY_ID, ROW_MAPPER, threadId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return ThreadUtils.createDTOFromDBO(results.get(0));
	}

	@Override
	public PaginatedResults<DiscussionThread> getThreads(long forumId,
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

		List<DBOThread> results = new ArrayList<DBOThread>();
		results = jdbcTemplate.query(query, ROW_MAPPER, forumId);
		PaginatedResults<DiscussionThread> threads = new PaginatedResults<DiscussionThread>();
		threads.setResults(ThreadUtils.createDTOListFromDBOList(results));
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
	public DiscussionThread updateMessageUrl(long threadId, String newMessageUrl) {
		if (newMessageUrl == null) throw new IllegalArgumentException("Message Url cannot be null");
		String etag = UUID.randomUUID().toString();
		Long modifiedOn = new Date().getTime();
		jdbcTemplate.update(SQL_UPDATE_MESSAGE_URL, newMessageUrl, etag, modifiedOn, threadId);
		return getThread(threadId);
	}

	@WriteTransaction
	@Override
	public DiscussionThread updateTitle(long threadId, byte[] title) {
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
