package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_ETAG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_MODIFIED_ON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_SCORE;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_SERIALIZED_ENTITY;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_STATUS;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SCORE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SERIALIZED_ENTITY;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_STATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_SUBSTATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.evaluation.dao.SubmissionUtils;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The database object for the score and status of a Submission to a Synapse Evaluation
 * 
 * @author bkng
 */
public class SubmissionStatusDBO implements MigratableDatabaseObject<SubmissionStatusDBO, SubmissionStatusDBO> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_SUBMISSION_ID, COL_SUBSTATUS_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn(PARAM_SUBSTATUS_ETAG, COL_SUBSTATUS_ETAG).withIsEtag(true),
			new FieldColumn(PARAM_SUBSTATUS_VERSION, COL_SUBSTATUS_VERSION),
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
				sub.setVersion(rs.getLong(COL_SUBSTATUS_VERSION));
				sub.setModifiedOn(rs.getLong(COL_SUBSTATUS_MODIFIED_ON));
				sub.setStatus(rs.getInt(COL_SUBSTATUS_STATUS));
				Double score = rs.getDouble(COL_SUBSTATUS_SCORE);
				sub.setScore(rs.wasNull() ? null : score);
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
	private Long version;
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
	public Long getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Long createdOn) {
		this.modifiedOn = createdOn;
	}

	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
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
	
	public String geteTag() {
		return eTag;
	}
	
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
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
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SubmissionStatusDBO [id=" + id + ", eTag=" + eTag
				+ ", version=" + version + ", modifiedOn=" + modifiedOn
				+ ", status=" + status + ", score=" + score
				+ ", serializedEntity=" + Arrays.toString(serializedEntity)
				+ "]";
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SUBMISSION_STATUS;
	}
	@Override
	public MigratableTableTranslation<SubmissionStatusDBO, SubmissionStatusDBO> getTranslator() {
		return new MigratableTableTranslation<SubmissionStatusDBO, SubmissionStatusDBO>(){

			@Override
			public SubmissionStatusDBO createDatabaseObjectFromBackup(
					SubmissionStatusDBO backup) {
				if (backup.getVersion()!=null) return backup;
				backup.setVersion(DBOConstants.SUBSTATUS_INITIAL_VERSION_NUMBER);
				byte[] serialized = backup.getSerializedEntity();
				backup.setSerializedEntity(serialized);
				SubmissionStatus status = SubmissionUtils.copyFromSerializedField(backup);
				status.setStatusVersion(DBOConstants.SUBSTATUS_INITIAL_VERSION_NUMBER);
				SubmissionUtils.copyToSerializedField(status, backup);
				return backup;
			}

			@Override
			public SubmissionStatusDBO createBackupFromDatabaseObject(
					SubmissionStatusDBO dbo) {
				return dbo;
			}
		};
	}
	@Override
	public Class<? extends SubmissionStatusDBO> getBackupClass() {
		return SubmissionStatusDBO.class;
	}
	@Override
	public Class<? extends SubmissionStatusDBO> getDatabaseObjectClass() {
		return SubmissionStatusDBO.class;
	}
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	
}
