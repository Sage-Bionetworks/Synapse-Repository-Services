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
public class DBOMessageThreadObject implements MigratableDatabaseObject<DBOMessageThreadObject, DBOMessageThreadObject> {
	private Long threadId;
	private Long objectId;
	private String objectType;
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("threadId", SqlConstants.COL_MESSAGE_THREAD_OBJECT_THREAD_ID, true).withIsBackupId(true), 
		new FieldColumn("objectId", SqlConstants.COL_MESSAGE_THREAD_OBJECT_ID, true), 
		new FieldColumn("objectType", SqlConstants.COL_MESSAGE_THREAD_OBJECT_TYPE, true)
	};
	
	@Override
	public TableMapping<DBOMessageThreadObject> getTableMapping() {
		return new TableMapping<DBOMessageThreadObject>() {

			@Override
			public DBOMessageThreadObject mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOMessageThreadObject dbo = new DBOMessageThreadObject();
				dbo.setThreadId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_OBJECT_THREAD_ID));
				dbo.setObjectId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_OBJECT_ID));
				dbo.setObjectType(rs.getString(SqlConstants.COL_MESSAGE_THREAD_OBJECT_TYPE));
				return dbo;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_THREAD_OBJECT;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_THREAD_OBJECT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMessageThreadObject> getDBOClass() {
				return DBOMessageThreadObject.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_THREAD_OBJECT;
	}


	@Override
	public MigratableTableTranslation<DBOMessageThreadObject, DBOMessageThreadObject> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOMessageThreadObject, DBOMessageThreadObject>(){

			@Override
			public DBOMessageThreadObject createDatabaseObjectFromBackup(
					DBOMessageThreadObject backup) {
				return backup;
			}

			@Override
			public DBOMessageThreadObject createBackupFromDatabaseObject(DBOMessageThreadObject dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBOMessageThreadObject> getBackupClass() {
		return DBOMessageThreadObject.class;
	}


	@Override
	public Class<? extends DBOMessageThreadObject> getDatabaseObjectClass() {
		return DBOMessageThreadObject.class;
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

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((objectType == null) ? 0 : objectType.hashCode());
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
		DBOMessageThreadObject other = (DBOMessageThreadObject) obj;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectType == null) {
			if (other.objectType != null)
				return false;
		} else if (!objectType.equals(other.objectType))
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
		return "DBOMessageThreadObject [threadId=" + threadId + ", objectId="
				+ objectId + ", objectType=" + objectType + "]";
	}

}
