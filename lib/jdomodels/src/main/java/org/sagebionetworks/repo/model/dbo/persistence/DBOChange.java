package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHANGES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object used to keep track of all changes that occurred in the repository.
 * 
 * @author jmhill
 *
 */
public class DBOChange implements MigratableDatabaseObject<DBOChange, DBOChange>  {
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("changeNumber", COL_CHANGES_CHANGE_NUM, true).withIsBackupId(true),
		new FieldColumn("timeStamp", COL_CHANGES_TIME_STAMP),
		new FieldColumn("objectId", COL_CHANGES_OBJECT_ID),
		new FieldColumn("parentId", COL_CHANGES_PARENT_ID),
		new FieldColumn("objectType", COL_CHANGES_OBJECT_TYPE),
		new FieldColumn("objectEtag", COL_CHANGES_OBJECT_ETAG),
		new FieldColumn("changeType", COL_CHANGES_CHANGE_TYPE),
	};
	
	private Long changeNumber;
	private Timestamp timeStamp;
    private Long objectId;
    private Long parentId;
    private ObjectType objectType;
    private String objectEtag;
    private ChangeType changeType;

	@Override
	public TableMapping<DBOChange> getTableMapping() {
		return new TableMapping<DBOChange>() {

			@Override
			public DBOChange mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOChange change = new DBOChange();
				change.setChangeNumber(rs.getLong(COL_CHANGES_CHANGE_NUM));
				change.setTimeStamp(rs.getTimestamp(COL_CHANGES_TIME_STAMP));
				change.setObjectId(rs.getLong(COL_CHANGES_OBJECT_ID));
				long parentId = rs.getLong(COL_CHANGES_PARENT_ID);
				if (!rs.wasNull()) {
					change.setParentId(parentId);
				}
				change.setObjectType(ObjectType.valueOf(rs.getString(COL_CHANGES_OBJECT_TYPE)));
				change.setObjectEtag(rs.getString(COL_CHANGES_OBJECT_ETAG));
				change.setChangeType(ChangeType.valueOf(rs.getString(COL_CHANGES_CHANGE_TYPE)));
				return change;
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


	/**
	 * The type of change.
	 * @return
	 */
	public String getChangeType() {
		return changeType.name();
	}
	
	/**
	 * The type of change.
	 * @return
	 */
	public ChangeType getChangeTypeEnum() {
		return changeType;
	}


	/**
	 * The type of change
	 * @param changeType
	 */
	public void setChangeType(ChangeType changeType) {
		this.changeType = changeType;
	}



	/**
	 * The object's E-tag
	 * @return
	 */
	public String getObjectEtag() {
		return objectEtag;
	}


	/**
	 * The object's E-tag
	 * @param objectEtag
	 */
	public void setObjectEtag(String objectEtag) {
		this.objectEtag = objectEtag;
	}


	/**
	 * The object's type
	 * @return
	 */
	public ObjectType getObjectTypeEnum() {
		return objectType;
	}
	
	/**
	 * The object's type
	 * @return
	 */
	public String getObjectType() {
		return objectType.name();
	}


	/**
	 * The object's type
	 * @param objectType
	 */
	public void setObjectType(ObjectType objectType) {
		this.objectType = objectType;
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

	/**
	 * @return the parentId
	 */
	public Long getParentId() {
		return parentId;
	}


	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result
				+ ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result
				+ ((objectEtag == null) ? 0 : objectEtag.hashCode());
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOChange other = (DBOChange) obj;
		if (changeNumber == null) {
			if (other.changeNumber != null) {
				return false;
			}
		} else if (!changeNumber.equals(other.changeNumber)) {
			return false;
		}
		if (changeType != other.changeType) {
			return false;
		}
		if (objectEtag == null) {
			if (other.objectEtag != null) {
				return false;
			}
		} else if (!objectEtag.equals(other.objectEtag)) {
			return false;
		}
		if (objectId == null) {
			if (other.objectId != null) {
				return false;
			}
		} else if (!objectId.equals(other.objectId)) {
			return false;
		}
		if (objectType != other.objectType) {
			return false;
		}
		if (parentId == null) {
			if (other.parentId != null) {
				return false;
			}
		} else if (!parentId.equals(other.parentId)) {
			return false;
		}
		if (timeStamp == null) {
			if (other.timeStamp != null) {
				return false;
			}
		} else if (!timeStamp.equals(other.timeStamp)) {
			return false;
		}
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DBOChange [changeNumber=" + changeNumber + ", timeStamp="
				+ timeStamp + ", objectId=" + objectId + ", parentId="
				+ parentId + ", objectType=" + objectType + ", objectEtag="
				+ objectEtag + ", changeType=" + changeType + "]";
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.CHANGE;
	}


	@Override
	public MigratableTableTranslation<DBOChange, DBOChange> getTranslator() {
		// TODO Auto-generated method stub
		return new MigratableTableTranslation<DBOChange, DBOChange>(){

			@Override
			public DBOChange createDatabaseObjectFromBackup(DBOChange backup) {
				return backup;
			}

			@Override
			public DBOChange createBackupFromDatabaseObject(DBOChange dbo) {
				return dbo;
			}};
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
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}
