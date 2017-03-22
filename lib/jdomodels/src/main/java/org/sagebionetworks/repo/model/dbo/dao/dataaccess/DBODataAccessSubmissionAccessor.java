package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_SUBMISSION_ACCESSOR;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_SUBMISSION_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODataAccessSubmissionAccessor implements MigratableDatabaseObject<DBODataAccessSubmissionAccessor, DBODataAccessSubmissionAccessor>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("submissionId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("accessorId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID, true)
		};

	private Long submissionId;
	private Long accessorId;

	@Override
	public String toString() {
		return "DBODataAccessSubmissionAccessor [submissionId=" + submissionId + ", accessorId=" + accessorId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessorId == null) ? 0 : accessorId.hashCode());
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
		DBODataAccessSubmissionAccessor other = (DBODataAccessSubmissionAccessor) obj;
		if (accessorId == null) {
			if (other.accessorId != null)
				return false;
		} else if (!accessorId.equals(other.accessorId))
			return false;
		if (submissionId == null) {
			if (other.submissionId != null)
				return false;
		} else if (!submissionId.equals(other.submissionId))
			return false;
		return true;
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

	@Override
	public TableMapping<DBODataAccessSubmissionAccessor> getTableMapping() {
		return new TableMapping<DBODataAccessSubmissionAccessor>(){

			@Override
			public DBODataAccessSubmissionAccessor mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODataAccessSubmissionAccessor dbo = new DBODataAccessSubmissionAccessor();
				dbo.setAccessorId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID));
				dbo.setSubmissionId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_SUBMISSION_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_ACCESS_SUBMISSION_ACCESSOR;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODataAccessSubmissionAccessor> getDBOClass() {
				return DBODataAccessSubmissionAccessor.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION_ACCESSOR;
	}

	@Override
	public MigratableTableTranslation<DBODataAccessSubmissionAccessor, DBODataAccessSubmissionAccessor> getTranslator() {
		return new MigratableTableTranslation<DBODataAccessSubmissionAccessor, DBODataAccessSubmissionAccessor>(){

			@Override
			public DBODataAccessSubmissionAccessor createDatabaseObjectFromBackup(
					DBODataAccessSubmissionAccessor backup) {
				return backup;
			}

			@Override
			public DBODataAccessSubmissionAccessor createBackupFromDatabaseObject(DBODataAccessSubmissionAccessor dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBODataAccessSubmissionAccessor> getBackupClass() {
		return DBODataAccessSubmissionAccessor.class;
	}

	@Override
	public Class<? extends DBODataAccessSubmissionAccessor> getDatabaseObjectClass() {
		return DBODataAccessSubmissionAccessor.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
