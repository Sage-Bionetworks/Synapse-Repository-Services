package org.sagebionetworks.bridge.model.dbo.persistence;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * an anonymized participant in a study
 */
@Table(name = SqlConstants.TABLE_PARTICIPANT)
public class DBOParticipant implements MigratableDatabaseObject<DBOParticipant, DBOParticipant> {

	@Field(name = SqlConstants.COL_PARTICIPANT_ID, backupId = true, primary = true, nullable = false)
	private Long participantId;

	private static TableMapping<DBOParticipant> tableMapping = AutoTableMapping.create(DBOParticipant.class);

	@Override
	public TableMapping<DBOParticipant> getTableMapping() {
		return tableMapping;
	}

	public Long getParticipantId() {
		return participantId;
	}

	public void setParticipantId(Long participantId) {
		this.participantId = participantId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((participantId == null) ? 0 : participantId.hashCode());
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
		DBOParticipant other = (DBOParticipant) obj;
		if (participantId == null) {
			if (other.participantId != null)
				return false;
		} else if (!participantId.equals(other.participantId))
			return false;
		return true;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.BRIDGE_PARTICIPANT;
	}

	@Override
	public MigratableTableTranslation<DBOParticipant, DBOParticipant> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOParticipant, DBOParticipant>() {

			@Override
			public DBOParticipant createDatabaseObjectFromBackup(DBOParticipant backup) {
				return backup;
			}

			@Override
			public DBOParticipant createBackupFromDatabaseObject(DBOParticipant dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOParticipant> getBackupClass() {
		return DBOParticipant.class;
	}

	@Override
	public Class<? extends DBOParticipant> getDatabaseObjectClass() {
		return DBOParticipant.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
