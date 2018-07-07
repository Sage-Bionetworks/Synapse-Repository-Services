package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * The DBO object for the status of the received message, with respect to each recipient.
 */
public class DBOMessageStatus implements MigratableDatabaseObject<DBOMessageStatus, DBOMessageStatus> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("messageId", SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID, true).withIsBackupId(true),
		new FieldColumn("recipientId", SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID, true),
		new FieldColumn("status", SqlConstants.COL_MESSAGE_STATUS)
	};
	
	private Long messageId;
	private Long recipientId;
	private MessageStatusType status;


	@Override
	public TableMapping<DBOMessageStatus> getTableMapping() {
		return new TableMapping<DBOMessageStatus>() {
			
			@Override
			public DBOMessageStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMessageStatus result = new DBOMessageStatus();
				result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_STATUS_MESSAGE_ID));
				result.setRecipientId(rs.getLong(SqlConstants.COL_MESSAGE_STATUS_RECIPIENT_ID));
				result.setStatus(MessageStatusType.valueOf(rs.getString(SqlConstants.COL_MESSAGE_STATUS)));
				return result;
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_STATUS;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_STATUS;
			}
			
			@Override
			public Class<? extends DBOMessageStatus> getDBOClass() {
				return DBOMessageStatus.class;
			}
		};
	}
	

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getRecipientId() {
		return recipientId;
	}


	public void setRecipientId(Long recipientId) {
		this.recipientId = recipientId;
	}


	public String getStatus() {
		return status.name();
	}


	public void setStatus(MessageStatusType status) {
		this.status = status;
	}

	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOMessageStatus, DBOMessageStatus> getTranslator() {
		return new BasicMigratableTableTranslation<DBOMessageStatus>();
	}

	@Override
	public Class<? extends DBOMessageStatus> getBackupClass() {
		return DBOMessageStatus.class;
	}
	
	@Override
	public Class<? extends DBOMessageStatus> getDatabaseObjectClass() {
		return DBOMessageStatus.class;
	}
	
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result
				+ ((recipientId == null) ? 0 : recipientId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		DBOMessageStatus other = (DBOMessageStatus) obj;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		if (recipientId == null) {
			if (other.recipientId != null)
				return false;
		} else if (!recipientId.equals(other.recipientId))
			return false;
		if (status != other.status)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOMessageStatus [messageId=" + messageId + ", recipientId="
				+ recipientId + ", status=" + status + "]";
	}

}
