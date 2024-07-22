package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_NOTES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VERIFICATION_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_STATE;

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

public class DBOVerificationState implements
		MigratableDatabaseObject<DBOVerificationState, DBOVerificationState> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_VERIFICATION_STATE_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("verificationId", COL_VERIFICATION_STATE_VERIFICATION_ID),
			new FieldColumn("createdOn", COL_VERIFICATION_STATE_CREATED_ON),
			new FieldColumn("createdBy", COL_VERIFICATION_STATE_CREATED_BY),
			new FieldColumn("state", COL_VERIFICATION_STATE_STATE),
			new FieldColumn("reason", COL_VERIFICATION_STATE_REASON),
			new FieldColumn("notes", COL_VERIFICATION_STATE_NOTES),
	};

	private Long id;
	private Long verificationId;
	private Long createdBy;
	private Long createdOn;
	private String state;
	private byte[] reason;
	private byte[] notes;

	@Override
	public TableMapping<DBOVerificationState> getTableMapping() {
		return new TableMapping<DBOVerificationState>() {
			
			@Override
			public DBOVerificationState mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOVerificationState dbo = new DBOVerificationState();
				dbo.setId(rs.getLong(COL_VERIFICATION_STATE_ID));
				dbo.setVerificationId(rs.getLong(COL_VERIFICATION_STATE_VERIFICATION_ID));
				dbo.setCreatedBy(rs.getLong(COL_VERIFICATION_STATE_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(COL_VERIFICATION_STATE_CREATED_ON));
				dbo.setState(rs.getString(COL_VERIFICATION_STATE_STATE));
				dbo.setReason(rs.getBytes(COL_VERIFICATION_STATE_REASON));
				dbo.setNotes(rs.getBytes(COL_VERIFICATION_STATE_NOTES));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_VERIFICATION_STATE;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_VERIFICATION_STATE;
			}
			
			@Override
			public Class<? extends DBOVerificationState> getDBOClass() {
				return DBOVerificationState.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_STATE;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationState, DBOVerificationState> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOVerificationState> getBackupClass() {
		return DBOVerificationState.class;
	}

	@Override
	public Class<? extends DBOVerificationState> getDatabaseObjectClass() {
		return DBOVerificationState.class;
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

	public Long getVerificationId() {
		return verificationId;
	}

	public void setVerificationId(Long verificationId) {
		this.verificationId = verificationId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public byte[] getReason() {
		return reason;
	}

	public void setReason(byte[] reason) {
		this.reason = reason;
	}
	
	public byte[] getNotes() {
		return notes;
	}
	
	public void setNotes(byte[] notes) {
		this.notes = notes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(notes);
		result = prime * result + Arrays.hashCode(reason);
		result = prime * result + Objects.hash(createdBy, createdOn, id, state, verificationId);
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
		DBOVerificationState other = (DBOVerificationState) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(id, other.id) && Arrays.equals(notes, other.notes)
				&& Arrays.equals(reason, other.reason) && Objects.equals(state, other.state)
				&& Objects.equals(verificationId, other.verificationId);
	}

	@Override
	public String toString() {
		return "DBOVerificationState [id=" + id + ", verificationId=" + verificationId + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", state=" + state + ", reason=" + Arrays.toString(reason) + ", notes=" + Arrays.toString(notes) + "]";
	}
	
}
