package org.sagebionetworks.repo.model.message;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BROADCAST_MESSAGE_CHANGE_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BROADCAST_MESSAGE_SENT_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_BROADCAST_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BROADCAST_MESSAGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * This table tacks change message events that have been broadcast to users via email.
 *
 */
public class DBOBroadcastMessage implements MigratableDatabaseObject<DBOBroadcastMessage, DBOBroadcastMessage> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("changeNumber", COL_BROADCAST_MESSAGE_CHANGE_NUMBER, true).withIsBackupId(true),
		new FieldColumn("sentOn", COL_BROADCAST_MESSAGE_SENT_ON),
	};
	
	private Long changeNumber;
	private Long sentOn;

	public Long getChangeNumber() {
		return changeNumber;
	}


	public void setChangeNumber(Long changeNumber) {
		this.changeNumber = changeNumber;
	}

	public Long getSentOn() {
		return sentOn;
	}


	public void setSentOn(Long sentOn) {
		this.sentOn = sentOn;
	}


	@Override
	public TableMapping<DBOBroadcastMessage> getTableMapping() {

		return new TableMapping<DBOBroadcastMessage>() {

			@Override
			public DBOBroadcastMessage mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOBroadcastMessage dbo = new DBOBroadcastMessage();
				dbo.setChangeNumber(rs.getLong(COL_BROADCAST_MESSAGE_CHANGE_NUMBER));
				dbo.setSentOn(rs.getLong(COL_BROADCAST_MESSAGE_SENT_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_BROADCAST_MESSAGE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_BROADCAST_MESSAGE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOBroadcastMessage> getDBOClass() {
				return DBOBroadcastMessage.class;
			}
		};
	}
	

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.BROADCAST_MESSAGE;
	}

	@Override
	public MigratableTableTranslation<DBOBroadcastMessage, DBOBroadcastMessage> getTranslator() {
		return new BasicMigratableTableTranslation<DBOBroadcastMessage>();
	}

	@Override
	public Class<? extends DBOBroadcastMessage> getBackupClass() {
		return DBOBroadcastMessage.class;
	}

	@Override
	public Class<? extends DBOBroadcastMessage> getDatabaseObjectClass() {
		return DBOBroadcastMessage.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result + ((sentOn == null) ? 0 : sentOn.hashCode());
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
		DBOBroadcastMessage other = (DBOBroadcastMessage) obj;
		if (changeNumber == null) {
			if (other.changeNumber != null)
				return false;
		} else if (!changeNumber.equals(other.changeNumber))
			return false;
		if (sentOn == null) {
			if (other.sentOn != null)
				return false;
		} else if (!sentOn.equals(other.sentOn))
			return false;
		return true;
	}

}
