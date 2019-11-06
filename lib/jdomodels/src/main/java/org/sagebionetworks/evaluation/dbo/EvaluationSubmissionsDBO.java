package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION_SUBMISSIONS;

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

@Table(name = TABLE_EVALUATION_SUBMISSIONS)
public class EvaluationSubmissionsDBO implements MigratableDatabaseObject<EvaluationSubmissionsDBO, EvaluationSubmissionsDBO> {
	
	@Field(name = COL_EVALUATION_SUBMISSIONS_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = COL_EVALUATION_SUBMISSIONS_ETAG, backupId = false, etag = true, varchar = 256, primary = false, nullable = false)
	private String etag;
	
	@Field(name = COL_EVALUATION_SUBMISSIONS_EVAL_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_EVALUATION, field = COL_EVALUATION_ID, cascadeDelete = true)
	private Long evaluationId;
	
	private static final TableMapping<EvaluationSubmissionsDBO> TABLE_MAPPING = AutoTableMapping.create(EvaluationSubmissionsDBO.class);

	@Override
	public TableMapping<EvaluationSubmissionsDBO> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.EVALUATION_SUBMISSIONS;
	}

	@Override
	public MigratableTableTranslation<EvaluationSubmissionsDBO, EvaluationSubmissionsDBO> getTranslator() {
		return new BasicMigratableTableTranslation<EvaluationSubmissionsDBO>();
	}

	@Override
	public Class<? extends EvaluationSubmissionsDBO> getBackupClass() {
		return EvaluationSubmissionsDBO.class;
	}

	@Override
	public Class<? extends EvaluationSubmissionsDBO> getDatabaseObjectClass() {
		return EvaluationSubmissionsDBO.class;
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

	public Long getEvaluationId() {
		return evaluationId;
	}

	public void setEvaluationId(Long evaluationId) {
		this.evaluationId = evaluationId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((evaluationId == null) ? 0 : evaluationId.hashCode());
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
		EvaluationSubmissionsDBO other = (EvaluationSubmissionsDBO) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (evaluationId == null) {
			if (other.evaluationId != null)
				return false;
		} else if (!evaluationId.equals(other.evaluationId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EvaluationSubmissionsDBO [id=" + id + ", etag=" + etag
				+ ", evaluationId=" + evaluationId + "]";
	}
	

}
