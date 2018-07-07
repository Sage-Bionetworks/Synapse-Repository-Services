package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PARTICIPANT_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;

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
@Table(name = TABLE_CHALLENGE, constraints = { 
		"unique key UNIQUE_CHALL_PID ("+ COL_CHALLENGE_PROJECT_ID +")" })
public class DBOChallenge implements MigratableDatabaseObject<DBOChallenge, DBOChallenge> {
	@Field(name = COL_CHALLENGE_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = COL_CHALLENGE_ETAG, backupId = false, primary = false, nullable = false, etag=true)
	private String etag;
	
	// NOTE:  This is a FK to the TEAM table, not the USER_GROUP table, ensuring that the
	// principal registered is a Team
	@Field(name = COL_CHALLENGE_PARTICIPANT_TEAM_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_TEAM, field = COL_TEAM_ID, cascadeDelete = false)
	private Long participantTeamId;
	
	@Field(name = COL_CHALLENGE_PROJECT_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_NODE, field = COL_NODE_ID, cascadeDelete = true)
	private Long projectId;
	
	@Field(name=COL_CHALLENGE_SERIALIZED_ENTITY, blob = "mediumblob", backupId = false, primary = false, nullable = false)
	private byte[] serializedEntity;
	
	private static TableMapping<DBOChallenge> tableMapping = AutoTableMapping.create(DBOChallenge.class);

	@Override
	public TableMapping<DBOChallenge> getTableMapping() {
		return tableMapping;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.CHALLENGE;
	}


	@Override
	public MigratableTableTranslation<DBOChallenge, DBOChallenge> getTranslator() {
		// We do not currently have a backup for this object.
		return new BasicMigratableTableTranslation<DBOChallenge>();
	}


	@Override
	public Class<? extends DBOChallenge> getBackupClass() {
		return DBOChallenge.class;
	}


	@Override
	public Class<? extends DBOChallenge> getDatabaseObjectClass() {
		return DBOChallenge.class;
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


	public Long getParticipantTeamId() {
		return participantTeamId;
	}


	public void setParticipantTeamId(Long participantTeamId) {
		this.participantTeamId = participantTeamId;
	}


	public Long getProjectId() {
		return projectId;
	}


	public void setProjectId(Long projectId) {
		this.projectId = projectId;
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
				+ ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime
				* result
				+ ((participantTeamId == null) ? 0 : participantTeamId
						.hashCode());
		result = prime * result + Arrays.hashCode(serializedEntity);
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
		DBOChallenge other = (DBOChallenge) obj;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
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
		if (participantTeamId == null) {
			if (other.participantTeamId != null)
				return false;
		} else if (!participantTeamId.equals(other.participantTeamId))
			return false;
		if (!Arrays.equals(serializedEntity, other.serializedEntity))
			return false;
		return true;
	}

	
	
}
