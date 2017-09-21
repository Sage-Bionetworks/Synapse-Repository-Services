package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

/**
 * Database Object for a MembershipInvtnSubmission.
 *
 */
public class DBOMembershipInvtnSubmission implements MigratableDatabaseObject<DBOMembershipInvtnSubmission, DBOMembershipInvtnSubmission> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MEMBERSHIP_INVITATION_SUBMISSION_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_MEMBERSHIP_INVITATION_ETAG).withIsEtag(true),
		new FieldColumn("createdOn", COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON),
		new FieldColumn("teamId", COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID),
		new FieldColumn("expiresOn", COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON),
		new FieldColumn("inviteeId", COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID),
		new FieldColumn("inviteeEmail", COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_EMAIL),
		new FieldColumn("properties", COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES)
	};
	
	private Long id;
	private String etag;
	private Long createdOn;
	private Long teamId;
	private Long expiresOn;
	private Long inviteeId;
	private String inviteeEmail;
	private byte[] properties;

	@Override
	public TableMapping<DBOMembershipInvtnSubmission> getTableMapping() {
		return new TableMapping<DBOMembershipInvtnSubmission>(){
			@Override
			public DBOMembershipInvtnSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMembershipInvtnSubmission dbo = new DBOMembershipInvtnSubmission();
				dbo.setId(rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_ID));
				String etag = rs.getString(COL_MEMBERSHIP_INVITATION_ETAG);
				if (rs.wasNull()) etag=null;
				dbo.setEtag(etag);
				Long createdOn = rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON);
				if (rs.wasNull()) createdOn=null;
				dbo.setCreatedOn(createdOn);
				dbo.setTeamId(rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID));
				Long expiresOn = rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON);
				if (rs.wasNull()) expiresOn=null;
				dbo.setExpiresOn(expiresOn);
				Long inviteeId = rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID);
				if (rs.wasNull()) inviteeId=null;
				dbo.setInviteeId(inviteeId);
				String inviteeEmail = rs.getString(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_EMAIL);
				if (rs.wasNull()) inviteeEmail=null;
				dbo.setInviteeEmail(inviteeEmail);
				java.sql.Blob blob = rs.getBlob(COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES);
				if(blob != null){
					dbo.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_MEMBERSHIP_INVITATION_SUBMISSION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_MEMBERSHIP_INVITATION_SUBMISSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMembershipInvtnSubmission> getDBOClass() {
				return DBOMembershipInvtnSubmission.class;
			}
			
		};
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


	public Long getCreatedOn() {
		return createdOn;
	}



	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}



	public byte[] getProperties() {
		return properties;
	}



	public void setProperties(byte[] properties) {
		this.properties = properties;
	}


	public Long getTeamId() {
		return teamId;
	}



	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}



	public Long getExpiresOn() {
		return expiresOn;
	}



	public void setExpiresOn(Long expiresOn) {
		this.expiresOn = expiresOn;
	}



	public Long getInviteeId() {
		return inviteeId;
	}



	public void setInviteeId(Long inviteeId) {
		this.inviteeId = inviteeId;
	}

	public String getInviteeEmail() {
		return inviteeEmail;
	}

	public void setInviteeEmail(String inviteeEmail) {
		this.inviteeEmail = inviteeEmail;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MEMBERSHIP_INVITATION_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOMembershipInvtnSubmission, DBOMembershipInvtnSubmission> getTranslator() {
		return new MigratableTableTranslation<DBOMembershipInvtnSubmission, DBOMembershipInvtnSubmission>(){

			@Override
			public DBOMembershipInvtnSubmission createDatabaseObjectFromBackup(DBOMembershipInvtnSubmission backup) {
				return backup;
			}

			@Override
			public DBOMembershipInvtnSubmission createBackupFromDatabaseObject(DBOMembershipInvtnSubmission dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOMembershipInvtnSubmission> getBackupClass() {
		return DBOMembershipInvtnSubmission.class;
	}

	@Override
	public Class<? extends DBOMembershipInvtnSubmission> getDatabaseObjectClass() {
		return DBOMembershipInvtnSubmission.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((expiresOn == null) ? 0 : expiresOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((inviteeId == null) ? 0 : inviteeId.hashCode());
		result = prime * result + ((inviteeEmail == null) ? 0 : inviteeEmail.hashCode());
		result = prime * result + Arrays.hashCode(properties);
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
		DBOMembershipInvtnSubmission other = (DBOMembershipInvtnSubmission) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (expiresOn == null) {
			if (other.expiresOn != null)
				return false;
		} else if (!expiresOn.equals(other.expiresOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (inviteeId == null) {
			if (other.inviteeId != null)
				return false;
		} else if (!inviteeId.equals(other.inviteeId))
			return false;
		if (inviteeEmail == null) {
			if (other.inviteeEmail != null)
				return false;
		} else if (!inviteeEmail.equals(other.inviteeEmail))
			return false;
		if (!Arrays.equals(properties, other.properties))
			return false;
		if (teamId == null) {
			if (other.teamId != null)
				return false;
		} else if (!teamId.equals(other.teamId))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "DBOMembershipInvtnSubmission [id=" + id + ", etag="
			+ etag + ", createdOn=" + createdOn + ", teamId=" + teamId
			+ ", expiresOn=" + expiresOn + ", inviteeId=" + inviteeId
			+ ", inviteeEmail=" + inviteeEmail
			+ ", properties=" + Arrays.toString(properties) + "]";
	}
}
