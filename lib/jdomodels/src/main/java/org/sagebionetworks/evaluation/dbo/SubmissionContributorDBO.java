package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_CREATED_ON;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION_CONTRIBUTOR;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_SUBMISSION_CONTRIBUTOR,
 constraints="UNIQUE KEY (`"+COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID+"`, `"+
		 COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID+"`)")
public class SubmissionContributorDBO implements MigratableDatabaseObject<SubmissionContributorDBO, SubmissionContributorDBO> {
	@Field(name = COL_SUBMISSION_CONTRIBUTOR_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = COL_SUBMISSION_CONTRIBUTOR_ETAG, primary = false, nullable = false, etag=true)
	private String etag;
	
	@Field(name = COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID, nullable = false)
	@ForeignKey(table = TABLE_SUBMISSION, field = COL_SUBMISSION_ID, cascadeDelete = true)
	private Long submissionId;
	
	@Field(name = COL_SUBMISSION_CONTRIBUTOR_PRINCIPAL_ID, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = false)
	private Long principalId;
	
	@Field(name = COL_SUBMISSION_CONTRIBUTOR_CREATED_ON, nullable = false)
	private Date createdOn;
	
	private static TableMapping<SubmissionContributorDBO> tableMapping = 
			AutoTableMapping.create(SubmissionContributorDBO.class);

	@Override
	public TableMapping<SubmissionContributorDBO> getTableMapping() {
		return tableMapping;
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
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
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result
				+ ((submissionId == null) ? 0 : submissionId.hashCode());
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
		SubmissionContributorDBO other = (SubmissionContributorDBO) obj;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
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
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (submissionId == null) {
			if (other.submissionId != null)
				return false;
		} else if (!submissionId.equals(other.submissionId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubmissionContributorDBO [id=" + id + ", etag=" + etag
				+ ", submissionId=" + submissionId + ", principalId="
				+ principalId + ", createdOn=" + createdOn + "]";
	}


}
