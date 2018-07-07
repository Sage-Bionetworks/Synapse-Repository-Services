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
 * Contains information about comments posted to arbitrary objects
 */
public class DBOComment implements MigratableDatabaseObject<DBOComment, DBOComment> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("messageId", SqlConstants.COL_COMMENT_MESSAGE_ID, true).withIsBackupId(true), 
		new FieldColumn("objectType", SqlConstants.COL_COMMENT_OBJECT_TYPE), 
		new FieldColumn("objectId", SqlConstants.COL_COMMENT_OBJECT_ID)
	};
	
	private Long messageId;
	private String objectType;
	private Long objectId;
	
	@Override
	public TableMapping<DBOComment> getTableMapping() {
		return new TableMapping<DBOComment>() {

			@Override
			public DBOComment mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOComment dbo = new DBOComment();
				dbo.setMessageId(rs.getLong(SqlConstants.COL_COMMENT_MESSAGE_ID));
				dbo.setObjectType(rs.getString(SqlConstants.COL_COMMENT_OBJECT_TYPE));
				dbo.setObjectId(rs.getLong(SqlConstants.COL_COMMENT_OBJECT_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_COMMENT;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_COMMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOComment> getDBOClass() {
				return DBOComment.class;
			}
		};
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.COMMENT;
	}


	@Override
	public MigratableTableTranslation<DBOComment, DBOComment> getTranslator() {
		return new BasicMigratableTableTranslation<DBOComment>();
	}


	@Override
	public Class<? extends DBOComment> getBackupClass() {
		return DBOComment.class;
	}


	@Override
	public Class<? extends DBOComment> getDatabaseObjectClass() {
		return DBOComment.class;
	}


	@SuppressWarnings("rawtypes")
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
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((objectType == null) ? 0 : objectType.hashCode());
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
		DBOComment other = (DBOComment) obj;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
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
		return true;
	}

	@Override
	public String toString() {
		return "DBOComment [messageId=" + messageId + ", objectType="
				+ objectType + ", objectId=" + objectId + "]";
	}

}
