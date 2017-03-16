package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODataAccessSubmission implements MigratableDatabaseObject<DBODataAccessSubmission, DBODataAccessSubmission>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID),
			new FieldColumn("dataAccessRequestId", COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID),
			new FieldColumn("submittedBy", COL_DATA_ACCESS_SUBMISSION_SUBMITTED_BY),
			new FieldColumn("submittedOn", COL_DATA_ACCESS_SUBMISSION_SUBMITTED_ON),
			new FieldColumn("reviewedBy", COL_DATA_ACCESS_SUBMISSION_REVIEWED_BY),
			new FieldColumn("modifiedOn", COL_DATA_ACCESS_SUBMISSION_MODIFIED_ON),
			new FieldColumn("state", COL_DATA_ACCESS_SUBMISSION_STATE),
			new FieldColumn("etag", COL_DATA_ACCESS_SUBMISSION_ETAG).withIsEtag(true),
			new FieldColumn("submissionSerialized", COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED)
		};

	private Long id;
	private Long accessRequirementId;
	private Long dataAccessRequestId;
	private Long submittedBy;
	private Long submittedOn;
	private Long reviewedBy;
	private Long modifiedOn;
	private DataAccessSubmissionState state;
	private String etag;
	private byte[] submissionSerialized;

	@Override
	public String toString() {
		return "DBODataAccessSubmission [id=" + id + ", accessRequirementId=" + accessRequirementId
				+ ", dataAccessRequestId=" + dataAccessRequestId + ", submittedBy=" + submittedBy + ", submittedOn="
				+ submittedOn + ", reviewedBy=" + reviewedBy + ", reviewedOn=" + modifiedOn + ", state=" + state
				+ ", etag=" + etag + ", submissionSerialized=" + Arrays.toString(submissionSerialized) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((dataAccessRequestId == null) ? 0 : dataAccessRequestId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((reviewedBy == null) ? 0 : reviewedBy.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + Arrays.hashCode(submissionSerialized);
		result = prime * result + ((submittedBy == null) ? 0 : submittedBy.hashCode());
		result = prime * result + ((submittedOn == null) ? 0 : submittedOn.hashCode());
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
		DBODataAccessSubmission other = (DBODataAccessSubmission) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
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
		if (reviewedBy == null) {
			if (other.reviewedBy != null)
				return false;
		} else if (!reviewedBy.equals(other.reviewedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (state != other.state)
			return false;
		if (!Arrays.equals(submissionSerialized, other.submissionSerialized))
			return false;
		if (submittedBy == null) {
			if (other.submittedBy != null)
				return false;
		} else if (!submittedBy.equals(other.submittedBy))
			return false;
		if (submittedOn == null) {
			if (other.submittedOn != null)
				return false;
		} else if (!submittedOn.equals(other.submittedOn))
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

	public Long getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(Long submittedBy) {
		this.submittedBy = submittedBy;
	}

	public Long getSubmittedOn() {
		return submittedOn;
	}

	public void setSubmittedOn(Long submittedOn) {
		this.submittedOn = submittedOn;
	}

	public Long getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(Long reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public Long getReviewedOn() {
		return modifiedOn;
	}

	public void setReviewedOn(Long reviewedOn) {
		this.modifiedOn = reviewedOn;
	}

	public DataAccessSubmissionState getState() {
		return state;
	}

	public void setState(DataAccessSubmissionState state) {
		this.state = state;
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

	@Override
	public TableMapping<DBODataAccessSubmission> getTableMapping() {
		return new TableMapping<DBODataAccessSubmission>(){

			@Override
			public DBODataAccessSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODataAccessSubmission dbo = new DBODataAccessSubmission();
				dbo.setId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ID));
				dbo.setAccessRequirementId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID));
				dbo.setDataAccessRequestId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID));
				dbo.setSubmittedBy(rs.getLong(COL_DATA_ACCESS_SUBMISSION_SUBMITTED_BY));
				dbo.setSubmittedOn(rs.getLong(COL_DATA_ACCESS_SUBMISSION_SUBMITTED_ON));
				dbo.setReviewedBy(rs.getLong(COL_DATA_ACCESS_SUBMISSION_REVIEWED_BY));
				dbo.setReviewedOn(rs.getLong(COL_DATA_ACCESS_SUBMISSION_MODIFIED_ON));
				dbo.setState(DataAccessSubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATE)));
				dbo.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_ETAG));
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
				dbo.setSubmissionSerialized(blob.getBytes(1, (int) blob.length()));
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
			public Class<? extends DBODataAccessSubmission> getDBOClass() {
				return DBODataAccessSubmission.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBODataAccessSubmission, DBODataAccessSubmission> getTranslator() {
		return new MigratableTableTranslation<DBODataAccessSubmission, DBODataAccessSubmission>(){

			@Override
			public DBODataAccessSubmission createDatabaseObjectFromBackup(DBODataAccessSubmission backup) {
				return backup;
			}

			@Override
			public DBODataAccessSubmission createBackupFromDatabaseObject(DBODataAccessSubmission dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBODataAccessSubmission> getBackupClass() {
		return DBODataAccessSubmission.class;
	}

	@Override
	public Class<? extends DBODataAccessSubmission> getDatabaseObjectClass() {
		return DBODataAccessSubmission.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBODataAccessSubmissionAccessor());
		return list;
	}

}
