package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ATTACHMENTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_DUC_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_IRB_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMISSION_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubmission implements MigratableDatabaseObject<DBOSubmission, DBOSubmission>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID),
			new FieldColumn("accessRequirementVersion", COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_VERSION),
			new FieldColumn("dataAccessRequestId", COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID),
			new FieldColumn("createdBy", COL_DATA_ACCESS_SUBMISSION_CREATED_BY),
			new FieldColumn("createdOn", COL_DATA_ACCESS_SUBMISSION_CREATED_ON),
			new FieldColumn("etag", COL_DATA_ACCESS_SUBMISSION_ETAG).withIsEtag(true),
			new FieldColumn("researchProjectId", COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID),
			new FieldColumn("ducFileHandleId", COL_DATA_ACCESS_SUBMISSION_DUC_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("irbFileHandleId", COL_DATA_ACCESS_SUBMISSION_IRB_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("attachmentsJson", COL_DATA_ACCESS_SUBMISSION_ATTACHMENTS).withHasFileHandleRef(true).withIsFileHandleIdJsonList(true),
			new FieldColumn("submissionJson", COL_DATA_ACCESS_SUBMISSION_SUBMISSION_JSON)
		};

	private Long id;
	private Long accessRequirementId;
	private Long accessRequirementVersion;
	private Long dataAccessRequestId;
	private Long createdBy;
	private Long createdOn;
	private String etag;
	private Long researchProjectId;
	private Long ducFileHandleId;
	private Long irbFileHandleId;
	private String attachmentsJson;
	private String submissionJson;
	/**
	 * Only populated when restoring a backup taken before the submission was stored as a JSON document. It has no
	 * {@link FieldColumn}: the translator moves its content into the document and file handle columns above and clears it. Remove with the
	 * rest of the legacy blob support (see {@link SubmissionUtils#readLegacySerializedField(byte[])}) once every stack
	 * has been migrated, tracked by PLFM-10061.
	 */
	private byte[] submissionSerialized;

	@Override
	public String toString() {
		return "DBOSubmission [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", accessRequirementVersion=" + accessRequirementVersion + ", dataAccessRequestId="
				+ dataAccessRequestId + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", etag=" + etag
				+ ", researchProjectId=" + researchProjectId + ", ducFileHandleId=" + ducFileHandleId
				+ ", irbFileHandleId=" + irbFileHandleId + ", attachmentsJson=" + attachmentsJson
				+ ", submissionJson=" + submissionJson
				+ ", submissionSerialized=" + Arrays.toString(submissionSerialized) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(submissionSerialized);
		result = prime * result + Objects.hash(id, accessRequirementId, accessRequirementVersion, dataAccessRequestId,
				createdBy, createdOn, etag, researchProjectId, ducFileHandleId, irbFileHandleId, attachmentsJson,
				submissionJson);
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
		DBOSubmission other = (DBOSubmission) obj;
		return Objects.equals(id, other.id) && Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(accessRequirementVersion, other.accessRequirementVersion)
				&& Objects.equals(dataAccessRequestId, other.dataAccessRequestId)
				&& Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(researchProjectId, other.researchProjectId)
				&& Objects.equals(ducFileHandleId, other.ducFileHandleId)
				&& Objects.equals(irbFileHandleId, other.irbFileHandleId)
				&& Objects.equals(attachmentsJson, other.attachmentsJson)
				&& Objects.equals(submissionJson, other.submissionJson)
				&& Arrays.equals(submissionSerialized, other.submissionSerialized);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public Long getAccessRequirementVersion() {
		return accessRequirementVersion;
	}

	public void setAccessRequirementVersion(Long accessRequirementVersion) {
		this.accessRequirementVersion = accessRequirementVersion;
	}

	public Long getDataAccessRequestId() {
		return dataAccessRequestId;
	}

	public void setDataAccessRequestId(Long dataAccessRequestId) {
		this.dataAccessRequestId = dataAccessRequestId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public byte[] getSubmissionSerialized() {
		return submissionSerialized;
	}

	public void setSubmissionSerialized(byte[] submissionSerialized) {
		this.submissionSerialized = submissionSerialized;
	}

	public Long getResearchProjectId() {
		return researchProjectId;
	}

	public void setResearchProjectId(Long researchProjectId) {
		this.researchProjectId = researchProjectId;
	}

	public Long getDucFileHandleId() {
		return ducFileHandleId;
	}

	public void setDucFileHandleId(Long ducFileHandleId) {
		this.ducFileHandleId = ducFileHandleId;
	}

	public Long getIrbFileHandleId() {
		return irbFileHandleId;
	}

	public void setIrbFileHandleId(Long irbFileHandleId) {
		this.irbFileHandleId = irbFileHandleId;
	}

	public String getAttachmentsJson() {
		return attachmentsJson;
	}

	public void setAttachmentsJson(String attachmentsJson) {
		this.attachmentsJson = attachmentsJson;
	}

	public String getSubmissionJson() {
		return submissionJson;
	}

	public void setSubmissionJson(String submissionJson) {
		this.submissionJson = submissionJson;
	}

	@Override
	public TableMapping<DBOSubmission> getTableMapping() {
		return new TableMapping<DBOSubmission>(){

			@Override
			public DBOSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubmission dbo = new DBOSubmission();
				dbo.setId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ID));
				dbo.setAccessRequirementId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID));
				dbo.setAccessRequirementVersion(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_VERSION));
				dbo.setDataAccessRequestId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID));
				dbo.setCreatedBy(rs.getLong(COL_DATA_ACCESS_SUBMISSION_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(COL_DATA_ACCESS_SUBMISSION_CREATED_ON));
				dbo.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_ETAG));
				Long researchProjectId = rs.getLong(COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID);
				dbo.setResearchProjectId(rs.wasNull() ? null : researchProjectId);
				Long ducFileHandleId = rs.getLong(COL_DATA_ACCESS_SUBMISSION_DUC_FILE_HANDLE_ID);
				dbo.setDucFileHandleId(rs.wasNull() ? null : ducFileHandleId);
				Long irbFileHandleId = rs.getLong(COL_DATA_ACCESS_SUBMISSION_IRB_FILE_HANDLE_ID);
				dbo.setIrbFileHandleId(rs.wasNull() ? null : irbFileHandleId);
				dbo.setAttachmentsJson(rs.getString(COL_DATA_ACCESS_SUBMISSION_ATTACHMENTS));
				dbo.setSubmissionJson(rs.getString(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_JSON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_ACCESS_SUBMISSION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_ACCESS_SUBMISSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSubmission> getDBOClass() {
				return DBOSubmission.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOSubmission, DBOSubmission> getTranslator() {
		return new MigratableTableTranslation<DBOSubmission, DBOSubmission>() {
			@Override
			public DBOSubmission createDatabaseObjectFromBackup(DBOSubmission backup) {
				if (backup.getSubmissionSerialized() != null) {
					SubmissionUtils.moveLegacySerializedFieldToColumns(backup);
				}
				return backup;
			}

			@Override
			public DBOSubmission createBackupFromDatabaseObject(DBOSubmission dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOSubmission> getBackupClass() {
		return DBOSubmission.class;
	}

	@Override
	public Class<? extends DBOSubmission> getDatabaseObjectClass() {
		return DBOSubmission.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOSubmissionStatus());
		list.add(new DBOSubmissionAccessorChange());
		return list;
	}

}
