package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.evaluation.dbo.DBOConstants.*;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.sagebionetworks.repo.model.TaggableEntity;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Database object for associating a File handle with a Submission
 * 
 * @author bkng
 */
public class SubmissionFileHandleDBO implements DatabaseObject<SubmissionFileHandleDBO>, TaggableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_SUBFILE_SUBMISSION_ID, COL_SUBFILE_SUBMISSION_ID, true),
			new FieldColumn(PARAM_SUBFILE_FILE_HANDLE_ID, COL_SUBFILE_FILE_HANDLE_ID, true)
			};

	public TableMapping<SubmissionFileHandleDBO> getTableMapping() {
		return new TableMapping<SubmissionFileHandleDBO>() {
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
	
}
