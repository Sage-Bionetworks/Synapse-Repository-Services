package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_SUBID;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_SUBSTATUS_ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_ANNO_BLOB;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class AnnotationsBlobDBO implements DatabaseObject<AnnotationsBlobDBO>{
	
	private Long submissionId;
	private byte[] annoBlob;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("submissionId", COL_SUBSTATUS_ANNO_SUBID, true),
		new FieldColumn("annoBlob", COL_SUBSTATUS_ANNO_BLOB),
		};

	@Override
	public TableMapping<AnnotationsBlobDBO> getTableMapping() {
		return new TableMapping<AnnotationsBlobDBO>() {
			
			@Override
			public AnnotationsBlobDBO mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				AnnotationsBlobDBO result = new AnnotationsBlobDBO();
				result.setSubmissionId(rs.getLong(COL_SUBSTATUS_ANNO_SUBID));
				java.sql.Blob blob = rs.getBlob(COL_SUBSTATUS_ANNO_BLOB);
				if (blob != null){
					result.setAnnoBlob(blob.getBytes(1, (int) blob.length()));
				}
				return result;
			}
			
			@Override
			public String getTableName() {
				return TABLE_SUBSTATUS_ANNO_BLOB;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_FILE_SUBSTATUS_ANNO_BLOB;
			}
			
			@Override
			public Class<? extends AnnotationsBlobDBO> getDBOClass() {
				return AnnotationsBlobDBO.class;
			}
		};
	}

	public Long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
	}

	public byte[] getAnnoBlob() {
		return annoBlob;
	}

	public void setAnnoBlob(byte[] annoBlob) {
		this.annoBlob = annoBlob;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(annoBlob);
		result = prime * result + ((submissionId == null) ? 0 : submissionId.hashCode());
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
		AnnotationsBlobDBO other = (AnnotationsBlobDBO) obj;
		if (!Arrays.equals(annoBlob, other.annoBlob))
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
		return "SubmissionStatusAnnotationBlobDBO [id=" + submissionId + ", annoBlob="
				+ Arrays.toString(annoBlob) + "]";
	}

}
