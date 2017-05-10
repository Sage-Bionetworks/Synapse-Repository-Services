package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubmissionAccessor implements MigratableDatabaseObject<DBOSubmissionAccessor, DBOSubmissionAccessor>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ID).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESS_REQUIREMENT_ID, true),
			new FieldColumn("accessorId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID, true),
			new FieldColumn("currentSubmissionId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CURRENT_SUBMISSION_ID),
			new FieldColumn("currentSubmissionId", COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ETAG).withIsEtag(true)
		};

	private Long currentSubmissionId;
	private Long accessRequirementId;
	private Long accessorId;
	private Long id;
	private String etag;

	@Override
	public String toString() {
		return "DBOSubmissionAccessor [currentSubmissionId=" + currentSubmissionId + ", accessRequirementId="
				+ accessRequirementId + ", accessorId=" + accessorId + ", id=" + id + ", etag=" + etag + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((accessorId == null) ? 0 : accessorId.hashCode());
		result = prime * result + ((currentSubmissionId == null) ? 0 : currentSubmissionId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
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
		DBOSubmissionAccessor other = (DBOSubmissionAccessor) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (accessorId == null) {
			if (other.accessorId != null)
				return false;
		} else if (!accessorId.equals(other.accessorId))
			return false;
		if (currentSubmissionId == null) {
			if (other.currentSubmissionId != null)
				return false;
		} else if (!currentSubmissionId.equals(other.currentSubmissionId))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public Long getCurrentSubmissionId() {
		return currentSubmissionId;
	}

	public void setCurrentSubmissionId(Long currentSubmissionId) {
		this.currentSubmissionId = currentSubmissionId;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public Long getAccessorId() {
		return accessorId;
	}

	public void setAccessorId(Long accessorId) {
		this.accessorId = accessorId;
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

	@Override
	public TableMapping<DBOSubmissionAccessor> getTableMapping() {
		return new TableMapping<DBOSubmissionAccessor>(){

			@Override
			public DBOSubmissionAccessor mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubmissionAccessor dbo = new DBOSubmissionAccessor();
				dbo.setId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ID));
				dbo.setAccessorId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID));
				dbo.setAccessRequirementId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESS_REQUIREMENT_ID));
				dbo.setCurrentSubmissionId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CURRENT_SUBMISSION_ID));
				dbo.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ETAG));
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
			public Class<? extends DBOSubmissionAccessor> getDBOClass() {
				return DBOSubmissionAccessor.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION_ACCESSOR;
	}

	@Override
	public MigratableTableTranslation<DBOSubmissionAccessor, DBOSubmissionAccessor> getTranslator() {
		return new MigratableTableTranslation<DBOSubmissionAccessor, DBOSubmissionAccessor>(){

			@Override
			public DBOSubmissionAccessor createDatabaseObjectFromBackup(
					DBOSubmissionAccessor backup) {
				return backup;
			}

			@Override
			public DBOSubmissionAccessor createBackupFromDatabaseObject(DBOSubmissionAccessor dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOSubmissionAccessor> getBackupClass() {
		return DBOSubmissionAccessor.class;
	}

	@Override
	public Class<? extends DBOSubmissionAccessor> getDatabaseObjectClass() {
		return DBOSubmissionAccessor.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
