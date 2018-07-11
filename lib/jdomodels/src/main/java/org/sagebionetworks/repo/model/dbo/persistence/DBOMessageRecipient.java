package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Contains information about the intended recipients (principals) of a message
 */
public class DBOMessageRecipient implements MigratableDatabaseObject<DBOMessageRecipient, DBOMessageRecipient> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("messageId", SqlConstants.COL_MESSAGE_RECIPIENT_MESSAGE_ID, true).withIsBackupId(true),
		new FieldColumn("recipientId", SqlConstants.COL_MESSAGE_RECIPIENT_ID, true)
	};
	
	private Long messageId;
	private Long recipientId;


	@Override
	public TableMapping<DBOMessageRecipient> getTableMapping() {
		return new TableMapping<DBOMessageRecipient>() {
			
			@Override
			public DBOMessageRecipient mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMessageRecipient result = new DBOMessageRecipient();
				result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_RECIPIENT_MESSAGE_ID));
				result.setRecipientId(rs.getLong(SqlConstants.COL_MESSAGE_RECIPIENT_ID));
				return result;
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_RECIPIENT;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_RECIPIENT;
			}
			
			@Override
			public Class<? extends DBOMessageRecipient> getDBOClass() {
				return DBOMessageRecipient.class;
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


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_RECIPIENT;
	}

	@Override
	public MigratableTableTranslation<DBOMessageRecipient, DBOMessageRecipient> getTranslator() {
		return new BasicMigratableTableTranslation<DBOMessageRecipient>();
	}

	@Override
	public Class<? extends DBOMessageRecipient> getBackupClass() {
		return DBOMessageRecipient.class;
	}
	
	@Override
	public Class<? extends DBOMessageRecipient> getDatabaseObjectClass() {
		return DBOMessageRecipient.class;
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
		DBOMessageRecipient other = (DBOMessageRecipient) obj;
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
		return true;
	}


	@Override
	public String toString() {
		return "DBOMessageRecipient [messageId=" + messageId + ", recipientId="
				+ recipientId + "]";
	}

}
