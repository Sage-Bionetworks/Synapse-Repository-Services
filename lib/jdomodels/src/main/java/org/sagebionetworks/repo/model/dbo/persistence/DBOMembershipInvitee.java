package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

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
 *
 */
public class DBOMembershipInvitee implements MigratableDatabaseObject<DBOMembershipInvitee, DBOMembershipInvitee> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("invitationId", COL_MEMBERSHIP_INVITEE_INVITATION_ID, true).withIsBackupId(true),
		new FieldColumn("inviteeId", COL_MEMBERSHIP_INVITEE_INVITEE_ID)
	};
	
	private Long invitationId;
	private Long inviteeId;


	@Override
	public TableMapping<DBOMembershipInvitee> getTableMapping() {
		return new TableMapping<DBOMembershipInvitee>(){
			@Override
			public DBOMembershipInvitee mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMembershipInvitee mi = new DBOMembershipInvitee();
				mi.setInvitationId(rs.getLong(COL_MEMBERSHIP_INVITEE_INVITATION_ID));
				mi.setInviteeId(rs.getLong(COL_MEMBERSHIP_INVITEE_INVITEE_ID));
				return mi;
			}

			@Override
			public String getTableName() {
				return TABLE_MEMBERSHIP_INVITEE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_MEMBERSHIP_INVITEE_SUBMISSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMembershipInvitee> getDBOClass() {
				return DBOMembershipInvitee.class;
			}
			
		};
	}



	public Long getInvitationId() {
		return invitationId;
	}



	public void setInvitationId(Long invitationId) {
		this.invitationId = invitationId;
	}



	public Long getInviteeId() {
		return inviteeId;
	}



	public void setInviteeId(Long inviteeId) {
		this.inviteeId = inviteeId;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MEMBERSHIP_INVITATION_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOMembershipInvitee, DBOMembershipInvitee> getTranslator() {
		return new MigratableTableTranslation<DBOMembershipInvitee, DBOMembershipInvitee>(){

			@Override
			public DBOMembershipInvitee createDatabaseObjectFromBackup(DBOMembershipInvitee backup) {
				return backup;
			}

			@Override
			public DBOMembershipInvitee createBackupFromDatabaseObject(DBOMembershipInvitee dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOMembershipInvitee> getBackupClass() {
		return DBOMembershipInvitee.class;
	}

	@Override
	public Class<? extends DBOMembershipInvitee> getDatabaseObjectClass() {
		return DBOMembershipInvitee.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((invitationId == null) ? 0 : invitationId.hashCode());
		result = prime * result
				+ ((inviteeId == null) ? 0 : inviteeId.hashCode());
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
		DBOMembershipInvitee other = (DBOMembershipInvitee) obj;
		if (invitationId == null) {
			if (other.invitationId != null)
				return false;
		} else if (!invitationId.equals(other.invitationId))
			return false;
		if (inviteeId == null) {
			if (other.inviteeId != null)
				return false;
		} else if (!inviteeId.equals(other.inviteeId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMembershipInvitee [invitationId=" + invitationId
				+ ", inviteeId=" + inviteeId + "]";
	}
}
