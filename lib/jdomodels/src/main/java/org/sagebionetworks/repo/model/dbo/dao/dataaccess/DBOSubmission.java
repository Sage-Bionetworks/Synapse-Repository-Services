package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

public class DBOSubmission implements MigratableDatabaseObject<DBOSubmission, DBOSubmission>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID),
			new FieldColumn("accessRequirementVersion", COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_VERSION),
			new FieldColumn("dataAccessRequestId", COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID),
			new FieldColumn("createdBy", COL_DATA_ACCESS_SUBMISSION_CREATED_BY),
			new FieldColumn("createdOn", COL_DATA_ACCESS_SUBMISSION_CREATED_ON),
			new FieldColumn("etag", COL_DATA_ACCESS_SUBMISSION_ETAG).withIsEtag(true),
			new FieldColumn("submissionSerialized", COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED).withHasFileHandleRef(true),
			new FieldColumn("researchProjectId", COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID)
		};

	private Long id;
	private Long accessRequirementId;
	private Long accessRequirementVersion;
	private Long dataAccessRequestId;
	private Long createdBy;
	private Long createdOn;
	private String etag;
	private byte[] submissionSerialized;
	private Long researchProjectId;

	@Override
	public String toString() {
		return "DBOSubmission [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", accessRequirementVersion=" + accessRequirementVersion + ", dataAccessRequestId="
				+ dataAccessRequestId + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", etag=" + etag
				+ ", submissionSerialized=" + Arrays.toString(submissionSerialized) + ", researchProjectId="
				+ researchProjectId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(submissionSerialized);
		result = prime * result + Objects.hash(id, accessRequirementId, accessRequirementVersion, dataAccessRequestId,
				createdBy, createdOn, etag, researchProjectId);
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
				&& Objects.equals(dataAccessRequestId, other.dataAccessRequestId) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Arrays.equals(submissionSerialized, other.submissionSerialized) && Objects.equals(researchProjectId, other.researchProjectId);
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
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
				dbo.setSubmissionSerialized(blob.getBytes(1, (int) blob.length()));
				dbo.setResearchProjectId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID));
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
			@TemporaryCode(author = "sandhra.sokhal@sagebase.org", comment = "Extract access requirement version from blob")
			@Override
			public DBOSubmission createDatabaseObjectFromBackup(DBOSubmission backup) {
				if (backup.getAccessRequirementVersion() == null) {
					Submission submission = SubmissionUtils.readSerializedField(backup.getSubmissionSerialized());
					backup.setAccessRequirementVersion(submission.getAccessRequirementVersion());
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
