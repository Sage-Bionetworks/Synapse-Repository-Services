package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.net.ntp.TimeStamp;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle.MetadataType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;


/**
 * The DBO object for messages.
 */
public class DBOMessage implements MigratableDatabaseObject<DBOMessage, DBOMessage> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("messageId", SqlConstants.COL_MESSAGE_ID, true).withIsBackupId(true),
		new FieldColumn("threadId", SqlConstants.COL_MESSAGE_THREAD_ID),
		new FieldColumn("createdBy", SqlConstants.COL_MESSAGE_CREATED_BY),
		new FieldColumn("bodyFileId", SqlConstants.COL_MESSAGE_BODY_FILE_ID),
		new FieldColumn("createdOn", SqlConstants.COL_MESSAGE_CREATED_ON),
		new FieldColumn("subject", SqlConstants.COL_MESSAGE_SUBJECT)
	};
	
	private Long messageId;
	private Long threadId;
	private Long createdBy;
	private Long bodyFileId;
	private Timestamp createdOn;
	private String subject;

	@Override
	public TableMapping<DBOMessage> getTableMapping() {
		return new TableMapping<DBOMessage>() {
			
			@Override
			public DBOMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMessage result = new DBOMessage();
				result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_ID));
				result.setThreadId(rs.getLong(SqlConstants.COL_MESSAGE_THREAD_ID));
				result.setCreatedBy(rs.getLong(SqlConstants.COL_MESSAGE_CREATED_BY));
				result.setBodyFileId(rs.getLong(SqlConstants.COL_MESSAGE_BODY_FILE_ID));
				result.setCreatedOn(rs.getTimestamp(SqlConstants.COL_MESSAGE_CREATED_ON));
				result.setSubject(rs.getString(SqlConstants.COL_MESSAGE_SUBJECT));
				return result;
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE;
			}
			
			@Override
			public Class<? extends DBOMessage> getDBOClass() {
				return DBOMessage.class;
			}
		};
	}
	

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}
	
	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getBodyFileId() {
		return bodyFileId;
	}

	public void setBodyFileId(Long bodyFileId) {
		this.bodyFileId = bodyFileId;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE;
	}

	@Override
	public MigratableTableTranslation<DBOMessage, DBOMessage> getTranslator() {
		return new MigratableTableTranslation<DBOMessage, DBOMessage>() {
			@Override
			public DBOMessage createDatabaseObjectFromBackup(DBOMessage backup) {
				return backup;
			}
			
			@Override
			public DBOMessage createBackupFromDatabaseObject(DBOMessage dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOMessage> getBackupClass() {
		return DBOMessage.class;
	}
	
	@Override
	public Class<? extends DBOMessage> getDatabaseObjectClass() {
		return DBOMessage.class;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((bodyFileId == null) ? 0 : bodyFileId.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
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
		DBOMessage other = (DBOMessage) obj;
		if (bodyFileId == null) {
			if (other.bodyFileId != null)
				return false;
		} else if (!bodyFileId.equals(other.bodyFileId))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
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
		return "DBOMessage [messageId=" + messageId + ", threadId=" + threadId
				+ ", createdBy=" + createdBy + ", bodyFileId=" + bodyFileId
				+ ", createdOn=" + createdOn + ", subject=" + subject + "]";
	}

}
