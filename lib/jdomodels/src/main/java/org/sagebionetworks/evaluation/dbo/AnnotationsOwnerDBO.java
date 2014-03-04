package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_EVALID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_SUBID;
import static org.sagebionetworks.repo.model.query.SQLConstants.DDL_FILE_SUBSTATUS_ANNO_OWNER;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_ANNO_OWNER;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class AnnotationsOwnerDBO implements DatabaseObject<AnnotationsOwnerDBO>{
	
	private Long submissionId;
	private Long evaluationId;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("submissionId", COL_SUBSTATUS_ANNO_SUBID, true),
		new FieldColumn("evaluationId", COL_SUBSTATUS_ANNO_EVALID),
		};

	@Override
	public TableMapping<AnnotationsOwnerDBO> getTableMapping() {
		return new TableMapping<AnnotationsOwnerDBO>() {
			
			@Override
			public AnnotationsOwnerDBO mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				AnnotationsOwnerDBO result = new AnnotationsOwnerDBO();
				result.setSubmissionId(rs.getLong(COL_SUBSTATUS_ANNO_SUBID));
				result.setEvaluationId(rs.getLong(COL_SUBSTATUS_ANNO_EVALID));
				return result;
			}
			
			@Override
			public String getTableName() {
				return TABLE_SUBSTATUS_ANNO_OWNER;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_FILE_SUBSTATUS_ANNO_OWNER;
			}
			
			@Override
			public Class<? extends AnnotationsOwnerDBO> getDBOClass() {
				return AnnotationsOwnerDBO.class;
			}
		};
	}

	public Long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
	}

	public Long getEvaluationId() {
		return evaluationId;
	}

	public void setEvaluationId(Long evaluationId) {
		this.evaluationId = evaluationId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((evaluationId == null) ? 0 : evaluationId.hashCode());
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
		AnnotationsOwnerDBO other = (AnnotationsOwnerDBO) obj;
		if (evaluationId == null) {
			if (other.evaluationId != null)
				return false;
		} else if (!evaluationId.equals(other.evaluationId))
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
		return "DBOSubmissionStatusAnnotationOwner [submissionId="
				+ submissionId + ", evaluationId=" + evaluationId + "]";
	}

}
