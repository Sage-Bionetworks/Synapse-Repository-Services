package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_MEMBERSHIP_REQUEST_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_REQUEST_SUBMISSION;

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
 * Database Object for a MembershipRqstSubmission.
 *
 */
public class DBOMembershipRqstSubmission implements MigratableDatabaseObject<DBOMembershipRqstSubmission, DBOMembershipRqstSubmission> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MEMBERSHIP_REQUEST_SUBMISSION_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_MEMBERSHIP_REQUEST_SUBMISSION_ETAG).withIsEtag(true),
		new FieldColumn("teamId", COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID),
		new FieldColumn("userId", COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID),
		new FieldColumn("expiresOn", COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON),
		new FieldColumn("properties", COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES)
	};
	
	private Long id;
	private String etag;
	private Long teamId;
	private Long userId;
	private long expiresOn;
	private byte[] properties;

	@Override
	public TableMapping<DBOMembershipRqstSubmission> getTableMapping() {
		return new TableMapping<DBOMembershipRqstSubmission>(){
			@Override
			public DBOMembershipRqstSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMembershipRqstSubmission team = new DBOMembershipRqstSubmission();
				team.setId(rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_ID));
				team.setEtag(rs.getString(COL_MEMBERSHIP_REQUEST_SUBMISSION_ETAG));
				team.setTeamId(rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID));
				team.setUserId(rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID));
				team.setExpiresOn(rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON));

				java.sql.Blob blob = rs.getBlob(COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES);
				if(blob != null){
					team.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				return team;
			}

			@Override
			public String getTableName() {
				return TABLE_MEMBERSHIP_REQUEST_SUBMISSION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_MEMBERSHIP_REQUEST_SUBMISSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMembershipRqstSubmission> getDBOClass() {
				return DBOMembershipRqstSubmission.class;
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



	public Long getUserId() {
		return userId;
	}



	public void setUserId(Long userId) {
		this.userId = userId;
	}



	public long getExpiresOn() {
		return expiresOn;
	}



	public void setExpiresOn(long expiresOn) {
		this.expiresOn = expiresOn;
	}



	@Override
	public MigrationType getMigratableTableType() {
		return null; //MigrationType.MEMBERSHIP_REQUEST_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOMembershipRqstSubmission, DBOMembershipRqstSubmission> getTranslator() {
		return new MigratableTableTranslation<DBOMembershipRqstSubmission, DBOMembershipRqstSubmission>(){

			@Override
			public DBOMembershipRqstSubmission createDatabaseObjectFromBackup(DBOMembershipRqstSubmission backup) {
				return backup;
			}

			@Override
			public DBOMembershipRqstSubmission createBackupFromDatabaseObject(DBOMembershipRqstSubmission dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOMembershipRqstSubmission> getBackupClass() {
		return DBOMembershipRqstSubmission.class;
	}

	@Override
	public Class<? extends DBOMembershipRqstSubmission> getDatabaseObjectClass() {
		return DBOMembershipRqstSubmission.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
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
		DBOMembershipRqstSubmission other = (DBOMembershipRqstSubmission) obj;
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
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "DBOMembershipRqstSubmission [id=" + id + ", etag=" + etag
				+ ", teamId=" + teamId + ", userId=" + userId + ", expiresOn="
				+ expiresOn + ", properties=" + Arrays.toString(properties)
				+ "]";
	}
}
