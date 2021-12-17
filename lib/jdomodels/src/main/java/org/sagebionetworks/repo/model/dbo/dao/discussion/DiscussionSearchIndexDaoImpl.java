package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_REPLY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_SEARCH_INDEX;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionSearchIndexRecord;
import org.sagebionetworks.repo.model.discussion.Match;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionSearchIndexDaoImpl implements DiscussionSearchIndexDao {
	
	private static final RowMapper<DBODiscussionSearchIndexRecord> RECORD_ROW_MAPPER = new DBODiscussionSearchIndexRecord().getTableMapping();
	
	private static final RowMapper<Match> MATCH_ROW_MAPPER = (rs, rowNum) -> {
		Match match = new Match()
			.setForumId(rs.getString(COL_DISCUSSION_SEARCH_INDEX_FORUM_ID))
			.setThreadId(rs.getString(COL_DISCUSSION_SEARCH_INDEX_THREAD_ID));
		
		Long replyId = rs.getLong(COL_DISCUSSION_SEARCH_INDEX_REPLY_ID);
		
		if (!DBODiscussionSearchIndexRecord.NO_REPLY_ID.equals(replyId)) {
			match.setReplyId(replyId.toString());
		}
		
		return match;
	};
		
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public DiscussionSearchIndexDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
	@WriteTransaction
	public void createOrUpdateRecordForReply(Long forumId, Long threadId, Long replyId, String searchContent) {
		if (DBODiscussionSearchIndexRecord.NO_REPLY_ID.equals(replyId)) {
			throw new IllegalArgumentException("Unexpected replyId: " + replyId);
		}
		createOrUpdate(forumId, threadId, replyId, searchContent);
	}
	
	@Override
	@WriteTransaction
	public void createOrUpdateRecordForThread(Long forumId, Long threadId, String searchContent) {
		createOrUpdate(forumId, threadId, DBODiscussionSearchIndexRecord.NO_REPLY_ID, searchContent);
	}

	private void createOrUpdate(Long forumId, Long threadId, Long replyId, String searchContent) {
		ValidateArgument.required(forumId, "The forumId");
		ValidateArgument.required(threadId, "The threadId");
		ValidateArgument.required(replyId, "The replyId");
		ValidateArgument.required(searchContent, "The searchContent");
		
		String inserSql = "INSERT INTO " + TABLE_DISCUSSION_SEARCH_INDEX + " VALUES(?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + COL_DISCUSSION_SEARCH_INDEX_SEARCH_CONTENT + " = ?";
		
		jdbcTemplate.update(inserSql, forumId, threadId, false, replyId, false, searchContent, searchContent);
	}

	@Override
	@WriteTransaction
	public void markThreadAsDeleted(Long threadId) {
		ValidateArgument.required(threadId, "The threadId");
		
		String updateSql = "UPDATE " + TABLE_DISCUSSION_SEARCH_INDEX + " SET " + COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED + " = TRUE "
			+ " WHERE " + COL_DISCUSSION_SEARCH_INDEX_THREAD_ID + " = ?"; 

		jdbcTemplate.update(updateSql, threadId);		
	}
	
	@Override
	@WriteTransaction
	public void markThreadAsNotDeleted(Long threadId) {
		ValidateArgument.required(threadId, "The threadId");
		
		String updateSql = "UPDATE " + TABLE_DISCUSSION_SEARCH_INDEX + " SET " + COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED + " = FALSE "
				+ " WHERE " + COL_DISCUSSION_SEARCH_INDEX_THREAD_ID + " = ?";
		
		jdbcTemplate.update(updateSql, threadId);
	}
	
	@Override
	@WriteTransaction
	public void markReplyAsDeleted(Long replyId) {
		ValidateArgument.required(replyId, "The replyId");
		
		String updateSql = "UPDATE " + TABLE_DISCUSSION_SEARCH_INDEX + " SET " + COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED + " = TRUE "
				+ " WHERE " + COL_DISCUSSION_SEARCH_INDEX_REPLY_ID + " = ?";

		jdbcTemplate.update(updateSql, replyId);
	}
	
	@Override
	@WriteTransaction
	public void markReplyAsNotDeleted(Long replyId) {
		ValidateArgument.required(replyId, "The replyId");
		
		String updateSql = "UPDATE " + TABLE_DISCUSSION_SEARCH_INDEX + " SET " + COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED + " = FALSE "
				+ " WHERE " + COL_DISCUSSION_SEARCH_INDEX_REPLY_ID + " = ?";
		
		jdbcTemplate.update(updateSql, replyId);
	}
	
	@Override
	public List<Match> search(Long forumId, String searchString, long limit, long offset) {
		ValidateArgument.required(forumId, "The forumId");
		ValidateArgument.required(searchString, "The searchString");
		
		String searchSql = "SELECT " + COL_DISCUSSION_SEARCH_INDEX_FORUM_ID + ", " + COL_DISCUSSION_SEARCH_INDEX_THREAD_ID + ", " + COL_DISCUSSION_SEARCH_INDEX_REPLY_ID 
			+ " FROM " + TABLE_DISCUSSION_SEARCH_INDEX
			+ " WHERE"
			+ " MATCH(" + COL_DISCUSSION_SEARCH_INDEX_SEARCH_CONTENT + ") AGAINST(?)"
			+ " AND " + COL_DISCUSSION_SEARCH_INDEX_FORUM_ID + " = ? "
			+ " AND " + COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED + " IS FALSE" 
			+ " AND " + COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED + " IS FALSE"
			+ " LIMIT ? OFFSET ?";
		
		return jdbcTemplate.query(searchSql, MATCH_ROW_MAPPER, searchString, forumId, limit, offset);
	}
	
	// For testing
	List<DBODiscussionSearchIndexRecord> listRecords(Long forumId) {
		return jdbcTemplate.query("SELECT * FROM " + TABLE_DISCUSSION_SEARCH_INDEX + " WHERE " + COL_DISCUSSION_SEARCH_INDEX_FORUM_ID + " = ?", RECORD_ROW_MAPPER, forumId);
	}

}
