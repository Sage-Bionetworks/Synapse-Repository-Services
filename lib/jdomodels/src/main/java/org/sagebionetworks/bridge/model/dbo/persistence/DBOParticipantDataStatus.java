package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PARTICIPANT_DATA_DESCRIPTOR;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * a participant data record
 */
@Table(name = SqlConstants.TABLE_PARTICIPANT_DATA_STATUS)
public class DBOParticipantDataStatus implements MigratableDatabaseObject<DBOParticipantDataStatus, DBOParticipantDataStatus> {

	public static final String PARTICIPANT_DATA_DESCRIPTOR_ID_FIELD = "participantDataDescriptorId";

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_DESCRIPTOR_ID, backupId = true, primary = true)
	@ForeignKey(table = TABLE_PARTICIPANT_DATA_DESCRIPTOR, field = COL_PARTICIPANT_DATA_DESCRIPTOR_ID)
	private Long participantDataDescriptorId;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_STATUS_STATUS, serialized = "mediumblob", nullable = false)
	private ParticipantDataStatus status;

	private static TableMapping<DBOParticipantDataStatus> tableMapping = AutoTableMapping.create(DBOParticipantDataStatus.class);

	@Override
	public TableMapping<DBOParticipantDataStatus> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PARTICIPANT_DATA;
	}

	public Long getParticipantDataDescriptorId() {
		return participantDataDescriptorId;
	}

	public void setParticipantDataDescriptorId(Long participantDataDescriptorId) {
		this.participantDataDescriptorId = participantDataDescriptorId;
	}

	public ParticipantDataStatus getStatus() {
		return status;
	}

	public void setStatus(ParticipantDataStatus status) {
		this.status = status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((participantDataDescriptorId == null) ? 0 : participantDataDescriptorId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		DBOParticipantDataStatus other = (DBOParticipantDataStatus) obj;
		if (participantDataDescriptorId == null) {
			if (other.participantDataDescriptorId != null)
				return false;
		} else if (!participantDataDescriptorId.equals(other.participantDataDescriptorId))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOParticipantDataStatus [participantDataDescriptorId=" + participantDataDescriptorId + ", status=" + status + "]";
	}

	@Override
	public MigratableTableTranslation<DBOParticipantDataStatus, DBOParticipantDataStatus> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOParticipantDataStatus, DBOParticipantDataStatus>() {

			@Override
			public DBOParticipantDataStatus createDatabaseObjectFromBackup(DBOParticipantDataStatus backup) {
				return backup;
			}

			@Override
			public DBOParticipantDataStatus createBackupFromDatabaseObject(DBOParticipantDataStatus dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOParticipantDataStatus> getBackupClass() {
		return DBOParticipantDataStatus.class;
	}

	@Override
	public Class<? extends DBOParticipantDataStatus> getDatabaseObjectClass() {
		return DBOParticipantDataStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
