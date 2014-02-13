package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COMMUNITY_TEAM_COMMUNITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COMMUNITY_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COMMUNITY_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;

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
 * Mapping between communities and teams
 */
@Table(name = TABLE_COMMUNITY_TEAM)
public class DBOCommunityTeam implements MigratableDatabaseObject<DBOCommunityTeam, DBOCommunityTeam> {

	public static final String TEAM_ID_FIELDNAME = "teamId";

	@Field(name = COL_COMMUNITY_TEAM_COMMUNITY_ID, nullable = false)
	@ForeignKey(table = TABLE_NODE, field = COL_NODE_ID)
	private Long communityId;

	@Field(name = COL_COMMUNITY_TEAM_TEAM_ID, primary = true, backupId = true, nullable = false)
	@ForeignKey(table = TABLE_TEAM, field = COL_TEAM_ID, cascadeDelete = true)
	private Long teamId;

	private static TableMapping<DBOCommunityTeam> tableMapping = AutoTableMapping.create(DBOCommunityTeam.class);

	@Override
	public TableMapping<DBOCommunityTeam> getTableMapping() {
		return tableMapping;
	}

	public Long getCommunityId() {
		return communityId;
	}

	public void setCommunityId(Long communityId) {
		this.communityId = communityId;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((communityId == null) ? 0 : communityId.hashCode());
		result = prime * result + ((teamId == null) ? 0 : teamId.hashCode());
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
		DBOCommunityTeam other = (DBOCommunityTeam) obj;
		if (communityId == null) {
			if (other.communityId != null)
				return false;
		} else if (!communityId.equals(other.communityId))
			return false;
		if (teamId == null) {
			if (other.teamId != null)
				return false;
		} else if (!teamId.equals(other.teamId))
			return false;
		return true;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.COMMUNITY_TEAM;
	}

	@Override
	public MigratableTableTranslation<DBOCommunityTeam, DBOCommunityTeam> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOCommunityTeam, DBOCommunityTeam>() {

			@Override
			public DBOCommunityTeam createDatabaseObjectFromBackup(DBOCommunityTeam backup) {
				return backup;
			}

			@Override
			public DBOCommunityTeam createBackupFromDatabaseObject(DBOCommunityTeam dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOCommunityTeam> getBackupClass() {
		return DBOCommunityTeam.class;
	}

	@Override
	public Class<? extends DBOCommunityTeam> getDatabaseObjectClass() {
		return DBOCommunityTeam.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
