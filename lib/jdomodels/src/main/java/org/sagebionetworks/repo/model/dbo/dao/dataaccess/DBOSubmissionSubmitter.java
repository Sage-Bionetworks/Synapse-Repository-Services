package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_SUBMISSION_SUBMITTER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubmissionSubmitter implements MigratableDatabaseObject<DBOSubmissionSubmitter, DBOSubmissionSubmitter>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ID).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID, true),
			new FieldColumn("submitterId", COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID, true),
			new FieldColumn("currentSubmissionId", COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID),
			new FieldColumn("currentSubmissionId", COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ETAG).withIsEtag(true)
		};

	private Long currentSubmissionId;
	private Long accessRequirementId;
	private Long submitterId;
	private Long id;
	private String etag;

	@Override
	public String toString() {
		return "DBOSubmissionSubmitter [currentSubmissionId=" + currentSubmissionId + ", accessRequirementId="
				+ accessRequirementId + ", submitterId=" + submitterId + ", id=" + id + ", etag=" + etag + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((submitterId == null) ? 0 : submitterId.hashCode());
		result = prime * result + ((currentSubmissionId == null) ? 0 : currentSubmissionId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		DBOSubmissionSubmitter other = (DBOSubmissionSubmitter) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (submitterId == null) {
			if (other.submitterId != null)
				return false;
		} else if (!submitterId.equals(other.submitterId))
			return false;
		if (currentSubmissionId == null) {
			if (other.currentSubmissionId != null)
				return false;
		} else if (!currentSubmissionId.equals(other.currentSubmissionId))
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
		return true;
	}

	public Long getCurrentSubmissionId() {
		return currentSubmissionId;
	}

	public void setCurrentSubmissionId(Long currentSubmissionId) {
		this.currentSubmissionId = currentSubmissionId;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public Long getSubmitterId() {
		return submitterId;
	}

	public void setSubmitterId(Long accessorId) {
		this.submitterId = accessorId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public TableMapping<DBOSubmissionSubmitter> getTableMapping() {
		return new TableMapping<DBOSubmissionSubmitter>(){

			@Override
			public DBOSubmissionSubmitter mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubmissionSubmitter dbo = new DBOSubmissionSubmitter();
				dbo.setId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ID));
				dbo.setSubmitterId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID));
				dbo.setAccessRequirementId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID));
				dbo.setCurrentSubmissionId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID));
				dbo.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ETAG));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_ACCESS_SUBMISSION_SUBMITTER;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSubmissionSubmitter> getDBOClass() {
				return DBOSubmissionSubmitter.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION_SUBMITTER;
	}

	@Override
	public MigratableTableTranslation<DBOSubmissionSubmitter, DBOSubmissionSubmitter> getTranslator() {
		return new BasicMigratableTableTranslation<DBOSubmissionSubmitter>();
	}

	@Override
	public Class<? extends DBOSubmissionSubmitter> getBackupClass() {
		return DBOSubmissionSubmitter.class;
	}

	@Override
	public Class<? extends DBOSubmissionSubmitter> getDatabaseObjectClass() {
		return DBOSubmissionSubmitter.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
