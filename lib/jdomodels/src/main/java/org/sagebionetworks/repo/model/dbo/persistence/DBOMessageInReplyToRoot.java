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
 * Holds the flattened links between a message and the message it replies to
 */
public class DBOMessageInReplyToRoot implements MigratableDatabaseObject<DBOMessageInReplyToRoot, DBOMessageInReplyToRoot> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("rootMessageId", SqlConstants.COL_MESSAGE_ROOT_MESSAGE_ID, true).withIsBackupId(true), 
		new FieldColumn("childMessageId", SqlConstants.COL_MESSAGE_ROOT_CHILD_ID)
	};
	
	private Long rootMessageId;
	private Long childMessageId;
	
	@Override
	public TableMapping<DBOMessageInReplyToRoot> getTableMapping() {
		return new TableMapping<DBOMessageInReplyToRoot>() {

			@Override
			public DBOMessageInReplyToRoot mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOMessageInReplyToRoot dbo = new DBOMessageInReplyToRoot();
				dbo.setRootMessageId(rs.getLong(SqlConstants.COL_MESSAGE_ROOT_MESSAGE_ID));
				dbo.setChildMessageId(rs.getLong(SqlConstants.COL_MESSAGE_ROOT_CHILD_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_MESSAGE_ROOT;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_MESSAGE_ROOT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMessageInReplyToRoot> getDBOClass() {
				return DBOMessageInReplyToRoot.class;
			}
		};
	}

	public Long getRootMessageId() {
		return rootMessageId;
	}

	public void setRootMessageId(Long rootMessageId) {
		this.rootMessageId = rootMessageId;
	}

	public Long getChildMessageId() {
		return childMessageId;
	}

	public void setChildMessageId(Long childMessageId) {
		this.childMessageId = childMessageId;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_ROOT;
	}


	@Override
	public MigratableTableTranslation<DBOMessageInReplyToRoot, DBOMessageInReplyToRoot> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOMessageInReplyToRoot, DBOMessageInReplyToRoot>(){

			@Override
			public DBOMessageInReplyToRoot createDatabaseObjectFromBackup(
					DBOMessageInReplyToRoot backup) {
				return backup;
			}

			@Override
			public DBOMessageInReplyToRoot createBackupFromDatabaseObject(DBOMessageInReplyToRoot dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBOMessageInReplyToRoot> getBackupClass() {
		return DBOMessageInReplyToRoot.class;
	}


	@Override
	public Class<? extends DBOMessageInReplyToRoot> getDatabaseObjectClass() {
		return DBOMessageInReplyToRoot.class;
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
				+ ((childMessageId == null) ? 0 : childMessageId.hashCode());
		result = prime * result
				+ ((rootMessageId == null) ? 0 : rootMessageId.hashCode());
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
		DBOMessageInReplyToRoot other = (DBOMessageInReplyToRoot) obj;
		if (childMessageId == null) {
			if (other.childMessageId != null)
				return false;
		} else if (!childMessageId.equals(other.childMessageId))
			return false;
		if (rootMessageId == null) {
			if (other.rootMessageId != null)
				return false;
		} else if (!rootMessageId.equals(other.rootMessageId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMessageInReplyToRoot [rootMessageId=" + rootMessageId
				+ ", childMessageId=" + childMessageId + "]";
	}

}
