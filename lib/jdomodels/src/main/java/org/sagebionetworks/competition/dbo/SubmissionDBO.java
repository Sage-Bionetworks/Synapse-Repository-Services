package org.sagebionetworks.competition.dbo;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.DDL_FILE_SUBMISSION;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.TABLE_SUBMISSION;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_USER_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_COMP_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_ENTITY_ID;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_NAME;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.competition.query.jdo.SQLConstants.COL_SUBMISSION_STATUS;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.sagebionetworks.competition.model.SubmissionStatus;
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
			new FieldColumn("userId", COL_SUBMISSION_USER_ID),
			new FieldColumn("compId", COL_SUBMISSION_COMP_ID),
			new FieldColumn("entityId", COL_SUBMISSION_ENTITY_ID),
			new FieldColumn("name", COL_SUBMISSION_NAME),
			new FieldColumn("createdOn", COL_SUBMISSION_CREATED_ON),
			new FieldColumn("status", COL_SUBMISSION_STATUS)
			};

	public TableMapping<SubmissionDBO> getTableMapping() {
		return new TableMapping<SubmissionDBO>() {
			// Map a result set to this object
			public SubmissionDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				SubmissionDBO sub = new SubmissionDBO();
				sub.setId(rs.getLong(COL_SUBMISSION_ID));
				sub.setUserId(rs.getLong(COL_SUBMISSION_USER_ID));
				sub.setCompId(rs.getLong(COL_SUBMISSION_COMP_ID));
				sub.setEntityId(rs.getLong(COL_SUBMISSION_ENTITY_ID));
				sub.setName(rs.getString(COL_SUBMISSION_NAME));
				Timestamp ts = rs.getTimestamp(COL_SUBMISSION_CREATED_ON);
				sub.setCreatedOn(ts==null ? null : new Date(ts.getTime()));
				sub.setStatus(rs.getInt(COL_SUBMISSION_STATUS));
				return sub;
			}

			public String getTableName() {
				return TABLE_SUBMISSION;
			}

			public String getDDLFileName() {
				return DDL_FILE_SUBMISSION;
			}

			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			public Class<? extends SubmissionDBO> getDBOClass() {
				return SubmissionDBO.class;
			}
		};
	}
	
	private Long id;
	private Long userId;
	private Long compId;
	private Long entityId;
	private Date createdOn;
	private String name;
	private int status;

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

	public Long getCompId() {
		return compId;
	}
	public void setCompId(Long compId) {
		this.compId = compId;
	}
	
	public Long getEntityId() {
		return entityId;
	}
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}
	
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public int getStatus() {
		return status;
	}
	private void setStatus(int status) {
		this.status = status;
	}
	
	public SubmissionStatus getStatusEnum() {
		return SubmissionStatus.values()[status];
	}
	public void setStatusEnum(SubmissionStatus ss) {
		if (ss == null)	throw new IllegalArgumentException("Competition status cannot be null");
		setStatus(ss.ordinal());
	}

	@Override
	public String toString() {
		return "DBOSubmission [id = " + id + ", userId = " + userId + ", compId=" + compId 
				+ ", entityId = " + entityId + ", name = " + name + ", createdOn=" + createdOn 
				+ ", status = " + SubmissionStatus.values()[status].toString()+ "]";
	}

}
