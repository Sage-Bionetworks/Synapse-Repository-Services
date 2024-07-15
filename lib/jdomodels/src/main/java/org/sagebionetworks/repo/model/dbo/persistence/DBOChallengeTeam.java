package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_CHALLENGE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and nodes. Used to relate Teams to Challenges
 */
public class DBOChallengeTeam implements MigratableDatabaseObject<DBOChallengeTeam, DBOChallengeTeam> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_CHALLENGE_TEAM_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_CHALLENGE_TEAM_ETAG).withIsEtag(true),
			new FieldColumn("teamId", COL_CHALLENGE_TEAM_TEAM_ID),
			new FieldColumn("challengeId", COL_CHALLENGE_TEAM_CHALLENGE_ID),
			new FieldColumn("serializedEntity", COL_CHALLENGE_TEAM_SERIALIZED_ENTITY)};

	private Long id;
	private String etag;
	private Long teamId;
	private Long challengeId;
	private byte[] serializedEntity;

	@Override
	public TableMapping<DBOChallengeTeam> getTableMapping() {
		return new TableMapping<DBOChallengeTeam>() {
			
			@Override
			public DBOChallengeTeam mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOChallengeTeam dbo = new DBOChallengeTeam();
				dbo.setId(rs.getLong(COL_CHALLENGE_TEAM_ID));
				dbo.setEtag(rs.getString(COL_CHALLENGE_TEAM_ETAG));
				dbo.setTeamId(rs.getLong(COL_CHALLENGE_TEAM_TEAM_ID));
				dbo.setChallengeId(rs.getLong(COL_CHALLENGE_TEAM_CHALLENGE_ID));
				dbo.setSerializedEntity(rs.getBytes(COL_CHALLENGE_TEAM_SERIALIZED_ENTITY));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_CHALLENGE_TEAM;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_CHALLENGE_TEAM;
			}
			
			@Override
			public Class<? extends DBOChallengeTeam> getDBOClass() {
				return DBOChallengeTeam.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.CHALLENGE_TEAM;
	}

	@Override
	public MigratableTableTranslation<DBOChallengeTeam, DBOChallengeTeam> getTranslator() {
		// We do not currently have a backup for this object.
		return new BasicMigratableTableTranslation<DBOChallengeTeam>();
	}

	@Override
	public Class<? extends DBOChallengeTeam> getBackupClass() {
		return DBOChallengeTeam.class;
	}

	@Override
	public Class<? extends DBOChallengeTeam> getDatabaseObjectClass() {
		return DBOChallengeTeam.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public Long getChallengeId() {
		return challengeId;
	}

	public void setChallengeId(Long challengeId) {
		this.challengeId = challengeId;
	}

	public byte[] getSerializedEntity() {
		return serializedEntity;
	}

	public void setSerializedEntity(byte[] serializedEntity) {
		this.serializedEntity = serializedEntity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(serializedEntity);
		result = prime * result + Objects.hash(challengeId, etag, id, teamId);
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
		return Objects.equals(challengeId, other.challengeId) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Arrays.equals(serializedEntity, other.serializedEntity)
				&& Objects.equals(teamId, other.teamId);
	}

}
