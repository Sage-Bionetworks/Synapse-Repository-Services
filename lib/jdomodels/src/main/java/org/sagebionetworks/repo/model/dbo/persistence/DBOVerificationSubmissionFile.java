package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_FILEHANDLEID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VERIFICATION_FILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_FILE;

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

public class DBOVerificationSubmissionFile implements
		MigratableDatabaseObject<DBOVerificationSubmissionFile, DBOVerificationSubmissionFile> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("verificationId", COL_VERIFICATION_FILE_VERIFICATION_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("fileHandleId", COL_VERIFICATION_FILE_FILEHANDLEID),
	};

	private Long verificationId;
	private Long fileHandleId;

	@Override
	public TableMapping<DBOVerificationSubmissionFile> getTableMapping() {
		return new TableMapping<DBOVerificationSubmissionFile>() {
			
			@Override
			public DBOVerificationSubmissionFile mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOVerificationSubmissionFile dbo = new DBOVerificationSubmissionFile();
				dbo.setVerificationId(rs.getLong(COL_VERIFICATION_FILE_VERIFICATION_ID));
				dbo.setFileHandleId(rs.getLong(COL_VERIFICATION_FILE_FILEHANDLEID));
				return null;
			}
			
			@Override
			public String getTableName() {
				return TABLE_VERIFICATION_FILE;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_VERIFICATION_FILE;
			}
			
			@Override
			public Class<? extends DBOVerificationSubmissionFile> getDBOClass() {
				return DBOVerificationSubmissionFile.class;
			}
		};
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
		return Objects.hash(fileHandleId, verificationId);
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
		return Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(verificationId, other.verificationId);
	}

	@Override
	public String toString() {
		return "DBOVerificationSubmissionFile [verificationId=" + verificationId + ", fileHandleId=" + fileHandleId
				+ "]";
	}


}
