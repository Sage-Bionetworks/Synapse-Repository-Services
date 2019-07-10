package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SENT_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SENT_MESSAGES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Record of messages sent.
 * @author John
 *
 */
public class DBOSentMessage implements DatabaseObject<DBOSentMessage> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("changeNumber", COL_SENT_MESSAGES_CHANGE_NUM).withIsBackupId(true),
			new FieldColumn("timeStamp", COL_SENT_MESSAGES_TIME_STAMP),
			new FieldColumn("objectId", COL_SENT_MESSAGES_OBJECT_ID, true),
			new FieldColumn("objectVersion", COL_SENT_MESSAGES_OBJECT_VERSION, true),
			new FieldColumn("objectType", COL_SENT_MESSAGES_OBJECT_TYPE, true),
		};
	
	private Long changeNumber;
	private Timestamp timeStamp;
    private Long objectId;
    private Long objectVersion;
    private String objectType;

	@Override
	public TableMapping<DBOSentMessage> getTableMapping() {
		return new TableMapping<DBOSentMessage>(){

			@Override
			public DBOSentMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSentMessage dbo = new DBOSentMessage();
				dbo.setChangeNumber(rs.getLong(COL_SENT_MESSAGES_CHANGE_NUM));
				dbo.setObjectId(rs.getLong(COL_SENT_MESSAGES_OBJECT_ID));
				dbo.setObjectVersion(rs.getLong(COL_SENT_MESSAGES_OBJECT_VERSION));
				dbo.setObjectType(rs.getString(COL_SENT_MESSAGES_OBJECT_TYPE));
				dbo.setTimeStamp(rs.getTimestamp(COL_SENT_MESSAGES_TIME_STAMP));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_SENT_MESSAGES;
			}

			@Override
			public String getDDLFileName() {
				return DDL_SENT_MESSAGE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSentMessage> getDBOClass() {
				return DBOSentMessage.class;
			}};
	}

	public Long getChangeNumber() {
		return changeNumber;
	}

	public void setChangeNumber(Long changeNumber) {
		this.changeNumber = changeNumber;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
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
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((objectVersion == null) ? 0 : objectVersion.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
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
		DBOSentMessage other = (DBOSentMessage) obj;
		if (changeNumber == null) {
			if (other.changeNumber != null)
				return false;
		} else if (!changeNumber.equals(other.changeNumber))
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
		return true;
	}

	@Override
	public String toString() {
		return "DBOSentMessage [changeNumber=" + changeNumber + ", timeStamp=" + timeStamp + ", objectId=" + objectId
				+ ", objectVersion=" + objectVersion + ", objectType=" + objectType + "]";
	}

}
