package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ATTACHMENTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SCHEMA_DATA_ATTACHMENTS;
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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
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
			new FieldColumn("schemaDataAttachmentsJson", COL_DATA_ACCESS_SUBMISSION_SCHEMA_DATA_ATTACHMENTS).withHasFileHandleRef(true).withIsFileHandleIdJsonList(true),
			new FieldColumn("submissionJson", COL_DATA_ACCESS_SUBMISSION_SUBMISSION_JSON)
		};

	/**
	 * Bridge: decodes the submission content that a previous stack stored as a XStream blob, only used by
	 * {@link #getTranslator()}. Removed in the next stack, see PLFM-10061.
	 */
	private static final UnmodifiableXStream SUBMISSION_XSTREAM = UnmodifiableXStream.builder()
			.allowTypes(Submission.class)
			.allowTypesByWildcard(new String[] {"org.sagebionetworks.repo.model.**"})
			.build();

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
	private String schemaDataAttachmentsJson;
	private String submissionJson;
	/**
	 * Bridge field: it has no column and is only populated when reading a backup written by a stack that still stored the
	 * submission content as a XStream blob. {@link #fromLegacyBackup(DBOSubmission)} converts it into the columns above. It
	 * is removed in the next stack, see PLFM-10061.
	 */
	private byte[] submissionSerialized;

	@Override
	public String toString() {
		return "DBOSubmission [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", accessRequirementVersion=" + accessRequirementVersion + ", dataAccessRequestId="
				+ dataAccessRequestId + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", etag=" + etag
				+ ", researchProjectId=" + researchProjectId + ", ducFileHandleId=" + ducFileHandleId
				+ ", irbFileHandleId=" + irbFileHandleId + ", attachmentsJson=" + attachmentsJson
				+ ", schemaDataAttachmentsJson=" + schemaDataAttachmentsJson
				+ ", submissionJson=" + submissionJson
				+ ", submissionSerialized=" + Arrays.toString(submissionSerialized) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(submissionSerialized);
		result = prime * result + Objects.hash(id, accessRequirementId, accessRequirementVersion, dataAccessRequestId,
				createdBy, createdOn, etag, researchProjectId, ducFileHandleId, irbFileHandleId, attachmentsJson, schemaDataAttachmentsJson,
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
				&& Objects.equals(schemaDataAttachmentsJson, other.schemaDataAttachmentsJson)
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

	public String getSchemaDataAttachmentsJson() {
		return schemaDataAttachmentsJson;
	}

	public void setSchemaDataAttachmentsJson(String schemaDataAttachmentsJson) {
		this.schemaDataAttachmentsJson = schemaDataAttachmentsJson;
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
				dbo.setSchemaDataAttachmentsJson(rs.getString(COL_DATA_ACCESS_SUBMISSION_SCHEMA_DATA_ATTACHMENTS));
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

	/**
	 * The translator converts a backup written by a stack that still stored the submission content in the
	 * {@link #getSubmissionSerialized() XStream blob} into the columns of the table, see
	 * {@link #fromLegacyBackup(DBOSubmission)}.
	 */
	@Override
	public MigratableTableTranslation<DBOSubmission, DBOSubmission> getTranslator() {
		return new MigratableTableTranslation<DBOSubmission, DBOSubmission>() {
			@Override
			public DBOSubmission createDatabaseObjectFromBackup(DBOSubmission backup) {
				return backup.getSubmissionSerialized() == null ? backup : fromLegacyBackup(backup);
			}

			@Override
			public DBOSubmission createBackupFromDatabaseObject(DBOSubmission dbo) {
				return dbo;
			}
		};
	}

	/**
	 * Bridge: converts a backup that stores the submission content in the XStream blob into a new row that stores it in the
	 * columns and the document. The status fields and accessor changes in the blob are ignored, they are migrated with their
	 * own tables. Removed in the next stack with the blob, see PLFM-10061.
	 * 
	 * @throws IllegalArgumentException If the backup {@link #hasNewColumnsAndLegacyBlob(DBOSubmission) has both} the blob and
	 *                                  the columns written from it
	 */
	static DBOSubmission fromLegacyBackup(DBOSubmission backup) {
		if (hasNewColumnsAndLegacyBlob(backup)) {
			throw new IllegalArgumentException("Both 'submissionSerialized' and the submission document or file handle columns are not null");
		}
		Submission blob = readLegacyBlob(backup.getSubmissionSerialized());
		return deduplicateInFavorOfBackupColumns(backup, blob);
	}

	/**
	 * Only a stack that already wrote the document and file handle columns from the blob can produce a backup with both, so
	 * such a backup is a double migration and is rejected.
	 */
	private static boolean hasNewColumnsAndLegacyBlob(DBOSubmission backup) {
		return backup.getSubmissionSerialized() != null && (backup.getSubmissionJson() != null
				|| backup.getDucFileHandleId() != null || backup.getIrbFileHandleId() != null || backup.getAttachmentsJson() != null
				|| backup.getSchemaDataAttachmentsJson() != null);
	}

	/**
	 * The blob repeats the values of the columns that sat beside it. Each value is stored once: the backup columns win, and
	 * the blob only supplies the fields that had no column, plus the access requirement version when its column is not set.
	 * The research project id is the id of the snapshot in the blob, which the document keeps whole.
	 */
	private static DBOSubmission deduplicateInFavorOfBackupColumns(DBOSubmission backup, Submission blob) {
		DBOSubmission dbo = new DBOSubmission();
		dbo.setId(backup.getId());
		dbo.setAccessRequirementId(backup.getAccessRequirementId());
		dbo.setAccessRequirementVersion(backup.getAccessRequirementVersion() != null ? backup.getAccessRequirementVersion()
				: blob.getAccessRequirementVersion());
		dbo.setDataAccessRequestId(backup.getDataAccessRequestId());
		dbo.setCreatedBy(backup.getCreatedBy());
		dbo.setCreatedOn(backup.getCreatedOn());
		dbo.setEtag(backup.getEtag());
		dbo.setResearchProjectId(blob.getResearchProjectSnapshot() == null ? null
				: Long.parseLong(blob.getResearchProjectSnapshot().getId()));
		dbo.setDucFileHandleId(SubmissionUtils.toLong(blob.getDucFileHandleId()));
		dbo.setIrbFileHandleId(SubmissionUtils.toLong(blob.getIrbFileHandleId()));
		dbo.setAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(blob.getAttachments()));
		dbo.setSubmissionJson(JDOSecondaryPropertyUtils.createJSONFromObject(SubmissionUtils.toContent(blob)));
		return dbo;
	}

	/**
	 * Bridge: decodes the XStream blob. Removed in the next stack, see PLFM-10061.
	 */
	private static Submission readLegacyBlob(byte[] serialized) {
		try {
			return (Submission) JDOSecondaryPropertyUtils.decompressObject(SUBMISSION_XSTREAM, serialized);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
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
