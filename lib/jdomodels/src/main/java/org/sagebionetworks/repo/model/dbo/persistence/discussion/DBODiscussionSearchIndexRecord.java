package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_REPLY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_SEARCH_INDEX_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DISCUSSION_SEARCH_INDEX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_SEARCH_INDEX;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Represents a record in the search index for the discussion forums
 */
public class DBODiscussionSearchIndexRecord implements DatabaseObject<DBODiscussionSearchIndexRecord> {
	
	public static final Long NO_REPLY_ID = -1L;
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("forumId", COL_DISCUSSION_SEARCH_INDEX_FORUM_ID, true),
		new FieldColumn("threadId", COL_DISCUSSION_SEARCH_INDEX_THREAD_ID, true),
		new FieldColumn("replyId", COL_DISCUSSION_SEARCH_INDEX_REPLY_ID, true),
		new FieldColumn("threadDeleted", COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED),
		new FieldColumn("replyDeleted", COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED),
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
			indexRecord.setThreadDeleted(rs.getBoolean(COL_DISCUSSION_SEARCH_INDEX_THREAD_DELETED));
			indexRecord.setReplyId(rs.getLong(COL_DISCUSSION_SEARCH_INDEX_REPLY_ID));
			indexRecord.setReplyDeleted(rs.getBoolean(COL_DISCUSSION_SEARCH_INDEX_REPLY_DELETED));
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
	private Boolean threadDeleted;
	private Boolean replyDeleted;
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
	
	public Boolean getThreadDeleted() {
		return threadDeleted;
	}
	
	public void setThreadDeleted(Boolean threadDeleted) {
		this.threadDeleted = threadDeleted;
	}
	
	public Long getReplyId() {
		return replyId;
	}

	public void setReplyId(Long replyId) {
		this.replyId = replyId;
	}
	
	public Boolean getReplyDeleted() {
		return replyDeleted;
	}
	
	public void setReplyDeleted(Boolean replyDeleted) {
		this.replyDeleted = replyDeleted;
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
		return Objects.hash(forumId, replyDeleted, replyId, searchContent, threadDeleted, threadId);
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
		return Objects.equals(forumId, other.forumId) && Objects.equals(replyDeleted, other.replyDeleted)
				&& Objects.equals(replyId, other.replyId) && Objects.equals(searchContent, other.searchContent)
				&& Objects.equals(threadDeleted, other.threadDeleted) && Objects.equals(threadId, other.threadId);
	}

	@Override
	public String toString() {
		return "DBODiscussionSearchIndexRecord [forumId=" + forumId + ", threadId=" + threadId + ", replyId=" + replyId + ", threadDeleted="
				+ threadDeleted + ", replyDeleted=" + replyDeleted + ", searchContent=" + searchContent + "]";
	}

}
