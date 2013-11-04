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
 * Mapping between entities and their associated message thread
 */
public class DBONodeMessages implements MigratableDatabaseObject<DBONodeMessages, DBONodeMessages> {
	private Long nodeId;
	private Long threadId;
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("nodeId", SqlConstants.COL_NODE_MESSAGES_NODE_ID, true).withIsBackupId(true),
		new FieldColumn("threadId", SqlConstants.COL_NODE_MESSAGES_THREAD_ID)
	};
	
	@Override
	public TableMapping<DBONodeMessages> getTableMapping() {
		return new TableMapping<DBONodeMessages>() {

			@Override
			public DBONodeMessages mapRow(ResultSet rs, int index)
					throws SQLException {
				DBONodeMessages dbo = new DBONodeMessages();
				dbo.setNodeId(rs.getLong(SqlConstants.COL_NODE_MESSAGES_NODE_ID));
				dbo.setThreadId(rs.getLong(SqlConstants.COL_NODE_MESSAGES_THREAD_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_NODE_MESSAGES;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_NODE_MESSAGES;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBONodeMessages> getDBOClass() {
				return DBONodeMessages.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE_MESSAGES;
	}


	@Override
	public MigratableTableTranslation<DBONodeMessages, DBONodeMessages> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBONodeMessages, DBONodeMessages>(){

			@Override
			public DBONodeMessages createDatabaseObjectFromBackup(
					DBONodeMessages backup) {
				return backup;
			}

			@Override
			public DBONodeMessages createBackupFromDatabaseObject(DBONodeMessages dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBONodeMessages> getBackupClass() {
		return DBONodeMessages.class;
	}


	@Override
	public Class<? extends DBONodeMessages> getDatabaseObjectClass() {
		return DBONodeMessages.class;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
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
		DBONodeMessages other = (DBONodeMessages) obj;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
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
		return "DBONodeMessages [nodeId=" + nodeId + ", threadId=" + threadId
				+ "]";
	}

}
