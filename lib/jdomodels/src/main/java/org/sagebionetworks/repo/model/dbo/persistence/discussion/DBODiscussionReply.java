package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_IS_EDITED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_MESSAGE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_REPLY_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DISCUSSION_REPLY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_REPLY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODiscussionReply implements MigratableDatabaseObject<DBODiscussionReply, DBODiscussionReply>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_DISCUSSION_REPLY_ID, true).withIsBackupId(true),
		new FieldColumn("threadId", COL_DISCUSSION_REPLY_THREAD_ID),
		new FieldColumn("etag", COL_DISCUSSION_REPLY_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", COL_DISCUSSION_REPLY_CREATED_BY),
		new FieldColumn("messageKey", COL_DISCUSSION_REPLY_MESSAGE_KEY),
		new FieldColumn("isEdited", COL_DISCUSSION_REPLY_IS_EDITED),
		new FieldColumn("isDeleted", COL_DISCUSSION_REPLY_IS_DELETED),
		new FieldColumn("createdOn", COL_DISCUSSION_REPLY_CREATED_ON),
		new FieldColumn("modifiedOn", COL_DISCUSSION_REPLY_MODIFIED_ON)
	};

	private Long id;
	private Long threadId;
	private String etag;
	private Long createdBy;
	private String messageKey;
	private Boolean isEdited;
	private Boolean isDeleted;
	private Date createdOn;
	private Date modifiedOn;

	@Override
	public String toString() {
		return "DBODiscussionReply [id=" + id + ", threadId=" + threadId
				+ ", etag=" + etag + ", createdBy=" + createdBy
				+ ", messageKey=" + messageKey + ", isEdited=" + isEdited
				+ ", isDeleted=" + isDeleted + ", createdOn=" + createdOn
				+ ", modifiedOn=" + modifiedOn + "]";
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((isDeleted == null) ? 0 : isDeleted.hashCode());
		result = prime * result
				+ ((isEdited == null) ? 0 : isEdited.hashCode());
		result = prime * result
				+ ((messageKey == null) ? 0 : messageKey.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
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
		DBODiscussionReply other = (DBODiscussionReply) obj;
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isDeleted == null) {
			if (other.isDeleted != null)
				return false;
		} else if (!isDeleted.equals(other.isDeleted))
			return false;
		if (isEdited == null) {
			if (other.isEdited != null)
				return false;
		} else if (!isEdited.equals(other.isEdited))
			return false;
		if (messageKey == null) {
			if (other.messageKey != null)
				return false;
		} else if (!messageKey.equals(other.messageKey))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (threadId == null) {
			if (other.threadId != null)
				return false;
		} else if (!threadId.equals(other.threadId))
			return false;
		return true;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	public Boolean getIsEdited() {
		return isEdited;
	}

	public void setIsEdited(Boolean isEdited) {
		this.isEdited = isEdited;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	@Override
	public TableMapping<DBODiscussionReply> getTableMapping() {
		return new TableMapping<DBODiscussionReply>(){

			@Override
			public DBODiscussionReply mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODiscussionReply dbo = new DBODiscussionReply();
				dbo.setId(rs.getLong(COL_DISCUSSION_REPLY_ID));
				dbo.setThreadId(rs.getLong(COL_DISCUSSION_REPLY_THREAD_ID));
				dbo.setEtag(rs.getString(COL_DISCUSSION_REPLY_ETAG));
				dbo.setCreatedBy(rs.getLong(COL_DISCUSSION_REPLY_CREATED_BY));
				dbo.setMessageKey(rs.getString(COL_DISCUSSION_REPLY_MESSAGE_KEY));
				dbo.setIsEdited(rs.getBoolean(COL_DISCUSSION_REPLY_IS_EDITED));
				dbo.setIsDeleted(rs.getBoolean(COL_DISCUSSION_REPLY_IS_DELETED));
				dbo.setCreatedOn(new Date(rs.getTimestamp(COL_DISCUSSION_REPLY_CREATED_ON).getTime()));
				dbo.setModifiedOn(new Date(rs.getTimestamp(COL_DISCUSSION_REPLY_MODIFIED_ON).getTime()));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DISCUSSION_REPLY;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DISCUSSION_REPLY;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODiscussionReply> getDBOClass() {
				return DBODiscussionReply.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DISCUSSION_REPLY;
	}

	@Override
	public MigratableTableTranslation<DBODiscussionReply, DBODiscussionReply> getTranslator() {
		return new BasicMigratableTableTranslation<DBODiscussionReply>();
	}

	@Override
	public Class<? extends DBODiscussionReply> getBackupClass() {
		return DBODiscussionReply.class;
	}

	@Override
	public Class<? extends DBODiscussionReply> getDatabaseObjectClass() {
		return DBODiscussionReply.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
