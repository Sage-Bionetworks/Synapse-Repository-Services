package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SENT_MESSAGES;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Record of messages sent.
 * @author John
 *
 */
@Table(name = TABLE_SENT_MESSAGES, constraints={"CONSTRAINT `SENT_MESS_CH_NUM_FK` FOREIGN KEY (`"+COL_SENT_MESSAGES_OBJECT_ID+"`,`"+COL_SENT_MESSAGES_OBJECT_TYPE+"`) REFERENCES `"+TABLE_CHANGES+"` (`"+COL_CHANGES_OBJECT_ID+"`,`"+COL_CHANGES_OBJECT_TYPE+"`) ON DELETE CASCADE"
,"UNIQUE KEY `SENT_UNIQUE_CHANG_NUM` (`"+COL_CHANGES_CHANGE_NUM+"`)"})
public class DBOSentMessage implements DatabaseObject<DBOSentMessage> {
	
	private static TableMapping<DBOSentMessage> tableMapping = AutoTableMapping.create(DBOSentMessage.class);
	
	@Field(name = COL_SENT_MESSAGES_CHANGE_NUM, nullable = true, primary=false, backupId = true)
	private Long changeNumber;
	
	@Field(name = COL_SENT_MESSAGES_TIME_STAMP, sql="DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private Timestamp timeStamp;
	
	@Field(name = COL_SENT_MESSAGES_OBJECT_ID,nullable = false, primary=true)
    private Long objectId;
	
	@Field(name = COL_SENT_MESSAGES_OBJECT_TYPE,nullable = false, primary=true)
    private ObjectType objectType;

	@Override
	public TableMapping<DBOSentMessage> getTableMapping() {
		return tableMapping;
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

	public ObjectType getObjectType() {
		return objectType;
	}

	public void setObjectType(ObjectType objectType) {
		this.objectType = objectType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
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
		if (objectType != other.objectType)
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}

}
