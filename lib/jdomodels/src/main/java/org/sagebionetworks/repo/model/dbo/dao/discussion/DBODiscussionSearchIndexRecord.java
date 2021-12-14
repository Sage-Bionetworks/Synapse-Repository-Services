package org.sagebionetworks.repo.model.dbo.dao.discussion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

/**
 * Represents a record in the search index for the discussion forums
 */
public class DBODiscussionSearchIndexRecord implements DatabaseObject<DBODiscussionSearchIndexRecord> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("forumId", COL_DISCUSSION_SEARCH_INDEX_FORUM_ID, true),
		new FieldColumn("threadId", COL_DISCUSSION_SEARCH_INDEX_THREAD_ID, true),
		new FieldColumn("replyId", COL_DISCUSSION_SEARCH_INDEX_REPLY_ID, true),
		new FieldColumn("searchContent", COL_DISCUSSION_SEARCH_INDEX_SEARCH_CONTENT)
	};

	private static final TableMapping<DBODiscussionSearchIndexRecord> TABLE_MAPPER = new TableMapping<DBODiscussionSearchIndexRecord>() {

		@Override
		public Class<? extends DBODiscussionSearchIndexRecord> getDBOClass() {
			return DBODiscussionSearchIndexRecord.class;
		}

		@Override
		public DBODiscussionSearchIndexRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODiscussionSearchIndexRecord indexRecord = new DBODiscussionSearchIndexRecord();
			indexRecord.setForumId(rs.getLong(COL_DISCUSSION_SEARCH_INDEX_FORUM_ID));
			indexRecord.setThreadId(rs.getLong(COL_DISCUSSION_SEARCH_INDEX_THREAD_ID));
			indexRecord.setReplyId(rs.getLong(COL_DISCUSSION_SEARCH_INDEX_REPLY_ID));
			indexRecord.setSearchContent(rs.getString(COL_DISCUSSION_SEARCH_INDEX_SEARCH_CONTENT));
			return indexRecord;
		}

		@Override
		public String getTableName() {
			return TABLE_DISCUSSION_SEARCH_INDEX;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DISCUSSION_SEARCH_INDEX;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

	};

	private Long forumId;
	private Long threadId;
	private Long replyId;
	private String searchContent;

	public DBODiscussionSearchIndexRecord() {
	}

	public Long getForumId() {
		return forumId;
	}

	public void setForumId(Long forumId) {
		this.forumId = forumId;
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public Long getReplyId() {
		return replyId;
	}

	public void setReplyId(Long replyId) {
		this.replyId = replyId;
	}

	public String getSearchContent() {
		return searchContent;
	}

	public void setSearchContent(String searchContent) {
		this.searchContent = searchContent;
	}

	@Override
	public TableMapping<DBODiscussionSearchIndexRecord> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public int hashCode() {
		return Objects.hash(forumId, replyId, searchContent, threadId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBODiscussionSearchIndexRecord other = (DBODiscussionSearchIndexRecord) obj;
		return Objects.equals(forumId, other.forumId) && Objects.equals(replyId, other.replyId)
				&& Objects.equals(searchContent, other.searchContent) && Objects.equals(threadId, other.threadId);
	}

	@Override
	public String toString() {
		return "DBODiscussionSearchIndex [forumId=" + forumId + ", threadId=" + threadId + ", replyId=" + replyId + ", searchContent="
				+ searchContent + "]";
	}

}
