package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_STATS_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DISCUSSION_THREAD_STATS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD_STATS;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBODiscussionThreadStats implements DatabaseObject<DBODiscussionThreadStats> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("threadId", COL_DISCUSSION_THREAD_STATS_THREAD_ID, true).withIsBackupId(true),
		new FieldColumn("numberOfViews", COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS),
		new FieldColumn("numberOfReplies", COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES),
		new FieldColumn("activeAuthors", COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS)
	};

	private Long threadIs;
	private Long numberOfViews;
	private Long numberOfReplies;
	private byte[] activeAuthors;

	@Override
	public String toString() {
		return "DBODiscussionThreadStats [threadIs=" + threadIs
				+ ", numberOfViews=" + numberOfViews + ", numberOfReplies="
				+ numberOfReplies + ", activeAuthors="
				+ Arrays.toString(activeAuthors) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(activeAuthors);
		result = prime * result
				+ ((numberOfReplies == null) ? 0 : numberOfReplies.hashCode());
		result = prime * result
				+ ((numberOfViews == null) ? 0 : numberOfViews.hashCode());
		result = prime * result
				+ ((threadIs == null) ? 0 : threadIs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBODiscussionThreadStats other = (DBODiscussionThreadStats) obj;
		if (!Arrays.equals(activeAuthors, other.activeAuthors))
			return false;
		if (numberOfReplies == null) {
			if (other.numberOfReplies != null)
				return false;
		} else if (!numberOfReplies.equals(other.numberOfReplies))
			return false;
		if (numberOfViews == null) {
			if (other.numberOfViews != null)
				return false;
		} else if (!numberOfViews.equals(other.numberOfViews))
			return false;
		if (threadIs == null) {
			if (other.threadIs != null)
				return false;
		} else if (!threadIs.equals(other.threadIs))
			return false;
		return true;
	}

	public Long getThreadIs() {
		return threadIs;
	}

	public void setThreadIs(Long threadIs) {
		this.threadIs = threadIs;
	}

	public Long getNumberOfViews() {
		return numberOfViews;
	}

	public void setNumberOfViews(Long numberOfViews) {
		this.numberOfViews = numberOfViews;
	}

	public Long getNumberOfReplies() {
		return numberOfReplies;
	}

	public void setNumberOfReplies(Long numberOfReplies) {
		this.numberOfReplies = numberOfReplies;
	}

	public byte[] getActiveAuthors() {
		return activeAuthors;
	}

	public void setActiveAuthors(byte[] activeAuthors) {
		this.activeAuthors = activeAuthors;
	}

	@Override
	public TableMapping<DBODiscussionThreadStats> getTableMapping() {
		return new TableMapping<DBODiscussionThreadStats>(){

			@Override
			public DBODiscussionThreadStats mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBODiscussionThreadStats dbo = new DBODiscussionThreadStats();
				dbo.setThreadIs(rs.getLong(COL_DISCUSSION_THREAD_STATS_THREAD_ID));
				dbo.setNumberOfViews(rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS));
				dbo.setNumberOfReplies(rs.getLong(COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES));
				Blob blob = rs.getBlob(COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS);
				dbo.setActiveAuthors(blob.getBytes(1, (int) blob.length()));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DISCUSSION_THREAD_STATS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DISCUSSION_THREAD_STATS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODiscussionThreadStats> getDBOClass() {
				return DBODiscussionThreadStats.class;
			}
			
		};
	}
}
