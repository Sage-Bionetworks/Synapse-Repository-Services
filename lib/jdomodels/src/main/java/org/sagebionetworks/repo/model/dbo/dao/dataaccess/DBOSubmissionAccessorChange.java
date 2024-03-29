package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESSOR_ID;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_ACCESSORS_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESS_TYPE;

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

/**
 * DBO for storing the accessor changes for a given submission
 */
public class DBOSubmissionAccessorChange implements MigratableDatabaseObject<DBOSubmissionAccessorChange, DBOSubmissionAccessorChange> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			// This is a sub-table of submission, so its backup ID is the submission id.
			new FieldColumn("submissionId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("accessorId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESSOR_ID, true),
			new FieldColumn("accessType", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESS_TYPE)
		};
	
	private static final TableMapping<DBOSubmissionAccessorChange> TABLE_MAPPING = new TableMapping<DBOSubmissionAccessorChange>() {

		@Override
		public DBOSubmissionAccessorChange mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSubmissionAccessorChange dbo = new DBOSubmissionAccessorChange();
			dbo.setSubmissionId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_SUBMISSION_ID));
			dbo.setAccessorId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESSOR_ID));
			dbo.setAccessType(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESS_TYPE));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_DATA_ACCESS_SUBMISSION_ACCESSORS_CHANGES;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOSubmissionAccessorChange> getDBOClass() {
			return DBOSubmissionAccessorChange.class;
		}
		
	};
	
	private static final MigratableTableTranslation<DBOSubmissionAccessorChange, DBOSubmissionAccessorChange> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	private Long submissionId;
	private Long accessorId;
	private String accessType;

	public DBOSubmissionAccessorChange() {
	}

	public Long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
	}

	public Long getAccessorId() {
		return accessorId;
	}

	public void setAccessorId(Long accessorId) {
		this.accessorId = accessorId;
	}
	
	public String getAccessType() {
		return accessType;
	}
	
	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	@Override
	public TableMapping<DBOSubmissionAccessorChange> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGE;
	}

	@Override
	public MigratableTableTranslation<DBOSubmissionAccessorChange, DBOSubmissionAccessorChange> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOSubmissionAccessorChange> getBackupClass() {
		return DBOSubmissionAccessorChange.class;
	}

	@Override
	public Class<? extends DBOSubmissionAccessorChange> getDatabaseObjectClass() {
		return DBOSubmissionAccessorChange.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessType, accessorId, submissionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOSubmissionAccessorChange other = (DBOSubmissionAccessorChange) obj;
		return Objects.equals(accessType, other.accessType) && Objects.equals(accessorId, other.accessorId)
				&& Objects.equals(submissionId, other.submissionId);
	}

	@Override
	public String toString() {
		return "DBOSubmissionAccessorChange [submissionId=" + submissionId + ", accessorId=" + accessorId + ", accessType=" + accessType
				+ "]";
	}

}
