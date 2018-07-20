package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_MEMBERSHIP_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_REQUEST;

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
 * Database Object for a MembershipRequest.
 *
 */
public class DBOMembershipRequest implements MigratableDatabaseObject<DBOMembershipRequest, DBOMembershipRequest> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MEMBERSHIP_REQUEST_ID, true).withIsBackupId(true),
		new FieldColumn("createdOn", COL_MEMBERSHIP_REQUEST_CREATED_ON),
		new FieldColumn("teamId", COL_MEMBERSHIP_REQUEST_TEAM_ID),
		new FieldColumn("userId", COL_MEMBERSHIP_REQUEST_USER_ID),
		new FieldColumn("expiresOn", COL_MEMBERSHIP_REQUEST_EXPIRES_ON),
		new FieldColumn("properties", COL_MEMBERSHIP_REQUEST_PROPERTIES)
	};
	
	private Long id;
	private Long createdOn;
	private Long teamId;
	private Long userId;
	private Long expiresOn;
	private byte[] properties;

	@Override
	public TableMapping<DBOMembershipRequest> getTableMapping() {
		return new TableMapping<DBOMembershipRequest>(){
			@Override
			public DBOMembershipRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMembershipRequest mrs = new DBOMembershipRequest();
				mrs.setId(rs.getLong(COL_MEMBERSHIP_REQUEST_ID));
				Long createdOn = rs.getLong(COL_MEMBERSHIP_REQUEST_CREATED_ON);
				if (rs.wasNull()) createdOn=null;
				mrs.setCreatedOn(createdOn);
				mrs.setTeamId(rs.getLong(COL_MEMBERSHIP_REQUEST_TEAM_ID));
				mrs.setUserId(rs.getLong(COL_MEMBERSHIP_REQUEST_USER_ID));
				Long expiresOn = rs.getLong(COL_MEMBERSHIP_REQUEST_EXPIRES_ON);
				if (rs.wasNull()) expiresOn=null;
				mrs.setExpiresOn(expiresOn);

				java.sql.Blob blob = rs.getBlob(COL_MEMBERSHIP_REQUEST_PROPERTIES);
				if(blob != null){
					mrs.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				return mrs;
			}

			@Override
			public String getTableName() {
				return TABLE_MEMBERSHIP_REQUEST;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_MEMBERSHIP_REQUEST;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMembershipRequest> getDBOClass() {
				return DBOMembershipRequest.class;
			}
			
		};
	}



	public Long getId() {
		return id;
	}



	public void setId(Long id) {
		this.id = id;
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



	public Long getUserId() {
		return userId;
	}



	public void setUserId(Long userId) {
		this.userId = userId;
	}



	public Long getExpiresOn() {
		return expiresOn;
	}



	public void setExpiresOn(Long expiresOn) {
		this.expiresOn = expiresOn;
	}



	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MEMBERSHIP_REQUEST_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOMembershipRequest, DBOMembershipRequest> getTranslator() {
		return new BasicMigratableTableTranslation<DBOMembershipRequest>();
	}

	@Override
	public Class<? extends DBOMembershipRequest> getBackupClass() {
		return DBOMembershipRequest.class;
	}

	@Override
	public Class<? extends DBOMembershipRequest> getDatabaseObjectClass() {
		return DBOMembershipRequest.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((expiresOn == null) ? 0 : expiresOn.hashCode());
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
		DBOMembershipRequest other = (DBOMembershipRequest) obj;
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
		return "DBOMembershipRequest [id=" + id + ", createdOn="
				+ createdOn + ", teamId=" + teamId + ", userId=" + userId
				+ ", expiresOn=" + expiresOn + ", properties="
				+ Arrays.toString(properties) + "]";
	}
}
