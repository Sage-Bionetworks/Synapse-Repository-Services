package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHANGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SENT_MESSAGES_TIME_STAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SENT_MESSAGES;
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
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("changeNumber", COL_CHANGES_CHANGE_NUM, true),
		new FieldColumn("timeStamp", COL_CHANGES_TIME_STAMP),
	};
	
	private Long changeNumber;
	private Timestamp timeStamp;

	@Override
	public TableMapping<DBOSentMessage> getTableMapping() {
		return new TableMapping<DBOSentMessage>() {

			@Override
			public DBOSentMessage mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOSentMessage change = new DBOSentMessage();
				change.setChangeNumber(rs.getLong(COL_SENT_MESSAGES_CHANGE_NUM));
				change.setTimeStamp(rs.getTimestamp(COL_SENT_MESSAGES_TIME_STAMP));
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_SENT_MESSAGES;
			}

			@Override
			public String getDDLFileName() {
				return DDL_SENT_MESSAGES;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSentMessage> getDBOClass() {
				return DBOSentMessage.class;
			}
		};
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeNumber == null) ? 0 : changeNumber.hashCode());
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
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}

}
