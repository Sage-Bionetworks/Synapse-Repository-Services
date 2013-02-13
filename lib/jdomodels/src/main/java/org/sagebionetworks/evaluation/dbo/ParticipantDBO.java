package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.*;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.sagebionetworks.repo.model.TaggableEntity;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The database object for a Participant in a Synapse Evaluation
 * 
 * @author bkng
 */
public class ParticipantDBO implements DatabaseObject<ParticipantDBO>, TaggableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_PARTICIPANT_USER_ID, COL_PARTICIPANT_USER_ID, true),
			new FieldColumn(PARAM_PARTICIPANT_EVAL_ID, COL_PARTICIPANT_EVAL_ID, true),
			new FieldColumn(PARAM_PARTICIPANT_CREATED_ON, COL_PARTICIPANT_CREATED_ON),
			};

	public TableMapping<ParticipantDBO> getTableMapping() {
		return new TableMapping<ParticipantDBO>() {
			// Map a result set to this object
			public ParticipantDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				ParticipantDBO part = new ParticipantDBO();
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
				+ ", createdOn=" + createdOn + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((evalId == null) ? 0 : evalId.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
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
		if (evalId == null) {
			if (other.evalId != null)
				return false;
		} else if (!evalId.equals(other.evalId))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
