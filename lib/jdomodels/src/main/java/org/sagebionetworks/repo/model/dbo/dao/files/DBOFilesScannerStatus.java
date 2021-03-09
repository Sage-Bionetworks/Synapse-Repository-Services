package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILES_SCANNER_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES_SCANNER_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Used to store the status of the file handle scanner main job
 */
public class DBOFilesScannerStatus implements DatabaseObject<DBOFilesScannerStatus> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_FILES_SCANNER_STATUS_ID, true),
			new FieldColumn("startedOn", COL_FILES_SCANNER_STATUS_STARTED_ON),
			new FieldColumn("updatedOn", COL_FILES_SCANNER_STATUS_UPDATED_ON),
			new FieldColumn("jobsStartedCount", COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT),
			new FieldColumn("jobsCompletedCount", COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT),
			new FieldColumn("scannedAssociationsCount", COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT)
	};

	static final TableMapping<DBOFilesScannerStatus> TABLE_MAPPING = new TableMapping<DBOFilesScannerStatus>() {

		@Override
		public DBOFilesScannerStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOFilesScannerStatus status = new DBOFilesScannerStatus();

			status.setId(rs.getLong(COL_FILES_SCANNER_STATUS_ID));
			status.setStartedOn(rs.getTimestamp(COL_FILES_SCANNER_STATUS_STARTED_ON).toInstant());
			status.setUpdatedOn(rs.getTimestamp(COL_FILES_SCANNER_STATUS_UPDATED_ON).toInstant());
			status.setJobsStartedCount(rs.getLong(COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT));
			status.setJobsCompletedCount(rs.getLong(COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT));
			status.setScannedAssociationsCount(rs.getLong(COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT));

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

	private Long id;
	private Instant startedOn;
	private Instant updatedOn;
	private Long jobsStartedCount;
	private Long jobsCompletedCount;
	private Long scannedAssociationsCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Instant startedOn) {
		this.startedOn = startedOn;
	}

	public Instant getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Instant updatedOn) {
		this.updatedOn = updatedOn;
	}

	public Long getJobsStartedCount() {
		return jobsStartedCount;
	}

	public void setJobsStartedCount(Long jobsStartedCount) {
		this.jobsStartedCount = jobsStartedCount;
	}

	public Long getJobsCompletedCount() {
		return jobsCompletedCount;
	}

	public void setJobsCompletedCount(Long jobsCompletedCount) {
		this.jobsCompletedCount = jobsCompletedCount;
	}

	public Long getScannedAssociationsCount() {
		return scannedAssociationsCount;
	}
	
	public void setScannedAssociationsCount(Long scannedAssociationsCount) {
		this.scannedAssociationsCount = scannedAssociationsCount;
	}
	
	@Override
	public TableMapping<DBOFilesScannerStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, jobsCompletedCount, jobsStartedCount, scannedAssociationsCount, startedOn, updatedOn);
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
		return Objects.equals(id, other.id) && Objects.equals(jobsCompletedCount, other.jobsCompletedCount)
				&& Objects.equals(jobsStartedCount, other.jobsStartedCount)
				&& Objects.equals(scannedAssociationsCount, other.scannedAssociationsCount) && Objects.equals(startedOn, other.startedOn)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

}
