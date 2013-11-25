package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between communities and teams
 */
public class DBOCommunityTeam implements MigratableDatabaseObject<DBOCommunityTeam, DBOCommunityTeam> {
	private Long communityId;
	private Long teamId;

	public static final String TEAM_ID_FIELDNAME = "teamId";
	public static final String COMMUNITY_ID_FIELDNAME = "communityId";
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(COMMUNITY_ID_FIELDNAME, COL_COMMUNITY_TEAM_COMMUNITY_ID, false),
			new FieldColumn(TEAM_ID_FIELDNAME, COL_COMMUNITY_TEAM_TEAM_ID, true).withIsBackupId(true)
	};

	@Override
	public TableMapping<DBOCommunityTeam> getTableMapping() {
		return new TableMapping<DBOCommunityTeam>() {

			@Override
			public DBOCommunityTeam mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOCommunityTeam change = new DBOCommunityTeam();
				change.setCommunityId(rs.getLong(COL_COMMUNITY_TEAM_COMMUNITY_ID));
				change.setTeamId(rs.getLong(COL_COMMUNITY_TEAM_TEAM_ID));
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_COMMUNITY_TEAM;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_COMMUNITY_TEAM;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOCommunityTeam> getDBOClass() {
				return DBOCommunityTeam.class;
			}
		};
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
		return new MigratableTableTranslation<DBOCommunityTeam, DBOCommunityTeam>(){

			@Override
			public DBOCommunityTeam createDatabaseObjectFromBackup(
					DBOCommunityTeam backup) {
				return backup;
			}

			@Override
			public DBOCommunityTeam createBackupFromDatabaseObject(DBOCommunityTeam dbo) {
				return dbo;
			}};
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
