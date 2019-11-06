package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_EDITED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_PINNED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_MESSAGE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DISCUSSION_THREAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Data Binding Object for the Thread table
 * @author kimyentruong
 *
 */
public class DBODiscussionThread  implements MigratableDatabaseObject<DBODiscussionThread, DBODiscussionThread> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_DISCUSSION_THREAD_ID, true).withIsBackupId(true),
		new FieldColumn("forumId", COL_DISCUSSION_THREAD_FORUM_ID),
		new FieldColumn("title", COL_DISCUSSION_THREAD_TITLE),
		new FieldColumn("etag", COL_DISCUSSION_THREAD_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", COL_DISCUSSION_THREAD_CREATED_BY),
		new FieldColumn("messageKey", COL_DISCUSSION_THREAD_MESSAGE_KEY),
		new FieldColumn("isEdited", COL_DISCUSSION_THREAD_IS_EDITED),
		new FieldColumn("isDeleted", COL_DISCUSSION_THREAD_IS_DELETED),
		new FieldColumn("isPinned", COL_DISCUSSION_THREAD_IS_PINNED),
		new FieldColumn("createdOn", COL_DISCUSSION_THREAD_CREATED_ON),
		new FieldColumn("modifiedOn", COL_DISCUSSION_THREAD_MODIFIED_ON)
	};

	private Long id;
	private Long forumId;
	private byte[] title;
	private String etag;
	private Long createdBy;
	private String messageKey;
	private Boolean isEdited;
	private Boolean isDeleted;
	private Boolean isPinned;
	private Date createdOn;
	private Date modifiedOn;

	@Override
	public String toString() {
		return "DBODiscussionThread [id=" + id + ", forumId=" + forumId + ", title=" + Arrays.toString(title)
				+ ", etag=" + etag + ", createdBy=" + createdBy + ", messageKey=" + messageKey + ", isEdited="
				+ isEdited + ", isDeleted=" + isDeleted + ", isPinned=" + isPinned + ", createdOn=" + createdOn
				+ ", modifiedOn=" + modifiedOn + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((forumId == null) ? 0 : forumId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isDeleted == null) ? 0 : isDeleted.hashCode());
		result = prime * result + ((isEdited == null) ? 0 : isEdited.hashCode());
		result = prime * result + ((isPinned == null) ? 0 : isPinned.hashCode());
		result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + Arrays.hashCode(title);
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
		DBODiscussionThread other = (DBODiscussionThread) obj;
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
		if (forumId == null) {
			if (other.forumId != null)
				return false;
		} else if (!forumId.equals(other.forumId))
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
		if (isPinned == null) {
			if (other.isPinned != null)
				return false;
		} else if (!isPinned.equals(other.isPinned))
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
		if (!Arrays.equals(title, other.title))
			return false;
		return true;
	}

	public Boolean getIsPinned() {
		return isPinned;
	}

	public void setIsPinned(Boolean isPinned) {
		this.isPinned = isPinned;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getForumId() {
		return forumId;
	}

	public void setForumId(Long forumId) {
		this.forumId = forumId;
	}

	public byte[] getTitle() {
		return title;
	}

	public void setTitle(byte[] title) {
		this.title = title;
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
	public TableMapping<DBODiscussionThread> getTableMapping() {
		return new TableMapping<DBODiscussionThread>() {

			@Override
			public DBODiscussionThread mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODiscussionThread dbo = new DBODiscussionThread();
				dbo.setId(rs.getLong(COL_DISCUSSION_THREAD_ID));
				dbo.setForumId(rs.getLong(COL_DISCUSSION_THREAD_FORUM_ID));
				Blob blob = rs.getBlob(COL_DISCUSSION_THREAD_TITLE);
				dbo.setTitle(blob.getBytes(1, (int) blob.length()));
				dbo.setEtag(rs.getString(COL_DISCUSSION_THREAD_ETAG));
				dbo.setCreatedBy(rs.getLong(COL_DISCUSSION_THREAD_CREATED_BY));
				dbo.setMessageKey(rs.getString(COL_DISCUSSION_THREAD_MESSAGE_KEY));
				dbo.setIsEdited(rs.getBoolean(COL_DISCUSSION_THREAD_IS_EDITED));
				dbo.setIsDeleted(rs.getBoolean(COL_DISCUSSION_THREAD_IS_DELETED));
				dbo.setIsPinned(rs.getBoolean(COL_DISCUSSION_THREAD_IS_PINNED));
				dbo.setCreatedOn(new Date(rs.getTimestamp(COL_DISCUSSION_THREAD_CREATED_ON).getTime()));
				dbo.setModifiedOn(new Date(rs.getTimestamp(COL_DISCUSSION_THREAD_MODIFIED_ON).getTime()));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DISCUSSION_THREAD;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DISCUSSION_THREAD;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODiscussionThread> getDBOClass() {
				return DBODiscussionThread.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DISCUSSION_THREAD;
	}

	@Override
	public MigratableTableTranslation<DBODiscussionThread, DBODiscussionThread> getTranslator() {
		return new MigratableTableTranslation<DBODiscussionThread, DBODiscussionThread>(){

			@Override
			public DBODiscussionThread createDatabaseObjectFromBackup(DBODiscussionThread backup) {
				if (backup.getIsPinned() == null) {
					backup.setIsPinned(false);
				}
				return backup;
			}

			@Override
			public DBODiscussionThread createBackupFromDatabaseObject(DBODiscussionThread dbo) {
				if (dbo.getIsPinned() == null) {
					dbo.setIsPinned(false);
				}
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBODiscussionThread> getBackupClass() {
		return DBODiscussionThread.class;
	}

	@Override
	public Class<? extends DBODiscussionThread> getDatabaseObjectClass() {
		return DBODiscussionThread.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBODiscussionThreadView());
		list.add(new DBODiscussionThreadEntityReference());
		return list;
	}

}
