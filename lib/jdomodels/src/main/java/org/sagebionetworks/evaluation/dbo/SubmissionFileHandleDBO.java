package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBFILE_FILE_HANDLE_ID;
import static org.sagebionetworks.evaluation.dbo.DBOConstants.PARAM_SUBFILE_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBFILE_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBFILE_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_SUBFILE;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBFILE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object for associating a File handle with a Submission
 * 
 * @author bkng
 */
public class SubmissionFileHandleDBO implements MigratableDatabaseObject<SubmissionFileHandleDBO, SubmissionFileHandleDBO> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_SUBFILE_SUBMISSION_ID, COL_SUBFILE_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn(PARAM_SUBFILE_FILE_HANDLE_ID, COL_SUBFILE_FILE_HANDLE_ID, true).withHasFileHandleRef(true)
	};
	
	private static final TableMapping<SubmissionFileHandleDBO> TABLE_MAPPING = new TableMapping<SubmissionFileHandleDBO>() {
		// Map a result set to this object
		public SubmissionFileHandleDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
			SubmissionFileHandleDBO subFile = new SubmissionFileHandleDBO();
			subFile.setSubmissionId(rs.getLong(COL_SUBFILE_SUBMISSION_ID));
			subFile.setFileHandleId(rs.getLong(COL_SUBFILE_FILE_HANDLE_ID));
			return subFile;
		}

		public String getTableName() {
			return TABLE_SUBFILE;
		}

		public String getDDLFileName() {
			return DDL_FILE_SUBFILE;
		}

		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		public Class<? extends SubmissionFileHandleDBO> getDBOClass() {
			return SubmissionFileHandleDBO.class;
		}
	};

	public TableMapping<SubmissionFileHandleDBO> getTableMapping() {
		return TABLE_MAPPING;
	}
	
	private Long submissionId;
	private Long fileHandleId;

	public Long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
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
				+ ((submissionId == null) ? 0 : submissionId.hashCode());
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
		SubmissionFileHandleDBO other = (SubmissionFileHandleDBO) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (submissionId == null) {
			if (other.submissionId != null)
				return false;
		} else if (!submissionId.equals(other.submissionId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubmissionFileHandleDBO [submissionId=" + submissionId
				+ ", fileHandleId=" + fileHandleId + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SUBMISSION_FILE;
	}

	@Override
	public MigratableTableTranslation<SubmissionFileHandleDBO, SubmissionFileHandleDBO> getTranslator() {
		return new BasicMigratableTableTranslation<SubmissionFileHandleDBO>();
	}

	@Override
	public Class<? extends SubmissionFileHandleDBO> getBackupClass() {
		return SubmissionFileHandleDBO.class;
	}

	@Override
	public Class<? extends SubmissionFileHandleDBO> getDatabaseObjectClass() {
		return SubmissionFileHandleDBO.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	
}
