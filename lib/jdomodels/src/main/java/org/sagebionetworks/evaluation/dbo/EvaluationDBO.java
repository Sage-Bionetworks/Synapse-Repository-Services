package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_CONTENT_SOURCE;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_CREATED_ON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_DESCRIPTION;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ETAG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_NAME;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_OWNER_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_QUOTA;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_STATUS;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_SUB_INSTRUCT_MSG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_SUB_RECEIPT_MSG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_CONTENT_SOURCE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_QUOTA;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_STATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUB_INSTRUCT_MSG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUB_RECEIPT_MSG;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_EVALUATION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The database object for a Synapse Evaluation
 * 
 * @author bkng
 */
public class EvaluationDBO implements MigratableDatabaseObject<EvaluationDBO, EvaluationBackup>, ObservableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_EVALUATION_ID, COL_EVALUATION_ID, true).withIsBackupId(true),
			new FieldColumn(PARAM_EVALUATION_ETAG, COL_EVALUATION_ETAG).withIsEtag(true),
			new FieldColumn(PARAM_EVALUATION_NAME, COL_EVALUATION_NAME),
			new FieldColumn(PARAM_EVALUATION_DESCRIPTION, COL_EVALUATION_DESCRIPTION),
			new FieldColumn(PARAM_EVALUATION_OWNER_ID, COL_EVALUATION_OWNER_ID),
			new FieldColumn(PARAM_EVALUATION_CREATED_ON, COL_EVALUATION_CREATED_ON),
			new FieldColumn(PARAM_EVALUATION_CONTENT_SOURCE, COL_EVALUATION_CONTENT_SOURCE),
			new FieldColumn(PARAM_EVALUATION_STATUS, COL_EVALUATION_STATUS),
			new FieldColumn(PARAM_EVALUATION_SUB_INSTRUCT_MSG, COL_EVALUATION_SUB_INSTRUCT_MSG),
			new FieldColumn(PARAM_EVALUATION_SUB_RECEIPT_MSG, COL_EVALUATION_SUB_RECEIPT_MSG),
			new FieldColumn(PARAM_EVALUATION_QUOTA, COL_EVALUATION_QUOTA)
	};

	public TableMapping<EvaluationDBO> getTableMapping() {
		return new TableMapping<EvaluationDBO>() {
			// Map a result set to this object
			public EvaluationDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				EvaluationDBO eval = new EvaluationDBO();
				eval.setId(rs.getLong(COL_EVALUATION_ID));
				eval.seteTag(rs.getString(COL_EVALUATION_ETAG));
				eval.setName(rs.getString(COL_EVALUATION_NAME));
				Blob blob = rs.getBlob(COL_EVALUATION_DESCRIPTION);
				if(blob != null){
					eval.setDescription(blob.getBytes(1, (int) blob.length()));
				}
				eval.setOwnerId(rs.getLong(COL_EVALUATION_OWNER_ID));
				eval.setCreatedOn(rs.getLong(COL_EVALUATION_CREATED_ON));
				eval.setContentSource(rs.getLong(COL_EVALUATION_CONTENT_SOURCE));
				eval.setStatus(rs.getInt(COL_EVALUATION_STATUS));
				blob = rs.getBlob(COL_EVALUATION_SUB_INSTRUCT_MSG);
				if (blob != null) {
					eval.setSubmissionInstructionsMessage(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_EVALUATION_SUB_RECEIPT_MSG);
				if (blob != null) {
					eval.setSubmissionReceiptMessage(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_EVALUATION_QUOTA);
				if (blob != null) {
					eval.setQuota(blob.getBytes(1, (int) blob.length()));
				}
				return eval;
			}

			public String getTableName() {
				return TABLE_EVALUATION;
			}

			public String getDDLFileName() {
				return DDL_FILE_EVALUATION;
			}

			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			public Class<? extends EvaluationDBO> getDBOClass() {
				return EvaluationDBO.class;
			}
		};
	}
	
	private Long id;
	private String eTag;
	private String name;
	private byte[] description;
	private Long ownerId;
	private Long createdOn;
	private Long contentSource;
	private int status;
	private byte[] submissionInstructionsMessage;
	private byte[] submissionReceiptMessage;
	private byte[] quota;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public byte[] getDescription() {
		return description;
	}
	public void setDescription(byte[] description) {
		this.description = description;
	}

	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public Long getContentSource() {
		return contentSource;
	}
	public void setContentSource(Long contentSource) {
		this.contentSource = contentSource;
	}

	public int getStatus() {
		return status;
	}
	private void setStatus(int status) {
		this.status = status;
	}
	
	public EvaluationStatus getStatusEnum() {
		return EvaluationStatus.values()[status];
	}
	public void setStatusEnum(EvaluationStatus es) {
		if (es == null)	throw new IllegalArgumentException("Evaluation status cannot be null");
		setStatus(es.ordinal());
	}

	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public byte[] getSubmissionInstructionsMessage() {
		return submissionInstructionsMessage;
	}
	public void setSubmissionInstructionsMessage(byte[] submissionInstructionsMessage) {
		this.submissionInstructionsMessage = submissionInstructionsMessage;
	}
	public byte[] getSubmissionReceiptMessage() {
		return submissionReceiptMessage;
	}
	public void setSubmissionReceiptMessage(byte[] submissionReceiptMessage) {
		this.submissionReceiptMessage = submissionReceiptMessage;
	}
		
	public byte[] getQuota() {
		return quota;
	}
	public void setQuota(byte[] quota) {
		this.quota = quota;
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
		return ObjectType.EVALUATION;
	}
	
	@Override
	public String getEtag() {
		return eTag;
	}
	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.EVALUATION;
	}

	@Override
	public Class<? extends EvaluationBackup> getBackupClass() {
		return EvaluationBackup.class;
	}
	@Override
	public Class<? extends EvaluationDBO> getDatabaseObjectClass() {
		return EvaluationDBO.class;
	}
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	@Override
	public String toString() {
		return "EvaluationDBO [id=" + id + ", eTag=" + eTag + ", name=" + name
				+ ", description=" + Arrays.toString(description)
				+ ", ownerId=" + ownerId + ", createdOn=" + createdOn
				+ ", contentSource=" + contentSource + ", status=" + status
				+ ", submissionInstructionsMessage="
				+ Arrays.toString(submissionInstructionsMessage)
				+ ", submissionReceiptMessage="
				+ Arrays.toString(submissionReceiptMessage) + ", quota="
				+ Arrays.toString(quota) + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentSource == null) ? 0 : contentSource.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + Arrays.hashCode(quota);
		result = prime * result + status;
		result = prime * result
				+ Arrays.hashCode(submissionInstructionsMessage);
		result = prime * result + Arrays.hashCode(submissionReceiptMessage);
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
		EvaluationDBO other = (EvaluationDBO) obj;
		if (contentSource == null) {
			if (other.contentSource != null)
				return false;
		} else if (!contentSource.equals(other.contentSource))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (!Arrays.equals(description, other.description))
			return false;
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (!Arrays.equals(quota, other.quota))
			return false;
		if (status != other.status)
			return false;
		if (!Arrays.equals(submissionInstructionsMessage,
				other.submissionInstructionsMessage))
			return false;
		if (!Arrays.equals(submissionReceiptMessage,
				other.submissionReceiptMessage))
			return false;
		return true;
	}
	@Override
	public MigratableTableTranslation<EvaluationDBO, EvaluationBackup> getTranslator() {
		return new MigratableTableTranslation<EvaluationDBO, EvaluationBackup>(){

			@Override
			public EvaluationDBO createDatabaseObjectFromBackup(
					EvaluationBackup backup) {
				return EvaluationTranslationUtil.createDatabaseObjectFromBackup(backup);
			}

			@Override
			public EvaluationBackup createBackupFromDatabaseObject(EvaluationDBO dbo) {
				return EvaluationTranslationUtil.createBackupFromDatabaseObject(dbo);
			}
		};
	}

}
