package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import org.sagebionetworks.util.TemporaryCode;

@Table(name = SqlConstants.TABLE_QUIZ_RESPONSE)
public class DBOQuizResponse implements MigratableDatabaseObject<DBOQuizResponse, DBOQuizResponse> {
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_ETAG, etag = true, nullable = false)
	private String etag;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long createdBy;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;
	
	@Field(name = SqlConstants.COL_QUIZ_RESPONSE_REVOKED_ON, backupId = false, primary = false, nullable = true)
	private Long revokedOn;

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

	private static final TableMapping<DBOQuizResponse> TABLE_MAPPING = AutoTableMapping.create(DBOQuizResponse.class);

	private static final BasicMigratableTableTranslation<DBOQuizResponse> TABLE_MIGRATION = new BasicMigratableTableTranslation<DBOQuizResponse>() {
		
		@TemporaryCode(author = "marco", comment = "Remove after PLFM-8365 has be released to production")
		public DBOQuizResponse createDatabaseObjectFromBackup(DBOQuizResponse backup) {
			if (backup.getEtag() == null) {
				backup.setEtag(UUID.randomUUID().toString());
			}
			return backup;
		};
		
	};
	
	@Override
	public TableMapping<DBOQuizResponse> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.QUIZ_RESPONSE;
	}

	@Override
	public MigratableTableTranslation<DBOQuizResponse, DBOQuizResponse> getTranslator() {
		return TABLE_MIGRATION;
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
	

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
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

	public Long getRevokedOn() {
		return revokedOn;
	}

	public void setRevokedOn(Long revokedOn) {
		this.revokedOn = revokedOn;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(passingRecord);
		result = prime * result + Arrays.hashCode(serialized);
		result = prime * result + Objects.hash(createdBy, createdOn, etag, id, passed, quizId, revokedOn, score);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOQuizResponse)) {
			return false;
		}
		DBOQuizResponse other = (DBOQuizResponse) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(passed, other.passed) && Arrays.equals(passingRecord, other.passingRecord)
				&& Objects.equals(quizId, other.quizId) && Objects.equals(revokedOn, other.revokedOn) && Objects.equals(score, other.score)
				&& Arrays.equals(serialized, other.serialized);
	}

	@Override
	public String toString() {
		return "DBOQuizResponse [id=" + id + ", etag=" + etag + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", revokedOn="
				+ revokedOn + ", quizId=" + quizId + ", score=" + score + ", passed=" + passed + "]";
	}
	
	
}
