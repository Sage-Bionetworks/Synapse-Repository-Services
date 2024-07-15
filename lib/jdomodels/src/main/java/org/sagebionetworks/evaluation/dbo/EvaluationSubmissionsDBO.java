package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_SUBMISSIONS_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_EVALUATION_SUBMISSIONS;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION_SUBMISSIONS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class EvaluationSubmissionsDBO
		implements MigratableDatabaseObject<EvaluationSubmissionsDBO, EvaluationSubmissionsDBO> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_EVALUATION_SUBMISSIONS_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_EVALUATION_SUBMISSIONS_ETAG).withIsEtag(true),
			new FieldColumn("evaluationId", COL_EVALUATION_SUBMISSIONS_EVAL_ID) };

	private Long id;
	private String etag;
	private Long evaluationId;

	@Override
	public TableMapping<EvaluationSubmissionsDBO> getTableMapping() {
		return new TableMapping<EvaluationSubmissionsDBO>() {

			@Override
			public EvaluationSubmissionsDBO mapRow(ResultSet rs, int rowNum) throws SQLException {
				EvaluationSubmissionsDBO dbo = new EvaluationSubmissionsDBO();
				dbo.setId(rs.getLong(COL_EVALUATION_SUBMISSIONS_ID));
				dbo.setEtag(rs.getString(COL_EVALUATION_SUBMISSIONS_ETAG));
				dbo.setEvaluationId(rs.getLong(COL_EVALUATION_SUBMISSIONS_EVAL_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_EVALUATION_SUBMISSIONS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_EVALUATION_SUBMISSIONS;
			}

			@Override
			public Class<? extends EvaluationSubmissionsDBO> getDBOClass() {
				return EvaluationSubmissionsDBO.class;
			}
		};
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

	public Long getEvaluationId() {
		return evaluationId;
	}

	public void setEvaluationId(Long evaluationId) {
		this.evaluationId = evaluationId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, evaluationId, id);
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
		return Objects.equals(etag, other.etag) && Objects.equals(evaluationId, other.evaluationId)
				&& Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "EvaluationSubmissionsDBO [id=" + id + ", etag=" + etag + ", evaluationId=" + evaluationId + "]";
	}

}
