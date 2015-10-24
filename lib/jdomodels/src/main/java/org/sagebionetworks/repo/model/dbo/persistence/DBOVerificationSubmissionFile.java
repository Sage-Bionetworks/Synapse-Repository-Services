package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION_FILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_STATE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.VERIFICATION_SUBMISSION_ID;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_VERIFICATION_SUBMISSION_FILE)
public class DBOVerificationSubmissionFile implements
		MigratableDatabaseObject<DBOVerificationSubmissionFile, DBOVerificationSubmissionFile> {

	// TODO do secondary tables have backupId (foreign key) = true???
	@Field(name = VERIFICATION_STATE_ID, backupId = true, primary = false, nullable = false)
	@ForeignKey(table = TABLE_VERIFICATION_SUBMISSION, field = VERIFICATION_SUBMISSION_ID, cascadeDelete = true)
	private Long id;
	
	@Field(name = VERIFICATION_STATE_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_FILES, field = COL_FILES_ID, cascadeDelete = true)
	private Long fileHandleId;

	private static TableMapping<DBOVerificationSubmissionFile> TABLE_MAPPING = AutoTableMapping.create(DBOVerificationSubmissionFile.class);

	@Override
	public TableMapping<DBOVerificationSubmissionFile> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_FILE;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationSubmissionFile, DBOVerificationSubmissionFile> getTranslator() {
		return new MigratableTableTranslation<DBOVerificationSubmissionFile, DBOVerificationSubmissionFile>() {

			@Override
			public DBOVerificationSubmissionFile createDatabaseObjectFromBackup(
					DBOVerificationSubmissionFile backup) {
				return backup;
			}

			@Override
			public DBOVerificationSubmissionFile createBackupFromDatabaseObject(
					DBOVerificationSubmissionFile dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOVerificationSubmissionFile> getBackupClass() {
		return DBOVerificationSubmissionFile.class;
	}

	@Override
	public Class<? extends DBOVerificationSubmissionFile> getDatabaseObjectClass() {
		return DBOVerificationSubmissionFile.class;
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

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
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
		DBOVerificationSubmissionFile other = (DBOVerificationSubmissionFile) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
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
		return "DBOVerificationSubmissionFile [id=" + id + ", fileHandleId="
				+ fileHandleId + "]";
	}


}
