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
	private Long childMessageId;
	private Long parentMessageId;
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("childMessageId", SqlConstants.COL_MESSAGE_THREAD_CHILD_ID, true).withIsBackupId(true), 
		new FieldColumn("parentMessageId", SqlConstants.COL_MESSAGE_THREAD_PARENT_ID)
	};
	
	@Override
	public TableMapping<DBOMessageThread> getTableMapping() {
		return new TableMapping<DBOMessageThread>() {

			@Override
			public DBOMessageThread mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOMessageThread dbo = new DBOMessageThread();
				dbo.setChildMessageId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_CHILD_ID));
				dbo.setParentMessageId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_PARENT_ID));
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

	public Long getChildMessageId() {
		return childMessageId;
	}

	public void setChildMessageId(Long childMessageId) {
		this.childMessageId = childMessageId;
	}

	public Long getParentMessageId() {
		return parentMessageId;
	}

	public void setParentMessageId(Long parentMessageId) {
		this.parentMessageId = parentMessageId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((childMessageId == null) ? 0 : childMessageId.hashCode());
		result = prime * result
				+ ((parentMessageId == null) ? 0 : parentMessageId.hashCode());
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
		if (childMessageId == null) {
			if (other.childMessageId != null)
				return false;
		} else if (!childMessageId.equals(other.childMessageId))
			return false;
		if (parentMessageId == null) {
			if (other.parentMessageId != null)
				return false;
		} else if (!parentMessageId.equals(other.parentMessageId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMessageThread [childMessageId=" + childMessageId
				+ ", parentMessageId=" + parentMessageId + "]";
	}

}
