package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.*;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.TaggableEntity;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * The database object for the score and status of a Submission to a Synapse Evaluation
 * 
 * @author bkng
 */
public class SubmissionStatusDBO implements DatabaseObject<SubmissionStatusDBO>, TaggableEntity, ObservableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_SUBMISSION_ID, COL_SUBSTATUS_SUBMISSION_ID, true),
			new FieldColumn(PARAM_SUBSTATUS_ETAG, COL_SUBSTATUS_ETAG),
			new FieldColumn(PARAM_SUBSTATUS_MODIFIED_ON, COL_SUBSTATUS_MODIFIED_ON),
			new FieldColumn(PARAM_SUBSTATUS_STATUS, COL_SUBSTATUS_STATUS),
			new FieldColumn(PARAM_SUBSTATUS_SCORE, COL_SUBSTATUS_SCORE),
			new FieldColumn(PARAM_SUBSTATUS_SERIALIZED_ENTITY, COL_SUBSTATUS_SERIALIZED_ENTITY)
			};

	public TableMapping<SubmissionStatusDBO> getTableMapping() {
		return new TableMapping<SubmissionStatusDBO>() {
			// Map a result set to this object
			public SubmissionStatusDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				SubmissionStatusDBO sub = new SubmissionStatusDBO();
				sub.setId(rs.getLong(COL_SUBMISSION_ID));
				sub.seteTag(rs.getString(COL_SUBSTATUS_ETAG));
				sub.setModifiedOn(rs.getLong(COL_SUBSTATUS_MODIFIED_ON));
				sub.setStatus(rs.getInt(COL_SUBSTATUS_STATUS));
				sub.setScore(rs.getDouble(COL_SUBSTATUS_SCORE));
				java.sql.Blob blob = rs.getBlob(COL_SUBSTATUS_SERIALIZED_ENTITY);
				if(blob != null){
					sub.setSerializedEntity(blob.getBytes(1, (int) blob.length()));
				}
				return sub;
			}

			public String getTableName() {
				return TABLE_SUBSTATUS;
			}

			public String getDDLFileName() {
				return DDL_FILE_SUBSTATUS;
			}

			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			public Class<? extends SubmissionStatusDBO> getDBOClass() {
				return SubmissionStatusDBO.class;
			}
		};
	}
	
	private Long id;
	private String eTag;
	private Long modifiedOn;
	private int status;
	private Double score;
	private byte[] serializedEntity;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public Long getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Long createdOn) {
		this.modifiedOn = createdOn;
	}

	public int getStatus() {
		return status;
	}
	private void setStatus(int status) {
		this.status = status;
	}
	
	public SubmissionStatusEnum getStatusEnum() {
		return SubmissionStatusEnum.values()[status];
	}
	public void setStatusEnum(SubmissionStatusEnum ss) {
		if (ss == null)	throw new IllegalArgumentException("Submission status cannot be null");
		setStatus(ss.ordinal());
	}

	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
	@Override
	public String getIdString() {
		return id.toString();
	}
	@Override
	public String getParentIdString() {
		return null;
	}
	@Override
	public ObjectType getObjectType() {
		return ObjectType.SUBMISSION;
	}
	public byte[] getSerializedEntity() {
		return serializedEntity;
	}
	public void setSerializedEntity(byte[] serializedEntity) {
		this.serializedEntity = serializedEntity;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		result = prime * result + Arrays.hashCode(serializedEntity);
		result = prime * result + status;
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
		SubmissionStatusDBO other = (SubmissionStatusDBO) obj;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (score == null) {
			if (other.score != null)
				return false;
		} else if (!score.equals(other.score))
			return false;
		if (!Arrays.equals(serializedEntity, other.serializedEntity))
			return false;
		if (status != other.status)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SubmissionStatusDBO [id=" + id + ", eTag=" + eTag
				+ ", modifiedOn=" + modifiedOn + ", status=" + status
				+ ", score=" + score + ", serializedEntity="
				+ Arrays.toString(serializedEntity) + "]";
	}
	
}
