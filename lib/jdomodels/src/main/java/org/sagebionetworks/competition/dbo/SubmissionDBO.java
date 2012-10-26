package org.sagebionetworks.competition.dbo;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.DDL_FILE_SUBMISSION;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.TABLE_SUBMISSION;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_USER_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_COMP_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_ENTITY_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_STATUS;


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
public class SubmissionDBO implements DatabaseObject<SubmissionDBO>, TaggableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_SUBMISSION_ID, true),
			new FieldColumn("userId", COL_PARTICIPANT_USER_ID),
			new FieldColumn("compId", COL_PARTICIPANT_COMP_ID),
			new FieldColumn("createdOn", COL_PARTICIPANT_CREATED_ON),
			};

	public TableMapping<SubmissionDBO> getTableMapping() {
		return new TableMapping<SubmissionDBO>() {
			// Map a result set to this object
			public SubmissionDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				SubmissionDBO part = new SubmissionDBO();
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

			public Class<? extends SubmissionDBO> getDBOClass() {
				return SubmissionDBO.class;
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
