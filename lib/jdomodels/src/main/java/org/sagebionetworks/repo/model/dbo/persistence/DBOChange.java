package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHANGES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object used to keep track of all changes that occurred in the repository.
 * 
 * @author jmhill
 *
 */
public class DBOChange implements MigratableDatabaseObject<DBOChange, DBOChange>  {

	public static final long DEFAULT_NULL_VERSION = -1L;
	
	private static final MigratableTableTranslation<DBOChange, DBOChange> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("changeNumber", COL_CHANGES_CHANGE_NUM).withIsBackupId(true),
		new FieldColumn("timeStamp", COL_CHANGES_TIME_STAMP),
		new FieldColumn("objectId", COL_CHANGES_OBJECT_ID, true),
		new FieldColumn("objectVersion", COL_CHANGES_OBJECT_VERSION, true),
		new FieldColumn("objectType", COL_CHANGES_OBJECT_TYPE, true),
		new FieldColumn("changeType", COL_CHANGES_CHANGE_TYPE),
		new FieldColumn("userId", COL_CHANGES_USER_ID),
	};

	private Long changeNumber;
	private Timestamp timeStamp;
	private Long objectId;
	private Long objectVersion;
	private String objectType;
	private String changeType;
	private Long userId;

	/**
	 * The timestamp when this change was made
	 * @return
	 */
	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	/**
	 * The timestamp when this change was made
	 * @param timeStamp
	 */
	public void setTimeStamp(Timestamp timeStamp) {
		this.timeStamp = timeStamp;
	}


	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	/**
	 * The unique change number.  This is also the primary key for this table.
	 * @param changeNumber
	 */
	public void setChangeNumber(Long changeNumber) {
		this.changeNumber = changeNumber;
	}

	/**
	 * The unique change number.  This is also the primary key for this table.
	 * @return
	 */
	public Long getChangeNumber() {
		return changeNumber;
	}

	/**
	 * The Object's ID
	 * @return
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * The Object's ID
	 * @param objectId
	 */
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getObjectVersion() {
		return objectVersion;
	}

	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((objectVersion == null) ? 0 : objectVersion.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		DBOChange other = (DBOChange) obj;
		if (changeNumber == null) {
			if (other.changeNumber != null)
				return false;
		} else if (!changeNumber.equals(other.changeNumber))
			return false;
		if (changeType == null) {
			if (other.changeType != null)
				return false;
		} else if (!changeType.equals(other.changeType))
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
		if (objectVersion == null) {
			if (other.objectVersion != null)
				return false;
		} else if (!objectVersion.equals(other.objectVersion))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOChange [changeNumber=" + changeNumber + ", timeStamp=" + timeStamp + ", objectId=" + objectId
				+ ", objectVersion=" + objectVersion + ", objectType=" + objectType
				+ ", changeType=" + changeType + ", userId=" + userId + "]";
	}

	@Override
	public TableMapping<DBOChange> getTableMapping() {
		return new TableMapping<DBOChange>(){

			@Override
			public DBOChange mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOChange dbo = new DBOChange();
				dbo.setChangeNumber(rs.getLong(COL_CHANGES_CHANGE_NUM));
				dbo.setTimeStamp(rs.getTimestamp(COL_CHANGES_TIME_STAMP));
				dbo.setObjectId(rs.getLong(COL_CHANGES_OBJECT_ID));
				dbo.setObjectVersion(rs.getLong(COL_CHANGES_OBJECT_VERSION));
				dbo.setObjectType(rs.getString(COL_CHANGES_OBJECT_TYPE));
				Long userId = rs.getLong(COL_CHANGES_USER_ID);
				if (!rs.wasNull()) {
					dbo.setUserId(userId);
				}
				dbo.setChangeType(rs.getString(COL_CHANGES_CHANGE_TYPE));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_CHANGES;
			}

			@Override
			public String getDDLFileName() {
				return DDL_CHANGES;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOChange> getDBOClass() {
				return DBOChange.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.CHANGE;
	}

	@Override
	public MigratableTableTranslation<DBOChange, DBOChange> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOChange> getBackupClass() {
		return DBOChange.class;
	}

	@Override
	public Class<? extends DBOChange> getDatabaseObjectClass() {
		return DBOChange.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

}
