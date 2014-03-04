package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_QUEUE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROCESSED_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PROCESSED_MESSAGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROCESSED_MESSAGES;

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
public class DBOProcessedMessage implements DatabaseObject<DBOProcessedMessage> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("changeNumber", COL_PROCESSED_MESSAGES_CHANGE_NUM, true),
		new FieldColumn("timeStamp", COL_PROCESSED_MESSAGES_TIME_STAMP),
		new FieldColumn("processedBy", COL_PROCESSED_MESSAGES_QUEUE_NAME)
	};
	
	private Long changeNumber;
	private Timestamp timeStamp;
	private String queueName;

	@Override
	public TableMapping<DBOProcessedMessage> getTableMapping() {
		return new TableMapping<DBOProcessedMessage>() {

			@Override
			public DBOProcessedMessage mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOProcessedMessage change = new DBOProcessedMessage();
				change.setChangeNumber(rs.getLong(COL_PROCESSED_MESSAGES_CHANGE_NUM));
				change.setTimeStamp(rs.getTimestamp(COL_PROCESSED_MESSAGES_TIME_STAMP));
				change.setQueueName(rs.getString(COL_PROCESSED_MESSAGES_QUEUE_NAME));
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_PROCESSED_MESSAGES;
			}

			@Override
			public String getDDLFileName() {
				return DDL_PROCESSED_MESSAGES;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOProcessedMessage> getDBOClass() {
				return DBOProcessedMessage.class;
			}
		};
	}

	public Long getChangeNumber() {
		return changeNumber;
	}

	public void setChangeNumber(Long changeNumber) {
		if (changeNumber == null) {
			throw new NullPointerException("changeNumber cannot be null");
		}
		this.changeNumber = changeNumber;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		if (timeStamp == null) {
			throw new NullPointerException("timeStamp cannot be null");
		}
		this.timeStamp = timeStamp;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		if (queueName == null) {
			throw new NullPointerException("queueName cannot be null");
		}
		this.queueName = queueName;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
		result = prime * result
				+ ((queueName == null) ? 0 : queueName.hashCode());
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
		DBOProcessedMessage other = (DBOProcessedMessage) obj;
		if (changeNumber == null) {
			if (other.changeNumber != null)
				return false;
		} else if (!changeNumber.equals(other.changeNumber))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		return true;
	}

}
