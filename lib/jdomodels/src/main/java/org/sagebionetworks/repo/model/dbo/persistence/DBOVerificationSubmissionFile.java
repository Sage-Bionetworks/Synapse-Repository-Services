package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_FILEHANDLEID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.FK_VERIFICATION_FILE_FILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.FK_VERIFICATION_FILE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_FILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;

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

@Table(name = TABLE_VERIFICATION_FILE)
public class DBOVerificationSubmissionFile implements
		MigratableDatabaseObject<DBOVerificationSubmissionFile, DBOVerificationSubmissionFile> {
	
	@Field(name = COL_VERIFICATION_FILE_VERIFICATION_ID, backupId = true, primary = true, nullable = false)
	@ForeignKey(table = TABLE_VERIFICATION_SUBMISSION, field = COL_VERIFICATION_SUBMISSION_ID, cascadeDelete = true, name = FK_VERIFICATION_FILE_VERIFICATION_ID)
	private Long verificationId;
	
	@Field(name = COL_VERIFICATION_FILE_FILEHANDLEID, backupId = false, primary = true, nullable = false, hasFileHandleRef = true)
	@ForeignKey(table = TABLE_FILES, field = COL_FILES_ID, cascadeDelete = false, name = FK_VERIFICATION_FILE_FILE_ID)
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
		return new BasicMigratableTableTranslation<DBOVerificationSubmissionFile>();
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

	public Long getVerificationId() {
		return verificationId;
	}

	public void setVerificationId(Long verificationId) {
		this.verificationId = verificationId;
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
		result = prime * result
				+ ((verificationId == null) ? 0 : verificationId.hashCode());
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
		if (verificationId == null) {
			if (other.verificationId != null)
				return false;
		} else if (!verificationId.equals(other.verificationId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOVerificationSubmissionFile [verificationId="
				+ verificationId + ", fileHandleId=" + fileHandleId + "]";
	}


}
