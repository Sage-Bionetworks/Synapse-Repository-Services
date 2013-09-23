package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_INVITATION_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_MEMBERSHIP_INVITATION_SUBMISSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database Object for a MembershipInvtnSubmission.
 *
 */
public class DBOMembershipInvtnSubmission implements MigratableDatabaseObject<DBOMembershipInvtnSubmission, DBOMembershipInvtnSubmission> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MEMBERSHIP_INVITATION_SUBMISSION_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_MEMBERSHIP_INVITATION_SUBMISSION_ETAG).withIsEtag(true),
		new FieldColumn("teamId", COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID),
		new FieldColumn("expiresOn", COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON),
		new FieldColumn("properties", COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES)
	};
	
	private Long id;
	private String etag;
	private Long teamId;
	private long expiresOn;
	private byte[] properties;

	@Override
	public TableMapping<DBOMembershipInvtnSubmission> getTableMapping() {
		return new TableMapping<DBOMembershipInvtnSubmission>(){
			@Override
			public DBOMembershipInvtnSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMembershipInvtnSubmission team = new DBOMembershipInvtnSubmission();
				team.setId(rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_ID));
				team.setEtag(rs.getString(COL_MEMBERSHIP_INVITATION_SUBMISSION_ETAG));
				team.setTeamId(rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID));
				team.setExpiresOn(rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON));

				java.sql.Blob blob = rs.getBlob(COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES);
				if(blob != null){
					team.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				return team;
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



	public long getExpiresOn() {
		return expiresOn;
	}



	public void setExpiresOn(long expiresOn) {
		this.expiresOn = expiresOn;
	}



	@Override
	public MigrationType getMigratableTableType() {
		return null; //MigrationType.MEMBERSHIP_INVITATION_SUBMISSION;
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
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		List<MigratableDatabaseObject> list = new LinkedList<MigratableDatabaseObject>();
		list.add(new DBOMembershipInvitee());
		return list;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + (int) (expiresOn ^ (expiresOn >>> 32));
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (expiresOn != other.expiresOn)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
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
		return "DBOTeam [id=" + id + ", etag=" + etag + ", properties="
				+ Arrays.toString(properties) + "]";
	}
}
