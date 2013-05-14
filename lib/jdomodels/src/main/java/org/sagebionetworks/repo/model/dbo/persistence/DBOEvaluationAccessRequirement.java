package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_EVALUATION_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_EVALUATION_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOEvaluationAccessRequirement implements MigratableDatabaseObject<DBOEvaluationAccessRequirement, DBOEvaluationAccessRequirement> {

	private Long evaluationId;
	private Long accessRequirementId;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("evaluationId", COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID, true),
		new FieldColumn("accessRequirementId", COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID).withIsBackupId(true)
	};

	@Override
	public TableMapping<DBOEvaluationAccessRequirement> getTableMapping() {

		return new TableMapping<DBOEvaluationAccessRequirement>(){

			@Override
			public DBOEvaluationAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOEvaluationAccessRequirement nar = new DBOEvaluationAccessRequirement();
				nar.setNodeId(rs.getLong(COL_EVALUATION_ACCESS_REQUIREMENT_EVALUATION_ID));
				nar.setAccessRequirementId(rs.getLong(COL_EVALUATION_ACCESS_REQUIREMENT_REQUIREMENT_ID));
				return nar;
			}

			@Override
			public String getTableName() {
				return TABLE_EVALUATION_ACCESS_REQUIREMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_EVALUATION_ACCESS_REQUIREMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOEvaluationAccessRequirement> getDBOClass() {
				return DBOEvaluationAccessRequirement.class;
			}};
	}

	public Long getNodeId() {
		return evaluationId;
	}

	public void setNodeId(Long evaluationId) {
		this.evaluationId = evaluationId;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessRequirementId == null) ? 0 : accessRequirementId
						.hashCode());
		result = prime * result
				+ ((evaluationId == null) ? 0 : evaluationId.hashCode());
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
		DBOEvaluationAccessRequirement other = (DBOEvaluationAccessRequirement) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (evaluationId == null) {
			if (other.evaluationId != null)
				return false;
		} else if (!evaluationId.equals(other.evaluationId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOEvaluationAccessRequirement [evaluationId=" + evaluationId
				+ ", accessRequirementId=" + accessRequirementId + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.EVALUATION_ACCESS_REQUIREMENT;
	}

	@Override
	public MigratableTableTranslation<DBOEvaluationAccessRequirement, DBOEvaluationAccessRequirement> getTranslator() {

		return new MigratableTableTranslation<DBOEvaluationAccessRequirement, DBOEvaluationAccessRequirement>(){

			@Override
			public DBOEvaluationAccessRequirement createDatabaseObjectFromBackup(
					DBOEvaluationAccessRequirement backup) {
				return backup;
			}

			@Override
			public DBOEvaluationAccessRequirement createBackupFromDatabaseObject(
					DBOEvaluationAccessRequirement dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOEvaluationAccessRequirement> getBackupClass() {
		return DBOEvaluationAccessRequirement.class;
	}

	@Override
	public Class<? extends DBOEvaluationAccessRequirement> getDatabaseObjectClass() {
		return DBOEvaluationAccessRequirement.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}


}
