package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_VIEW_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_VIEW_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DISCUSSION_THREAD_VIEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD_VIEW;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODiscussionThreadView implements MigratableDatabaseObject<DBODiscussionThreadView, DBODiscussionThreadView>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("threadId", COL_DISCUSSION_THREAD_VIEW_THREAD_ID, true).withIsBackupId(true),
		new FieldColumn("userId", COL_DISCUSSION_THREAD_VIEW_USER_ID, true),
	};

	private Long threadId;
	private Long userId;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((threadId == null) ? 0 : threadId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		DBODiscussionThreadView other = (DBODiscussionThreadView) obj;
		if (threadId == null) {
			if (other.threadId != null)
				return false;
		} else if (!threadId.equals(other.threadId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODiscussionThreadView [threadId=" + threadId + ", userId=" + userId + "]";
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public TableMapping<DBODiscussionThreadView> getTableMapping() {
		return new TableMapping<DBODiscussionThreadView>() {

			@Override
			public DBODiscussionThreadView mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBODiscussionThreadView dbo = new DBODiscussionThreadView();
				dbo.setThreadId(rs.getLong(COL_DISCUSSION_THREAD_VIEW_THREAD_ID));
				dbo.setUserId(rs.getLong(COL_DISCUSSION_THREAD_VIEW_USER_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DISCUSSION_THREAD_VIEW;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DISCUSSION_THREAD_VIEW;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODiscussionThreadView> getDBOClass() {
				return DBODiscussionThreadView.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DISCUSSION_THREAD_VIEW;
	}

	@Override
	public MigratableTableTranslation<DBODiscussionThreadView, DBODiscussionThreadView> getTranslator() {
		return new BasicMigratableTableTranslation<DBODiscussionThreadView>();
	}

	@Override
	public Class<? extends DBODiscussionThreadView> getBackupClass() {
		return DBODiscussionThreadView.class;
	}

	@Override
	public Class<? extends DBODiscussionThreadView> getDatabaseObjectClass() {
		return DBODiscussionThreadView.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
