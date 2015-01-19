package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and nodes.  Used to relate Teams to Challenges
 */
@Table(name = TABLE_CHALLENGE_TEAM)
public class DBOChallengeTeam implements MigratableDatabaseObject<DBOChallengeTeam, DBOChallengeTeam> {
	@Field(name = COL_GROUP_NODE_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = COL_GROUP_NODE_ETAG, backupId = false, primary = false, nullable = false, etag=true)
	private String etag;
	
	@Field(name = COL_GROUP_NODE_GROUP_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long teamId;
	
	@Field(name = COL_GROUP_NODE_NODE_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_CHALLENGE, field = COL_NODE_ID, cascadeDelete = true)
	private Long challengeId;
	
	private static TableMapping<DBOChallengeTeam> tableMapping = AutoTableMapping.create(DBOChallengeTeam.class);

	@Override
	public TableMapping<DBOChallengeTeam> getTableMapping() {
		return tableMapping;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getGroupId() {
		return teamId;
	}

	public void setGroupId(Long groupId) {
		this.teamId = groupId;
	}

	public Long getNodeId() {
		return challengeId;
	}

	public void setNodeId(Long nodeId) {
		this.challengeId = nodeId;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.GROUP_NODE;
	}


	@Override
	public MigratableTableTranslation<DBOChallengeTeam, DBOChallengeTeam> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOChallengeTeam, DBOChallengeTeam>(){

			@Override
			public DBOChallengeTeam createDatabaseObjectFromBackup(
					DBOChallengeTeam backup) {
				return backup;
			}

			@Override
			public DBOChallengeTeam createBackupFromDatabaseObject(DBOChallengeTeam dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBOChallengeTeam> getBackupClass() {
		return DBOChallengeTeam.class;
	}


	@Override
	public Class<? extends DBOChallengeTeam> getDatabaseObjectClass() {
		return DBOChallengeTeam.class;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((teamId == null) ? 0 : teamId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((challengeId == null) ? 0 : challengeId.hashCode());
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
		DBOChallengeTeam other = (DBOChallengeTeam) obj;
		if (teamId == null) {
			if (other.teamId != null)
				return false;
		} else if (!teamId.equals(other.teamId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (challengeId == null) {
			if (other.challengeId != null)
				return false;
		} else if (!challengeId.equals(other.challengeId))
			return false;
		return true;
	}


}
