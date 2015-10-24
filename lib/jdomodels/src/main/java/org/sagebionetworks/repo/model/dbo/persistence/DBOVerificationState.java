package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_SUBMISSION_ID;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;

@Table(name = TABLE_VERIFICATION_STATE)
public class DBOVerificationState implements
		MigratableDatabaseObject<DBOVerificationState, DBOVerificationState> {

	// TODO do secondary tables have backupId (foreign key) = true???
	@Field(name = VERIFICATION_STATE_ID, backupId = true, primary = true, nullable = false)
	@ForeignKey(table = TABLE_VERIFICATION_SUBMISSION, field = VERIFICATION_SUBMISSION_ID, cascadeDelete = true)
	private Long id;
	
	@Field(name = VERIFICATION_STATE_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long createdBy;
	
	@Field(name = VERIFICATION_STATE_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;
	
	@Field(name = VERIFICATION_STATE_STATE, backupId = false, primary = false, nullable = false)
	private VerificationStateEnum state;

	@Field(name = VERIFICATION_STATE_REASON, backupId = false, primary = false, nullable = true)
	private String reason;

	private static TableMapping<DBOVerificationState> TABLE_MAPPING = AutoTableMapping.create(DBOVerificationState.class);

	@Override
	public TableMapping<DBOVerificationState> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_STATE;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationState, DBOVerificationState> getTranslator() {
		return new MigratableTableTranslation<DBOVerificationState, DBOVerificationState>() {

			@Override
			public DBOVerificationState createDatabaseObjectFromBackup(
					DBOVerificationState backup) {
				return backup;
			}

			@Override
			public DBOVerificationState createBackupFromDatabaseObject(
					DBOVerificationState dbo) {
				return dbo;
			}
			
		};
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

	public VerificationStateEnum getState() {
		return state;
	}

	public void setState(VerificationStateEnum state) {
		this.state = state;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((reason == null) ? 0 : reason.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
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
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (reason == null) {
			if (other.reason != null)
				return false;
		} else if (!reason.equals(other.reason))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOVerificationState [id=" + id + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + ", state=" + state + ", reason="
				+ reason + "]";
	}
	
	
}
