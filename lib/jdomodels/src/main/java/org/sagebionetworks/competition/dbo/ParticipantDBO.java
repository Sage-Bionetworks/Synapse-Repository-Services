package org.sagebionetworks.competition.dbo;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.*;
import static org.sagebionetworks.competition.dbo.DBOConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.sagebionetworks.repo.model.TaggableEntity;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The database object for a Participant in a Synapse Competition
 * 
 * @author bkng
 */
public class ParticipantDBO implements DatabaseObject<ParticipantDBO>, TaggableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_PARTICIPANT_USER_ID, COL_PARTICIPANT_USER_ID, true),
			new FieldColumn(PARAM_PARTICIPANT_COMP_ID, COL_PARTICIPANT_COMP_ID, true),
			new FieldColumn(PARAM_PARTICIPANT_CREATED_ON, COL_PARTICIPANT_CREATED_ON),
			};

	public TableMapping<ParticipantDBO> getTableMapping() {
		return new TableMapping<ParticipantDBO>() {
			// Map a result set to this object
			public ParticipantDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				ParticipantDBO part = new ParticipantDBO();
				part.setUserId(rs.getLong(COL_PARTICIPANT_USER_ID));
				part.setCompId(rs.getLong(COL_PARTICIPANT_COMP_ID));
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
	private Long compId;
	private Long createdOn;

	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getCompId() {
		return compId;
	}
	public void setCompId(Long compId) {
		this.compId = compId;
	}

	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public String toString() {
		return "DBOCompetition [userId=" + userId + ", compId=" + compId 
				+ ", createdOn=" + createdOn + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((compId == null) ? 0 : compId.hashCode());
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
		if (compId == null) {
			if (other.compId != null)
				return false;
		} else if (!compId.equals(other.compId))
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
