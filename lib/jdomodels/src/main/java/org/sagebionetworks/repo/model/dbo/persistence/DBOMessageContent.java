package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Holds common fields of messages between users and comments on arbitrary objects
 */
public class DBOMessageContent implements MigratableDatabaseObject<DBOMessageContent, DBOMessageContent> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("messageId", SqlConstants.COL_MESSAGE_CONTENT_ID, true).withIsBackupId(true),
		new FieldColumn("createdBy", SqlConstants.COL_MESSAGE_CONTENT_CREATED_BY),
		new FieldColumn("fileHandleId", SqlConstants.COL_MESSAGE_CONTENT_FILE_HANDLE_ID),
		new FieldColumn("inReplyTo", SqlConstants.COL_MESSAGE_CONTENT_IN_REPLY_TO),
		new FieldColumn("createdOn", SqlConstants.COL_MESSAGE_CONTENT_CREATED_ON),
		new FieldColumn("etag", SqlConstants.COL_MESSAGE_CONTENT_ETAG)
	};
	
	private Long messageId;
	private Long createdBy;
	private Long fileHandleId;
	private Long inReplyTo;
	private Long createdOn;
	private String etag;

	@Override
	public TableMapping<DBOMessageContent> getTableMapping() {
		return new TableMapping<DBOMessageContent>() {
			
			@Override
			public DBOMessageContent mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMessageContent result = new DBOMessageContent();
				result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_CONTENT_ID));
				result.setCreatedBy(rs.getLong(SqlConstants.COL_MESSAGE_CONTENT_CREATED_BY));
				result.setFileHandleId(rs.getLong(SqlConstants.COL_MESSAGE_CONTENT_FILE_HANDLE_ID));
				String replyTo = rs.getString(SqlConstants.COL_MESSAGE_CONTENT_IN_REPLY_TO);
				if (replyTo != null) {
					result.setInReplyTo(Long.parseLong(replyTo));
				}
				result.setCreatedOn(rs.getLong(SqlConstants.COL_MESSAGE_CONTENT_CREATED_ON));
				result.setEtag(rs.getString(SqlConstants.COL_MESSAGE_CONTENT_ETAG));
				return result;
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_CONTENT;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_CONTENT;
			}
			
			@Override
			public Class<? extends DBOMessageContent> getDBOClass() {
				return DBOMessageContent.class;
			}
		};
	}


	public Long getMessageId() {
		return messageId;
	}


	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}


	public Long getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}


	public Long getFileHandleId() {
		return fileHandleId;
	}


	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}


	public Long getInReplyTo() {
		return inReplyTo;
	}


	public void setInReplyTo(Long inReplyTo) {
		this.inReplyTo = inReplyTo;
	}


	public Long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}


	public String getEtag() {
		return etag;
	}


	public void setEtag(String etag) {
		this.etag = etag;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE;
	}

	@Override
	public MigratableTableTranslation<DBOMessageContent, DBOMessageContent> getTranslator() {
		return new MigratableTableTranslation<DBOMessageContent, DBOMessageContent>() {
			@Override
			public DBOMessageContent createDatabaseObjectFromBackup(DBOMessageContent backup) {
				return backup;
			}
			
			@Override
			public DBOMessageContent createBackupFromDatabaseObject(DBOMessageContent dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOMessageContent> getBackupClass() {
		return DBOMessageContent.class;
	}
	
	@Override
	public Class<? extends DBOMessageContent> getDatabaseObjectClass() {
		return DBOMessageContent.class;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		List<MigratableDatabaseObject> secondaries = new ArrayList<MigratableDatabaseObject>();
		secondaries.add(new DBOMessageStatus());
		secondaries.add(new DBOMessageThread());
		return secondaries;
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((inReplyTo == null) ? 0 : inReplyTo.hashCode());
		result = prime * result
				+ ((messageId == null) ? 0 : messageId.hashCode());
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
		DBOMessageContent other = (DBOMessageContent) obj;
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
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (inReplyTo == null) {
			if (other.inReplyTo != null)
				return false;
		} else if (!inReplyTo.equals(other.inReplyTo))
			return false;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMessageContent [messageId=" + messageId + ", createdBy="
				+ createdBy + ", fileHandleId=" + fileHandleId + ", inReplyTo="
				+ inReplyTo + ", createdOn=" + createdOn + ", etag=" + etag
				+ "]";
	}

}
