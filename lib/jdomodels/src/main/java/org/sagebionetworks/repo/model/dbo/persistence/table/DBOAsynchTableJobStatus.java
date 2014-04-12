package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = ASYNCH_TABLE_JOB_STATUS)
public class DBOAsynchTableJobStatus implements MigratableDatabaseObject<DBOAsynchTableJobStatus, DBOAsynchTableJobStatus> {
	
	/**
	 * State of a job
	 */
	public enum JobState{
		PROCESSING,
		FAILED,
		COMPLETE,
	}
	
	/**
	 * Type of a job	 *
	 */
	public enum JobType {
		UPLOAD,
		DOWNLOAD
	}
	

	private static TableMapping<DBOAsynchTableJobStatus> tableMapping = AutoTableMapping.create(DBOAsynchTableJobStatus.class);
	
	@Field(name = COL_ASYNCH_JOB_ID, nullable = false, primary=true, backupId = true)
	private Long jobId;
	
	@Field(name = COL_ASYNCH_JOB_STATE, nullable = false)
	private JobState jobState;
	
	@Field(name = COL_ASYNCH_JOB_TYPE, nullable = false)
	private JobType jobType;

	
	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public JobState getJobState() {
		return jobState;
	}

	public void setJobState(JobState jobState) {
		this.jobState = jobState;
	}

	public JobType getJobType() {
		return jobType;
	}

	public void setJobType(JobType jobType) {
		this.jobType = jobType;
	}

	@Override
	public TableMapping<DBOAsynchTableJobStatus> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ASYNCH_TABLE_JOB_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOAsynchTableJobStatus, DBOAsynchTableJobStatus> getTranslator() {
		return new MigratableTableTranslation<DBOAsynchTableJobStatus, DBOAsynchTableJobStatus>(){

			@Override
			public DBOAsynchTableJobStatus createDatabaseObjectFromBackup(
					DBOAsynchTableJobStatus backup) {
				return backup;
			}

			@Override
			public DBOAsynchTableJobStatus createBackupFromDatabaseObject(
					DBOAsynchTableJobStatus dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOAsynchTableJobStatus> getBackupClass() {
		return DBOAsynchTableJobStatus.class;
	}

	@Override
	public Class<? extends DBOAsynchTableJobStatus> getDatabaseObjectClass() {
		return DBOAsynchTableJobStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}
