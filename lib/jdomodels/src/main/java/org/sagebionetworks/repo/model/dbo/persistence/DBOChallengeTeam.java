package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and nodes.  Used to relate Teams to Challenges
 */
@Table(name = TABLE_CHALLENGE_TEAM, constraints = { 
		"unique key UNIQUE_CT_CHALL_AND_TEAM ("+ COL_CHALLENGE_TEAM_TEAM_ID + "," + COL_CHALLENGE_TEAM_CHALLENGE_ID +")" })
public class DBOChallengeTeam implements MigratableDatabaseObject<DBOChallengeTeam, DBOChallengeTeam> {
	@Field(name = COL_CHALLENGE_TEAM_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = COL_CHALLENGE_TEAM_ETAG, backupId = false, primary = false, nullable = false, etag=true)
	private String etag;
	
	// NOTE:  This is a FK to the TEAM table, not the USER_GROUP table, ensuring that the
	// principal registered is a Team
	@Field(name = COL_CHALLENGE_TEAM_TEAM_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_TEAM, field = COL_TEAM_ID, cascadeDelete = true)
	private Long teamId;
	
	@Field(name = COL_CHALLENGE_TEAM_CHALLENGE_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_CHALLENGE, field = COL_CHALLENGE_ID, cascadeDelete = true)
	private Long challengeId;
	
	@Field(name=COL_CHALLENGE_TEAM_SERIALIZED_ENTITY, blob = "mediumblob", backupId = false, primary = false, nullable = false)
	private byte[] serializedEntity;
	
	private static TableMapping<DBOChallengeTeam> tableMapping = AutoTableMapping.create(DBOChallengeTeam.class);

	@Override
	public TableMapping<DBOChallengeTeam> getTableMapping() {
		return tableMapping;
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
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
		result = prime * result
				+ ((challengeId == null) ? 0 : challengeId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(serializedEntity);
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
		DBOChallengeTeam other = (DBOChallengeTeam) obj;
		if (challengeId == null) {
			if (other.challengeId != null)
				return false;
		} else if (!challengeId.equals(other.challengeId))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(serializedEntity, other.serializedEntity))
			return false;
		if (teamId == null) {
			if (other.teamId != null)
				return false;
		} else if (!teamId.equals(other.teamId))
			return false;
		return true;
	}


}
