package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_CONTENT_SOURCE;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_CREATED_ON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_DESCRIPTION;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_END_TIMESTAMP;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ETAG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_NAME;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_OWNER_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_QUOTA_JSON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_START_TIMESTAMP;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_SUB_INSTRUCT_MSG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_SUB_RECEIPT_MSG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_CONTENT_SOURCE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_END_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_QUOTA_JSON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_START_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUB_INSTRUCT_MSG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUB_RECEIPT_MSG;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_EVALUATION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
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
			new FieldColumn(PARAM_EVALUATION_SUB_INSTRUCT_MSG, COL_EVALUATION_SUB_INSTRUCT_MSG),
			new FieldColumn(PARAM_EVALUATION_SUB_RECEIPT_MSG, COL_EVALUATION_SUB_RECEIPT_MSG),
			new FieldColumn(PARAM_EVALUATION_QUOTA_JSON, COL_EVALUATION_QUOTA_JSON),
			new FieldColumn(PARAM_EVALUATION_START_TIMESTAMP, COL_EVALUATION_START_TIMESTAMP),
			new FieldColumn(PARAM_EVALUATION_END_TIMESTAMP, COL_EVALUATION_END_TIMESTAMP)
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
				blob = rs.getBlob(COL_EVALUATION_SUB_INSTRUCT_MSG);
				if (blob != null) {
					eval.setSubmissionInstructionsMessage(blob.getBytes(1, (int) blob.length()));
				}
				blob = rs.getBlob(COL_EVALUATION_SUB_RECEIPT_MSG);
				if (blob != null) {
					eval.setSubmissionReceiptMessage(blob.getBytes(1, (int) blob.length()));
				}
				eval.setQuotaJson(rs.getString(COL_EVALUATION_QUOTA_JSON));
				eval.setStartTimestamp(rs.getLong(COL_EVALUATION_START_TIMESTAMP));
				if (rs.wasNull()) {
					eval.setStartTimestamp(null);
				}
				eval.setEndTimestamp(rs.getLong(COL_EVALUATION_END_TIMESTAMP));
				if (rs.wasNull()) {
					eval.setEndTimestamp(null);
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
	private byte[] submissionInstructionsMessage;
	private byte[] submissionReceiptMessage;
	private byte[] quota;
	private String quotaJson;
	private Long startTimestamp;
	private Long endTimestamp;
	
	public String getQuotaJson() {
		return quotaJson;
	}
	public void setQuotaJson(String quotaJson) {
		this.quotaJson = quotaJson;
	}
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
	public ObjectType getObjectType() {
		return ObjectType.EVALUATION;
	}
	
	@Override
	public String getEtag() {
		return eTag;
	}
	
	public Long getStartTimestamp() {
		return startTimestamp;
	}
	public void setStartTimestamp(Long startTimestamp) {
		this.startTimestamp = startTimestamp;
	}
	public Long getEndTimestamp() {
		return endTimestamp;
	}
	public void setEndTimestamp(Long endTimestamp) {
		this.endTimestamp = endTimestamp;
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
		return "EvaluationDBO [id=" + id + ", eTag=" + eTag + ", name=" + name + ", description="
				+ Arrays.toString(description) + ", ownerId=" + ownerId + ", createdOn=" + createdOn
				+ ", contentSource=" + contentSource + ", submissionInstructionsMessage="
				+ Arrays.toString(submissionInstructionsMessage) + ", submissionReceiptMessage="
				+ Arrays.toString(submissionReceiptMessage) + ", quota=" + Arrays.toString(quota) + ", quotaJson="
				+ quotaJson + ", startTimestamp=" + startTimestamp + ", endTimestamp=" + endTimestamp + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + Arrays.hashCode(quota);
		result = prime * result + Arrays.hashCode(submissionInstructionsMessage);
		result = prime * result + Arrays.hashCode(submissionReceiptMessage);
		result = prime * result + Objects.hash(contentSource, createdOn, eTag, endTimestamp, id, name, ownerId,
				quotaJson, startTimestamp);
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
		return Objects.equals(contentSource, other.contentSource) && Objects.equals(createdOn, other.createdOn)
				&& Arrays.equals(description, other.description) && Objects.equals(eTag, other.eTag)
				&& Objects.equals(endTimestamp, other.endTimestamp) && Objects.equals(id, other.id)
				&& Objects.equals(name, other.name) && Objects.equals(ownerId, other.ownerId)
				&& Arrays.equals(quota, other.quota) && Objects.equals(quotaJson, other.quotaJson)
				&& Objects.equals(startTimestamp, other.startTimestamp)
				&& Arrays.equals(submissionInstructionsMessage, other.submissionInstructionsMessage)
				&& Arrays.equals(submissionReceiptMessage, other.submissionReceiptMessage);
	}
	
	public static UnmodifiableXStream XSTREAM = UnmodifiableXStream.builder().allowTypes(SubmissionQuota.class).build();

	@Override
	public MigratableTableTranslation<EvaluationDBO, EvaluationBackup> getTranslator() {
		
		return new MigratableTableTranslation<EvaluationDBO, EvaluationBackup>(){

			@Override
			public EvaluationDBO createDatabaseObjectFromBackup(EvaluationBackup backup) {
				EvaluationDBO dbo = EvaluationTranslationUtil.createDatabaseObjectFromBackup(backup);
				try {
					if (dbo.getQuota() != null) {
						if (dbo.getQuotaJson() != null) {
							throw new IllegalArgumentException(
									String.format("Both '%s' and '%s' are not null", "quota", "quotaJson"));
						}
					}
					SubmissionQuota quota = (SubmissionQuota) JDOSecondaryPropertyUtils.decompressObject(XSTREAM,
							dbo.getQuota());
					dbo.setQuotaJson(JDOSecondaryPropertyUtils.createJSONFromObject(quota));
					dbo.setQuota(null);
					return dbo;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public EvaluationBackup createBackupFromDatabaseObject(EvaluationDBO dbo) {
				return EvaluationTranslationUtil.createBackupFromDatabaseObject(dbo);
			}
		};
	}

}
