package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.*;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_PARTICIPANT_EVAL_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_PARTICIPANT_USER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.TaggableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The database object for a Participant in a Synapse Evaluation
 * 
 * @author bkng
 */
public class ParticipantDBO implements MigratableDatabaseObject<ParticipantDBO, ParticipantDBO>, TaggableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_PARTICIPANT_ID, COL_PARTICIPANT_ID).withIsBackupId(true),
			new FieldColumn(PARAM_PARTICIPANT_USER_ID, COL_PARTICIPANT_USER_ID, true),
			new FieldColumn(PARAM_PARTICIPANT_EVAL_ID, COL_PARTICIPANT_EVAL_ID, true),
			new FieldColumn(PARAM_PARTICIPANT_CREATED_ON, COL_PARTICIPANT_CREATED_ON),
			};

	public TableMapping<ParticipantDBO> getTableMapping() {
		return new TableMapping<ParticipantDBO>() {
			// Map a result set to this object
			public ParticipantDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				ParticipantDBO part = new ParticipantDBO();
				part.setId(rs.getLong(COL_PARTICIPANT_ID));
				part.setUserId(rs.getLong(COL_PARTICIPANT_USER_ID));
				part.setEvalId(rs.getLong(COL_PARTICIPANT_EVAL_ID));
				part.setCreatedOn(rs.getLong(COL_PARTICIPANT_CREATED_ON));
				return part;
			}

			public String getTableName() {
				return TABLE_PARTICIPANT;
			}

			public String getDDLFileName() {
				return DDL_FILE_PARTICIPANT;
			}

			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			public Class<? extends ParticipantDBO> getDBOClass() {
				return ParticipantDBO.class;
			}
		};
	}
	
	private Long userId;
	private Long evalId;
	private Long createdOn;
	private Long id;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getEvalId() {
		return evalId;
	}
	public void setEvalId(Long evalId) {
		this.evalId = evalId;
	}

	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public String toString() {
		return "ParticipantDBO [userId=" + userId + ", evalId=" + evalId
				+ ", createdOn=" + createdOn + ", id=" + id + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((evalId == null) ? 0 : evalId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ParticipantDBO other = (ParticipantDBO) obj;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (evalId == null) {
			if (other.evalId != null)
				return false;
		} else if (!evalId.equals(other.evalId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PARTICIPANT;
	}
	@Override
	public MigratableTableTranslation<ParticipantDBO, ParticipantDBO> getTranslator() {
		return new MigratableTableTranslation<ParticipantDBO, ParticipantDBO>(){

			@Override
			public ParticipantDBO createDatabaseObjectFromBackup(
					ParticipantDBO backup) {
				return backup;
			}

			@Override
			public ParticipantDBO createBackupFromDatabaseObject(
					ParticipantDBO dbo) {
				return dbo;
			}};
	}
	@Override
	public Class<? extends ParticipantDBO> getBackupClass() {
		return ParticipantDBO.class;
	}
	@Override
	public Class<? extends ParticipantDBO> getDatabaseObjectClass() {
		return ParticipantDBO.class;
	}
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}
