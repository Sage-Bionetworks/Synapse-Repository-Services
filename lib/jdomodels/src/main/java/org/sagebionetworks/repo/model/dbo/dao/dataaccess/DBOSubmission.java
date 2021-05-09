package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;
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

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubmission implements MigratableDatabaseObject<DBOSubmission, DBOSubmission>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID),
			new FieldColumn("dataAccessRequestId", COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID),
			new FieldColumn("createdBy", COL_DATA_ACCESS_SUBMISSION_CREATED_BY),
			new FieldColumn("createdOn", COL_DATA_ACCESS_SUBMISSION_CREATED_ON),
			new FieldColumn("etag", COL_DATA_ACCESS_SUBMISSION_ETAG).withIsEtag(true),
			new FieldColumn("submissionSerialized", COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED).withHasFileHandleRef(true),
			new FieldColumn("researchProjectId", COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID)
		};

	private Long id;
	private Long accessRequirementId;
	private Long dataAccessRequestId;
	private Long createdBy;
	private Long createdOn;
	private String etag;
	private byte[] submissionSerialized;
	private Long researchProjectId;

	@Override
	public String toString() {
		return "DBOSubmission [id=" + id + ", accessRequirementId=" + accessRequirementId + ", dataAccessRequestId="
				+ dataAccessRequestId + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", etag=" + etag
				+ ", submissionSerialized=" + Arrays.toString(submissionSerialized) + ", researchProjectId="
				+ researchProjectId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((dataAccessRequestId == null) ? 0 : dataAccessRequestId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((researchProjectId == null) ? 0 : researchProjectId.hashCode());
		result = prime * result + Arrays.hashCode(submissionSerialized);
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
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (dataAccessRequestId == null) {
			if (other.dataAccessRequestId != null)
				return false;
		} else if (!dataAccessRequestId.equals(other.dataAccessRequestId))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (researchProjectId == null) {
			if (other.researchProjectId != null)
				return false;
		} else if (!researchProjectId.equals(other.researchProjectId))
			return false;
		if (!Arrays.equals(submissionSerialized, other.submissionSerialized))
			return false;
		return true;
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
		return new BasicMigratableTableTranslation<DBOSubmission>();
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
		return list;
	}

}
