package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Mapping between messages and their associated message thread
 */
public class DBOMessageThread implements MigratableDatabaseObject<DBOMessageThread, DBOMessageThread> {
	private Long threadId;
	private Long messageId;
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("threadId", SqlConstants.COL_MESSAGE_THREAD_ID, true), 
		new FieldColumn("messageId", SqlConstants.COL_MESSAGE_THREAD_MESSAGE_ID, true).withIsBackupId(true)
	};
	
	@Override
	public TableMapping<DBOMessageThread> getTableMapping() {
		return new TableMapping<DBOMessageThread>() {

			@Override
			public DBOMessageThread mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOMessageThread dbo = new DBOMessageThread();
				dbo.setThreadId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_ID));
				dbo.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_MESSAGE_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_THREAD;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_THREAD;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMessageThread> getDBOClass() {
				return DBOMessageThread.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_THREAD;
	}


	@Override
	public MigratableTableTranslation<DBOMessageThread, DBOMessageThread> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOMessageThread, DBOMessageThread>(){

			@Override
			public DBOMessageThread createDatabaseObjectFromBackup(
					DBOMessageThread backup) {
				return backup;
			}

			@Override
			public DBOMessageThread createBackupFromDatabaseObject(DBOMessageThread dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBOMessageThread> getBackupClass() {
		return DBOMessageThread.class;
	}


	@Override
	public Class<? extends DBOMessageThread> getDatabaseObjectClass() {
		return DBOMessageThread.class;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result
				+ ((threadId == null) ? 0 : threadId.hashCode());
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
		DBOMessageThread other = (DBOMessageThread) obj;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		if (threadId == null) {
			if (other.threadId != null)
				return false;
		} else if (!threadId.equals(other.threadId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMessageThread [threadId=" + threadId + ", messageId="
				+ messageId + "]";
	}

}
