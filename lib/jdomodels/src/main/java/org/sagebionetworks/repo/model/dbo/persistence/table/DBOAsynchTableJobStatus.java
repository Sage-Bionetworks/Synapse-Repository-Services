package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Database object for a asynchronous table job.
 * 
 * @author John
 *
 */
@Table(name = ASYNCH_TABLE_JOB_STATUS)
public class DBOAsynchTableJobStatus implements MigratableDatabaseObject<DBOAsynchTableJobStatus, DBOAsynchTableJobStatus> {
	
	/**
	 * The maximum number of characters in a string message.
	 */
	public static final int MAX_MESSAGE_CHARS = 3000;
	
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
	
	@Field(name = COL_ASYNCH_TABLE_JOB_ID, nullable = false, primary=true, backupId = true)
	private Long jobId;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_ETAG, nullable = false, etag = true, varchar=256)
	private String etag;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_STATE, nullable = false)
	private JobState jobState;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_TYPE, nullable = false)
	private JobType jobType;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_TABLE_ID, nullable = false, varchar=200)
	private String tableId;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_ERROR_MESSAGE, varchar=MAX_MESSAGE_CHARS, nullable = true)
	private String errorMessage;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_ERROR_DETAILS, blob="mediumblob", nullable = true)
	private byte[] errorDetails;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_PROGRESS_CURRENT, nullable = true)
	private Long progressCurrent;

	@Field(name = COL_ASYNCH_TABLE_JOB_PROGRESS_TOTAL, nullable = true)
	private Long progressTotal;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_PROGRESS_MESSAGE, nullable = true, varchar=MAX_MESSAGE_CHARS)
	private String progressMessage;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_UPLOAD_FILE_HANDLE_ID, nullable = true)
	private Long uploadFileHandleId;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_DOWNLOAD_URL, varchar=3000, nullable = true)
	private String downloadUrl;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_STARTED_ON, nullable = false)
	private Long startedOn;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_STARTED_BY, nullable = false)
	@ForeignKey(table = SqlConstants.TABLE_USER_GROUP, field = SqlConstants.COL_USER_GROUP_ID, cascadeDelete = true)
	private Long startedByUserId;
	
	@Field(name = COL_ASYNCH_TABLE_JOB_CHANGED_ON, nullable = false)
	private Long changedOn;
	
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

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public byte[] getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(byte[] errorDetails) {
		this.errorDetails = errorDetails;
	}

	public Long getProgressCurrent() {
		return progressCurrent;
	}

	public void setProgressCurrent(Long progressCurrent) {
		this.progressCurrent = progressCurrent;
	}

	public Long getProgressTotal() {
		return progressTotal;
	}

	public void setProgressTotal(Long progressTotal) {
		this.progressTotal = progressTotal;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public Long getUploadFileHandleId() {
		return uploadFileHandleId;
	}

	public void setUploadFileHandleId(Long uploadFileHandleId) {
		this.uploadFileHandleId = uploadFileHandleId;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	public Long getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Long changedOn) {
		this.changedOn = changedOn;
	}

	public Long getStartedByUserId() {
		return startedByUserId;
	}

	public void setStartedByUserId(Long startedByUserId) {
		this.startedByUserId = startedByUserId;
	}

	public String getTableId() {
		return tableId;
	}

	public void setTableId(String tableId) {
		this.tableId = tableId;
	}

	public static void setTableMapping(
			TableMapping<DBOAsynchTableJobStatus> tableMapping) {
		DBOAsynchTableJobStatus.tableMapping = tableMapping;
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
