package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.Arrays;
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
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@Table(name = SqlConstants.TABLE_QUIZ_RESPONSE)
public class DBOQuizResponse implements MigratableDatabaseObject<DBOQuizResponse, DBOQuizResponse> {
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long createdBy;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;

	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_QUIZ_ID, backupId = false, primary = false, nullable = false)
	private Long quizId;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_SCORE, backupId = false, primary = false, nullable = false)
	private Long score;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_PASSED, backupId = false, primary = false, nullable = false)
	private Boolean passed;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_SERIALIZED, backupId = false, primary = false, nullable = false, serialized="mediumblob")
	private byte[] serialized;

	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_PASSING_RECORD, backupId = false, primary = false, nullable = false, serialized="mediumblob")
	private byte[] passingRecord;

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
		return new BasicMigratableTableTranslation<DBOQuizResponse>();
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
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

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
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

	public byte[] getPassingRecord() {
		return passingRecord;
	}

	public void setPassingRecord(byte[] passingRecord) {
		this.passingRecord = passingRecord;
	}

	public static void setTableMapping(TableMapping<DBOQuizResponse> tableMapping) {
		DBOQuizResponse.tableMapping = tableMapping;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((passed == null) ? 0 : passed.hashCode());
		result = prime * result + Arrays.hashCode(passingRecord);
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
		if (!Arrays.equals(passingRecord, other.passingRecord))
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
				+ ", createdOn=" + createdOn + ", quizId=" + quizId
				+ ", score=" + score + ", passed=" + passed + ", serialized="
				+ Arrays.toString(serialized) + ", passingRecord="
				+ Arrays.toString(passingRecord) + "]";
	}
	
	
}
