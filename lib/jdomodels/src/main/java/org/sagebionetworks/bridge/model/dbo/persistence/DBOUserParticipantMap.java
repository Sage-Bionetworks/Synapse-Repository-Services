package org.sagebionetworks.bridge.model.dbo.persistence;

import java.util.Arrays;
import java.util.List;

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
 * an anonymized participant in a study
 */
@Table(name = SqlConstants.TABLE_USER_PARTICIPANT_MAP)
public class DBOUserParticipantMap implements MigratableDatabaseObject<DBOUserParticipantMap, DBOUserParticipantMap> {

	@Field(name = SqlConstants.COL_USER_PARTICIPANT_MAP_USER_ID, backupId = true, primary = true, nullable = false)
	@ForeignKey(table = SqlConstants.TABLE_USER_GROUP, field = SqlConstants.COL_USER_GROUP_ID, cascadeDelete = true)
	private Long userId;

	@Field(name = SqlConstants.COL_USER_PARTICIPANT_MAP_MAP, blob = "mediumblob", nullable = false)
	private byte[] mapBlob;

	private static TableMapping<DBOUserParticipantMap> tableMapping = AutoTableMapping.create(DBOUserParticipantMap.class);

	@Override
	public TableMapping<DBOUserParticipantMap> getTableMapping() {
		return tableMapping;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public byte[] getMapBlob() {
		return mapBlob;
	}

	public void setMapBlob(byte[] mapBlob) {
		this.mapBlob = mapBlob;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(mapBlob);
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
		DBOUserParticipantMap other = (DBOUserParticipantMap) obj;
		if (!Arrays.equals(mapBlob, other.mapBlob))
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
		return "DBOUserParticipantMap [userId=" + userId + ", mapBlob=" + Arrays.toString(mapBlob) + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.BRIDGE_USER_PARTICIPANT_MAP;
	}

	@Override
	public MigratableTableTranslation<DBOUserParticipantMap, DBOUserParticipantMap> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOUserParticipantMap, DBOUserParticipantMap>() {

			@Override
			public DBOUserParticipantMap createDatabaseObjectFromBackup(DBOUserParticipantMap backup) {
				return backup;
			}

			@Override
			public DBOUserParticipantMap createBackupFromDatabaseObject(DBOUserParticipantMap dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOUserParticipantMap> getBackupClass() {
		return DBOUserParticipantMap.class;
	}

	@Override
	public Class<? extends DBOUserParticipantMap> getDatabaseObjectClass() {
		return DBOUserParticipantMap.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
