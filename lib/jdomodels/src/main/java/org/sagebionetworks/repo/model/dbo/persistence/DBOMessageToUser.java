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
 * Contains information specific to a message sent to a user
 */
public class DBOMessageToUser implements MigratableDatabaseObject<DBOMessageToUser, DBOMessageToUser> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("messageId", SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID, true).withIsBackupId(true),
		new FieldColumn("subject", SqlConstants.COL_MESSAGE_TO_USER_SUBJECT)
	};
	
	private Long messageId;
	private String subject;


	@Override
	public TableMapping<DBOMessageToUser> getTableMapping() {
		return new TableMapping<DBOMessageToUser>() {
			
			@Override
			public DBOMessageToUser mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMessageToUser result = new DBOMessageToUser();
				result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID));
				result.setSubject(rs.getString(SqlConstants.COL_MESSAGE_TO_USER_SUBJECT));
				return result;
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_TO_USER;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_TO_USER;
			}
			
			@Override
			public Class<? extends DBOMessageToUser> getDBOClass() {
				return DBOMessageToUser.class;
			}
		};
	}
	
	
	public Long getMessageId() {
		return messageId;
	}


	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}


	public String getSubject() {
		return subject;
	}


	public void setSubject(String subject) {
		this.subject = subject;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOMessageToUser, DBOMessageToUser> getTranslator() {
		return new MigratableTableTranslation<DBOMessageToUser, DBOMessageToUser>() {
			@Override
			public DBOMessageToUser createDatabaseObjectFromBackup(DBOMessageToUser backup) {
				return backup;
			}
			
			@Override
			public DBOMessageToUser createBackupFromDatabaseObject(DBOMessageToUser dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOMessageToUser> getBackupClass() {
		return DBOMessageToUser.class;
	}
	
	@Override
	public Class<? extends DBOMessageToUser> getDatabaseObjectClass() {
		return DBOMessageToUser.class;
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
				+ ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
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
		DBOMessageToUser other = (DBOMessageToUser) obj;
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
		return true;
	}


	@Override
	public String toString() {
		return "DBOMessageToUser [messageId=" + messageId + ", subject="
				+ subject + "]";
	}

}
