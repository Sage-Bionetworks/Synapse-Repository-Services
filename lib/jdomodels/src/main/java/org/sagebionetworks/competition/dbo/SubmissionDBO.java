package org.sagebionetworks.competition.dbo;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.*;
import static org.sagebionetworks.competition.dbo.DBOConstants.*;

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
			new FieldColumn(PARAM_SUBMISSION_ID, COL_SUBMISSION_ID, true),
			new FieldColumn(PARAM_SUBMISSION_USER_ID, COL_SUBMISSION_USER_ID),
			new FieldColumn(PARAM_SUBMISSION_COMP_ID, COL_SUBMISSION_COMP_ID),
			new FieldColumn(PARAM_SUBMISSION_ENTITY_ID, COL_SUBMISSION_ENTITY_ID),
			new FieldColumn(PARAM_SUBMISSION_NAME, COL_SUBMISSION_NAME),
			new FieldColumn(PARAM_SUBMISSION_CREATED_ON, COL_SUBMISSION_CREATED_ON),
			new FieldColumn(PARAM_SUBMISSION_STATUS, COL_SUBMISSION_STATUS),
			new FieldColumn(PARAM_SUBMISSION_SCORE, COL_SUBMISSION_SCORE)
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
				sub.setScore(rs.getInt(COL_SUBMISSION_SCORE));
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
	private int score;

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

	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	
	@Override
	public String toString() {
		return "DBOSubmission [id = " + id + ", userId = " + userId + ", compId=" + compId 
				+ ", entityId = " + entityId + ", name = " + name + ", createdOn=" + createdOn 
				+ ", status = " + SubmissionStatus.values()[status].toString()+  ", score = " + score + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((compId == null) ? 0 : compId.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + score;
		result = prime * result + status;
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
		SubmissionDBO other = (SubmissionDBO) obj;
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
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (score != other.score)
			return false;
		if (status != other.status)
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
