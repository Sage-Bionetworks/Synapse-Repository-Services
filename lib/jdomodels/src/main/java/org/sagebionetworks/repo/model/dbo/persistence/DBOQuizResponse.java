package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@Table(name = SqlConstants.TABLE_QUIZ_RESPONSE)
public class DBOQuizResponse implements MigratableDatabaseObject<DBOQuizResponse, DBOQuizResponse> {
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	// TODO define the foreign key constraomt
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY, backupId = false, primary = false, nullable = false)
	private Long createdBy;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY, backupId = false, primary = false, nullable = false)
	private Long createOn;
	private Long quizId;
	private Long score;
	private Boolean passed;
	private byte[] serialized;

	private static TableMapping<DBOQuizResponse> tableMapping = AutoTableMapping.create(DBOQuizResponse.class);

	@Override
	public TableMapping<DBOQuizResponse> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.QUIZ_RESPONSE;
	}

	@Override
	public MigratableTableTranslation<DBOQuizResponse, DBOQuizResponse> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOQuizResponse, DBOQuizResponse>() {

			@Override
			public DBOQuizResponse createDatabaseObjectFromBackup(DBOQuizResponse backup) {
				return backup;
			}

			@Override
			public DBOQuizResponse createBackupFromDatabaseObject(DBOQuizResponse dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOQuizResponse> getBackupClass() {
		return DBOQuizResponse.class;
	}

	@Override
	public Class<? extends DBOQuizResponse> getDatabaseObjectClass() {
		return DBOQuizResponse.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreateOn() {
		return createOn;
	}

	public void setCreateOn(Long createOn) {
		this.createOn = createOn;
	}

	public Long getQuizId() {
		return quizId;
	}

	public void setQuizId(Long quizId) {
		this.quizId = quizId;
	}

	public Long getScore() {
		return score;
	}

	public void setScore(Long score) {
		this.score = score;
	}

	public Boolean getPassed() {
		return passed;
	}

	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

	public byte[] getSerialized() {
		return serialized;
	}

	public void setSerialized(byte[] serialized) {
		this.serialized = serialized;
	}

	public static void setTableMapping(TableMapping<DBOQuizResponse> tableMapping) {
		DBOQuizResponse.tableMapping = tableMapping;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createOn == null) ? 0 : createOn.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((passed == null) ? 0 : passed.hashCode());
		result = prime * result + ((quizId == null) ? 0 : quizId.hashCode());
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		result = prime * result + Arrays.hashCode(serialized);
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
		DBOQuizResponse other = (DBOQuizResponse) obj;
		if (createOn == null) {
			if (other.createOn != null)
				return false;
		} else if (!createOn.equals(other.createOn))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (passed == null) {
			if (other.passed != null)
				return false;
		} else if (!passed.equals(other.passed))
			return false;
		if (quizId == null) {
			if (other.quizId != null)
				return false;
		} else if (!quizId.equals(other.quizId))
			return false;
		if (score == null) {
			if (other.score != null)
				return false;
		} else if (!score.equals(other.score))
			return false;
		if (!Arrays.equals(serialized, other.serialized))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOQuizResponse [id=" + id + ", createdBy=" + createdBy
				+ ", createOn=" + createOn + ", quizId=" + quizId + ", score="
				+ score + ", passed=" + passed + ", serialized="
				+ Arrays.toString(serialized) + "]";
	}
	
	
}
