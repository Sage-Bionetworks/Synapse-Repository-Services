package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_CREATED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_SUBMISSION_CONTRIBUTOR;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION_CONTRIBUTOR;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class SubmissionContributorDBO
		implements MigratableDatabaseObject<SubmissionContributorDBO, SubmissionContributorDBO> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_SUBMISSION_CONTRIBUTOR_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_SUBMISSION_CONTRIBUTOR_ETAG).withIsEtag(true),
			new FieldColumn("submissionId", COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID),
			new FieldColumn("principalId", COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID),
			new FieldColumn("createdOn", COL_SUBMISSION_CONTRIBUTOR_CREATED_ON)};

	private Long id;
	private String etag;
	private Long submissionId;
	private Long principalId;
	private Date createdOn;

	@Override
	public TableMapping<SubmissionContributorDBO> getTableMapping() {
		return new TableMapping<SubmissionContributorDBO>() {
			
			@Override
			public SubmissionContributorDBO mapRow(ResultSet rs, int rowNum) throws SQLException {
				SubmissionContributorDBO dbo = new SubmissionContributorDBO();
				dbo.setId(rs.getLong(COL_SUBMISSION_CONTRIBUTOR_ID));
				dbo.setEtag(rs.getString(COL_SUBMISSION_CONTRIBUTOR_ETAG));
				dbo.setSubmissionId(rs.getLong(COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID));
				dbo.setPrincipalId(rs.getLong(COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID));
				dbo.setCreatedOn(rs.getDate(COL_SUBMISSION_CONTRIBUTOR_CREATED_ON));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_SUBMISSION_CONTRIBUTOR;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_SUBMISSION_CONTRIBUTOR;
			}
			
			@Override
			public Class<? extends SubmissionContributorDBO> getDBOClass() {
				return SubmissionContributorDBO.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SUBMISSION_CONTRIBUTOR;
	}

	@Override
	public MigratableTableTranslation<SubmissionContributorDBO, SubmissionContributorDBO> getTranslator() {
		return new BasicMigratableTableTranslation<SubmissionContributorDBO>();
	}

	@Override
	public Class<? extends SubmissionContributorDBO> getBackupClass() {
		return SubmissionContributorDBO.class;
	}

	@Override
	public Class<? extends SubmissionContributorDBO> getDatabaseObjectClass() {
		return SubmissionContributorDBO.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
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

	public Long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, etag, id, principalId, submissionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubmissionContributorDBO other = (SubmissionContributorDBO) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(principalId, other.principalId)
				&& Objects.equals(submissionId, other.submissionId);
	}

	@Override
	public String toString() {
		return "SubmissionContributorDBO [id=" + id + ", etag=" + etag + ", submissionId=" + submissionId
				+ ", principalId=" + principalId + ", createdOn=" + createdOn + "]";
	}

}
