package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILES_SCANNER_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES_SCANNER_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Used to store the status of the file handle scanner main job
 */
public class DBOFilesScannerStatus implements MigratableDatabaseObject<DBOFilesScannerStatus, DBOFilesScannerStatus> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_FILES_SCANNER_STATUS_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_FILES_SCANNER_STATUS_ETAG).withIsEtag(true),
			new FieldColumn("startedOn", COL_FILES_SCANNER_STATUS_STARTED_ON),
			new FieldColumn("updatedOn", COL_FILES_SCANNER_STATUS_UPDATED_ON),
			new FieldColumn("state", COL_FILES_SCANNER_STATUS_STATE),
			new FieldColumn("jobsCount", COL_FILES_SCANNER_STATUS_JOBS_COUNT) 
	};

	private static final TableMapping<DBOFilesScannerStatus> TABLE_MAPPING = new TableMapping<DBOFilesScannerStatus>() {

		@Override
		public DBOFilesScannerStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOFilesScannerStatus status = new DBOFilesScannerStatus();
			
			status.setId(rs.getLong(COL_FILES_SCANNER_STATUS_ID));
			status.setEtag(rs.getString(COL_FILES_SCANNER_STATUS_ETAG));
			status.setStartedOn(rs.getTimestamp(COL_FILES_SCANNER_STATUS_STARTED_ON));
			status.setUpdatedOn(rs.getTimestamp(COL_FILES_SCANNER_STATUS_UPDATED_ON));
			status.setState(rs.getString(COL_FILES_SCANNER_STATUS_STATE));
			status.setJobsCount(rs.getLong(COL_FILES_SCANNER_STATUS_JOBS_COUNT));
			
			return status;

		}

		@Override
		public String getTableName() {
			return TABLE_FILES_SCANNER_STATUS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILES_SCANNER_STATUS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOFilesScannerStatus> getDBOClass() {
			return DBOFilesScannerStatus.class;
		}
	};

	private static final MigratableTableTranslation<DBOFilesScannerStatus, DBOFilesScannerStatus> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private Timestamp startedOn;
	private Timestamp updatedOn;
	private String state;
	private Long jobsCount;

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

	public Timestamp getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Timestamp startedOn) {
		this.startedOn = startedOn;
	}

	public Timestamp getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Long getJobsCount() {
		return jobsCount;
	}

	public void setJobsCount(Long jobsCount) {
		this.jobsCount = jobsCount;
	}

	@Override
	public TableMapping<DBOFilesScannerStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FILES_SCANNER_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOFilesScannerStatus, DBOFilesScannerStatus> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOFilesScannerStatus> getBackupClass() {
		return DBOFilesScannerStatus.class;
	}

	@Override
	public Class<? extends DBOFilesScannerStatus> getDatabaseObjectClass() {
		return DBOFilesScannerStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, id, jobsCount, startedOn, state, updatedOn);
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
		DBOFilesScannerStatus other = (DBOFilesScannerStatus) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(id, other.id) && Objects.equals(jobsCount, other.jobsCount)
				&& Objects.equals(startedOn, other.startedOn) && Objects.equals(state, other.state)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "DBOFilesScannerStatus [id=" + id + ", etag=" + etag + ", startedOn=" + startedOn + ", updatedOn=" + updatedOn + ", state="
				+ state + ", jobsCount=" + jobsCount + "]";
	}

}
