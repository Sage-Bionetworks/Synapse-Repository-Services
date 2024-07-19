package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PARTICIPANT_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_CHALLENGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and nodes.  Used to relate Teams to Challenges
 */
public class DBOChallenge implements MigratableDatabaseObject<DBOChallenge, DBOChallenge> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_CHALLENGE_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_CHALLENGE_ETAG).withIsEtag(true),
			new FieldColumn("participantTeamId", COL_CHALLENGE_PARTICIPANT_TEAM_ID),
			new FieldColumn("projectId", COL_CHALLENGE_PROJECT_ID),
			new FieldColumn("serializedEntity", COL_CHALLENGE_SERIALIZED_ENTITY),
	};

	private Long id;
	private String etag;
	private Long participantTeamId;
	private Long projectId;
	private byte[] serializedEntity;
	

	@Override
	public TableMapping<DBOChallenge> getTableMapping() {
		return new TableMapping<DBOChallenge>() {
			
			@Override
			public DBOChallenge mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOChallenge dbo = new DBOChallenge();
				dbo.setId(rs.getLong(COL_CHALLENGE_ID));
				dbo.setEtag(rs.getString(COL_CHALLENGE_ETAG));
				dbo.setParticipantTeamId(rs.getLong(COL_CHALLENGE_PARTICIPANT_TEAM_ID));
				dbo.setProjectId(rs.getLong(COL_CHALLENGE_PROJECT_ID));
				dbo.setSerializedEntity(rs.getBytes(COL_CHALLENGE_SERIALIZED_ENTITY));
				return dbo;
			}
			@Override public String getTableName() {
				return TABLE_CHALLENGE;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_CHALLENGE;
			}
			
			@Override
			public Class<? extends DBOChallenge> getDBOClass() {
				return DBOChallenge.class;
			}
		};
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
