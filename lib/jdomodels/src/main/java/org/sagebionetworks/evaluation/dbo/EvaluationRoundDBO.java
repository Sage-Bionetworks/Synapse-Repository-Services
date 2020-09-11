package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ROUND_ETAG;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ROUND_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ROUND_LIMITS_JSON;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ROUND_ROUND_END;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_EVALUATION_ROUND_ROUND_START;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_LIMITS;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ROUND_END;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ROUND_START;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_EVALUATION_ROUND;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION_ROUND;

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

public class EvaluationRoundDBO implements MigratableDatabaseObject<EvaluationRoundDBO, EvaluationRoundDBO>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_EVALUATION_ROUND_ID, COL_EVALUATION_ROUND_ID, true).withIsBackupId(true),
			new FieldColumn(PARAM_EVALUATION_ROUND_ETAG, COL_EVALUATION_ROUND_ETAG).withIsEtag(true),
			new FieldColumn(PARAM_EVALUATION_ROUND_EVALUATION_ID, COL_EVALUATION_ROUND_EVALUATION_ID),
			new FieldColumn(PARAM_EVALUATION_ROUND_ROUND_START, COL_EVALUATION_ROUND_ROUND_START),
			new FieldColumn(PARAM_EVALUATION_ROUND_ROUND_END, COL_EVALUATION_ROUND_ROUND_END),
			new FieldColumn(PARAM_EVALUATION_ROUND_LIMITS_JSON, COL_EVALUATION_ROUND_LIMITS)
	};
	private static final BasicMigratableTableTranslation<EvaluationRoundDBO> BASIC_MIGRATION_TABLE_TRANSLATION = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private Long evaluationId;
	private Long roundStart;
	private Long roundEnd;
	private String limitsJson;

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.EVALUATION_ROUND;
	}

	@Override
	public MigratableTableTranslation<EvaluationRoundDBO, EvaluationRoundDBO> getTranslator() {
		return BASIC_MIGRATION_TABLE_TRANSLATION;
	}

	@Override
	public Class<? extends EvaluationRoundDBO> getBackupClass() {
		return EvaluationRoundDBO.class;
	}

	@Override
	public Class<? extends EvaluationRoundDBO> getDatabaseObjectClass() {
		return EvaluationRoundDBO.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public TableMapping<EvaluationRoundDBO> getTableMapping() {
		return new TableMapping<EvaluationRoundDBO>() {
			@Override
			public String getTableName() {
				return TABLE_EVALUATION_ROUND;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_EVALUATION_ROUND;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends EvaluationRoundDBO> getDBOClass() {
				return EvaluationRoundDBO.class;
			}

			@Override
			public EvaluationRoundDBO mapRow(ResultSet resultSet, int rowNum) throws SQLException {
				EvaluationRoundDBO evaluationRoundDBO = new EvaluationRoundDBO();
				evaluationRoundDBO.setId(resultSet.getLong(COL_EVALUATION_ROUND_ID));
				evaluationRoundDBO.setEtag(resultSet.getString(COL_EVALUATION_ROUND_ETAG));
				evaluationRoundDBO.setEvaluationId(resultSet.getLong(COL_EVALUATION_ROUND_EVALUATION_ID));
				evaluationRoundDBO.setRoundStart(resultSet.getLong(COL_EVALUATION_ROUND_ROUND_START));
				if (resultSet.wasNull()){
					throw new IllegalStateException("roundStart should never be null");
				}
				evaluationRoundDBO.setRoundEnd(resultSet.getLong(COL_EVALUATION_ROUND_ROUND_END));
				if (resultSet.wasNull()){
					throw new IllegalStateException("roundEnd should never be null");
				}
				evaluationRoundDBO.setLimitsJson(resultSet.getString(COL_EVALUATION_ROUND_LIMITS));
				return evaluationRoundDBO;
			}
		};
	}


	/////////////////////////////
	// Field Getters and Setters
	////////////////////////////


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

	public long getRoundStart() {
		return roundStart;
	}

	public void setRoundStart(long roundStart) {
		this.roundStart = roundStart;
	}

	public long getRoundEnd() {
		return roundEnd;
	}

	public void setRoundEnd(long roundEnd) {
		this.roundEnd = roundEnd;
	}

	public String getLimitsJson() {
		return limitsJson;
	}

	public void setLimitsJson(String limitsJson) {
		this.limitsJson = limitsJson;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EvaluationRoundDBO that = (EvaluationRoundDBO) o;
		return roundStart == that.roundStart &&
				roundEnd == that.roundEnd &&
				Objects.equals(id, that.id) &&
				Objects.equals(etag, that.etag) &&
				Objects.equals(evaluationId, that.evaluationId) &&
				Objects.equals(limitsJson, that.limitsJson);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, etag, evaluationId, roundStart, roundEnd, limitsJson);
	}

	@Override
	public String toString() {
		return "EvaluationRoundDBO{" +
				"id=" + id +
				", etag='" + etag + '\'' +
				", evaluationId=" + evaluationId +
				", roundStart=" + roundStart +
				", roundEnd=" + roundEnd +
				", limitsJson='" + limitsJson + '\'' +
				'}';
	}
}
