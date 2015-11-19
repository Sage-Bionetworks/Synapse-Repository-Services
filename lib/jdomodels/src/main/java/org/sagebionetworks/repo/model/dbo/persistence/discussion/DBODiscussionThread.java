package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
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
		new FieldColumn("messageUrl", COL_DISCUSSION_THREAD_MESSAGE_URL),
		new FieldColumn("isEdited", COL_DISCUSSION_THREAD_IS_EDITED),
		new FieldColumn("isDeleted", COL_DISCUSSION_THREAD_IS_DELETED)
	};

	private Long id;
	private Long forumId;
	private byte[] title;
	private String etag;
	private Long createdBy;
	private String messageUrl;
	private Boolean isEdited;
	private Boolean isDeleted;

	@Override
	public String toString() {
		return "DBODiscussionThread [id=" + id + ", forumId=" + forumId
				+ ", title=" + Arrays.toString(title) + ", etag=" + etag
				+ ", createdBy=" + createdBy + ", messageUrl=" + messageUrl
				+ ", isEdited=" + isEdited + ", isDeleted=" + isDeleted + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((forumId == null) ? 0 : forumId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((isDeleted == null) ? 0 : isDeleted.hashCode());
		result = prime * result
				+ ((isEdited == null) ? 0 : isEdited.hashCode());
		result = prime * result
				+ ((messageUrl == null) ? 0 : messageUrl.hashCode());
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
		if (messageUrl == null) {
			if (other.messageUrl != null)
				return false;
		} else if (!messageUrl.equals(other.messageUrl))
			return false;
		if (!Arrays.equals(title, other.title))
			return false;
		return true;
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

	public String getMessageUrl() {
		return messageUrl;
	}

	public void setMessageUrl(String messageUrl) {
		this.messageUrl = messageUrl;
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
				dbo.setMessageUrl(rs.getString(COL_DISCUSSION_THREAD_MESSAGE_URL));
				dbo.setIsEdited(rs.getBoolean(COL_DISCUSSION_THREAD_IS_EDITED));
				dbo.setIsDeleted(rs.getBoolean(COL_DISCUSSION_THREAD_IS_DELETED));
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
				return backup;
			}

			@Override
			public DBODiscussionThread createBackupFromDatabaseObject(DBODiscussionThread dbo) {
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
		return null;
	}

}
