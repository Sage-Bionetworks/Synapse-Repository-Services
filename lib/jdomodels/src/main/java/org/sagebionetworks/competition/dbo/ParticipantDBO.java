package org.sagebionetworks.competition.dbo;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.DDL_FILE_PARTICIPANT;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.TABLE_PARTICIPANT;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_PARTICIPANT_USER_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_PARTICIPANT_COMP_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_PARTICIPANT_CREATED_ON;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

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
			new FieldColumn("userId", COL_PARTICIPANT_USER_ID, true),
			new FieldColumn("compId", COL_PARTICIPANT_COMP_ID, true),
			new FieldColumn("createdOn", COL_PARTICIPANT_CREATED_ON),
			};

	public TableMapping<ParticipantDBO> getTableMapping() {
		return new TableMapping<ParticipantDBO>() {
			// Map a result set to this object
			public ParticipantDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				ParticipantDBO part = new ParticipantDBO();
				part.setUserId(rs.getLong(COL_PARTICIPANT_USER_ID));
				part.setCompId(rs.getLong(COL_PARTICIPANT_COMP_ID));
				Timestamp ts = rs.getTimestamp(COL_PARTICIPANT_CREATED_ON);
				part.setCreatedOn(ts==null ? null : new Date(ts.getTime()));
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
	private Date createdOn;

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

	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public String toString() {
		return "DBOCompetition [userId=" + userId + ", compId=" + compId 
				+ ", createdOn=" + createdOn + "]";
	}

}
