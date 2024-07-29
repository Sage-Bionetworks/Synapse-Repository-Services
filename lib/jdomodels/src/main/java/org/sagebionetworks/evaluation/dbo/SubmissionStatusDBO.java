package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBMISSION_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_ANNOTATIONS;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_ENITTY_JSON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_ETAG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_MODIFIED_ON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_SCORE;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_STATUS;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ENTITY_JSON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SCORE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_STATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_SUBSTATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigrateFromXStreamToJSON;
import org.sagebionetworks.repo.model.dbo.migration.XStreamToJsonTranslator;
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
			new FieldColumn(PARAM_SUBSTATUS_ANNOTATIONS, COL_SUBSTATUS_ANNOTATIONS),
			new FieldColumn(PARAM_SUBSTATUS_SCORE, COL_SUBSTATUS_SCORE),
			new FieldColumn(PARAM_SUBSTATUS_ENITTY_JSON, COL_SUBSTATUS_ENTITY_JSON)
			};
	
			private static final TableMapping<SubmissionStatusDBO> TABLE_MAPPER = new TableMapping<SubmissionStatusDBO>() {
				// Map a result set to this object
				public SubmissionStatusDBO mapRow(ResultSet rs, int rowNum) throws SQLException {
					SubmissionStatusDBO sub = new SubmissionStatusDBO();
					sub.setId(rs.getLong(COL_SUBMISSION_ID));
					sub.seteTag(rs.getString(COL_SUBSTATUS_ETAG));
					sub.setVersion(rs.getLong(COL_SUBSTATUS_VERSION));
					sub.setModifiedOn(rs.getLong(COL_SUBSTATUS_MODIFIED_ON));
					sub.setStatus(rs.getInt(COL_SUBSTATUS_STATUS));
					sub.setAnnotations(rs.getString(COL_SUBSTATUS_ANNOTATIONS));
					Double score = rs.getDouble(COL_SUBSTATUS_SCORE);
					sub.setScore(rs.wasNull() ? null : score);
					sub.setEntityJson(rs.getString(COL_SUBSTATUS_ENTITY_JSON));
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

	public TableMapping<SubmissionStatusDBO> getTableMapping() {
		return TABLE_MAPPER;
	}
	
	private Long id;
	private String eTag;
	private Long version;
	private Long modifiedOn;
	private int status;
	private String annotations;
	private Double score;
	private byte[] serializedEntity;
	private String entityJson;

	public String getEntityJson() {
		return entityJson;
	}
	public void setEntityJson(String entityJson) {
		this.entityJson = entityJson;
	}
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
	
	public String getAnnotations() {
		return annotations;
	}
	
	public void setAnnotations(String annotations) {
		this.annotations = annotations;
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
	public MigrationType getMigratableTableType() {
		return MigrationType.SUBMISSION_STATUS;
	}
	
	@Override
	public MigratableTableTranslation<SubmissionStatusDBO, SubmissionStatusDBO> getTranslator() {
		return new MigrateFromXStreamToJSON<SubmissionStatusDBO>(
				XStreamToJsonTranslator.builder().setFromName("serializedEntity").setToName("entityJson")
						.setDboType(getBackupClass()).setDtoType(SubmissionStatus.class).build());	}
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(serializedEntity);
		result = prime * result + Objects.hash(annotations, eTag, entityJson, id, modifiedOn, score, status, version);
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
		return Objects.equals(annotations, other.annotations) && Objects.equals(eTag, other.eTag)
				&& Objects.equals(entityJson, other.entityJson) && Objects.equals(id, other.id)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(score, other.score)
				&& Arrays.equals(serializedEntity, other.serializedEntity) && status == other.status
				&& Objects.equals(version, other.version);
	}
	@Override
	public String toString() {
		return "SubmissionStatusDBO [id=" + id + ", eTag=" + eTag + ", version=" + version + ", modifiedOn="
				+ modifiedOn + ", status=" + status + ", annotations=" + annotations + ", score=" + score
				+ ", serializedEntity=" + Arrays.toString(serializedEntity) + ", entityJson=" + entityJson + "]";
	}
	
}
