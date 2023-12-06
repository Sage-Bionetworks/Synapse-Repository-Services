package org.sagebionetworks.repo.model.dbo.asynch;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ASYNCH_JOB_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CANCELING;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CHANGED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CONTEXT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_EXCEPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_TOTAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_REQUEST_BODY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_REQUEST_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_RESPONSE_BODY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_RUNTIME_MS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_ASYNCH_JOB_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Database object for a asynchronous table job.
 * 
 * @author John
 *
 */

public class DBOAsynchJobStatus implements DatabaseObject<DBOAsynchJobStatus> {
	
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
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("jobId", COL_ASYNCH_JOB_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_ASYNCH_JOB_ETAG).withIsEtag(true),
			new FieldColumn("jobState", COL_ASYNCH_JOB_STATE),
			new FieldColumn("jobType", COL_ASYNCH_JOB_TYPE),
			new FieldColumn("canceling", COL_ASYNCH_JOB_CANCELING),
			new FieldColumn("exception", COL_ASYNCH_JOB_EXCEPTION),
			new FieldColumn("errorMessage", COL_ASYNCH_JOB_ERROR_MESSAGE),
			new FieldColumn("errorDetails", COL_ASYNCH_JOB_ERROR_DETAILS),
			new FieldColumn("progressCurrent", COL_ASYNCH_JOB_PROGRESS_CURRENT),
			new FieldColumn("progressTotal", COL_ASYNCH_JOB_PROGRESS_TOTAL),
			new FieldColumn("progressMessage", COL_ASYNCH_JOB_PROGRESS_MESSAGE),
			new FieldColumn("startedOn", COL_ASYNCH_JOB_STARTED_ON),
			new FieldColumn("startedByUserId", COL_ASYNCH_JOB_STARTED_BY),
			new FieldColumn("changedOn", COL_ASYNCH_JOB_CHANGED_ON),
			new FieldColumn("requestBody", COL_ASYNCH_JOB_REQUEST_BODY),
			new FieldColumn("responseBody", COL_ASYNCH_JOB_RESPONSE_BODY),
			new FieldColumn("runtimeMS", COL_ASYNCH_JOB_RUNTIME_MS),
			new FieldColumn("requestHash", COL_ASYNCH_JOB_REQUEST_HASH),
			new FieldColumn("context", COL_ASYNCH_JOB_CONTEXT)};

	private Long jobId;
	private String etag;
	private JobState jobState;
	private AsynchJobType jobType;
	private Boolean canceling;
	private String exception;
	private String errorMessage;
	private String errorDetails;
	private Long progressCurrent;
	private Long progressTotal;
	private String progressMessage;
	private Timestamp startedOn;
	private Long startedByUserId;
	private Timestamp changedOn;
	private String requestBody;
	private String responseBody;
	private Long runtimeMS;
	private String requestHash;
	private String context;

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getJobState() {
		return jobState.name();
	}

	public void setJobState(JobState jobState) {
		this.jobState = jobState;
	}

	public String getJobType() {
		return jobType.name();
	}

	public void setJobType(AsynchJobType jobType) {
		this.jobType = jobType;
	}

	public Boolean getCanceling() {
		return canceling;
	}

	public void setCanceling(Boolean canceling) {
		this.canceling = canceling;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
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

	public Timestamp getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Timestamp startedOn) {
		this.startedOn = startedOn;
	}

	public Long getStartedByUserId() {
		return startedByUserId;
	}

	public void setStartedByUserId(long startedByUserId) {
		this.startedByUserId = startedByUserId;
	}

	public Timestamp getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Timestamp now) {
		this.changedOn = now;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	public Long getRuntimeMS() {
		return runtimeMS;
	}

	public void setRuntimeMS(Long runtimeMS) {
		this.runtimeMS = runtimeMS;
	}

	public String getRequestHash() {
		return requestHash;
	}

	public void setRequestHash(String requestHash) {
		this.requestHash = requestHash;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public static void setTableMapping(TableMapping<DBOAsynchJobStatus> tableMapping) {
		DBOAsynchJobStatus.TABLE_MAPPING = tableMapping;
	}

	@Override
	public TableMapping<DBOAsynchJobStatus> getTableMapping() {
		return TABLE_MAPPING;
	}
	
	private static TableMapping<DBOAsynchJobStatus> TABLE_MAPPING = new TableMapping<DBOAsynchJobStatus>() {

		@Override
		public DBOAsynchJobStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAsynchJobStatus dbo = new DBOAsynchJobStatus();
			dbo.setJobId(rs.getLong(COL_ASYNCH_JOB_ID));
			dbo.setEtag(rs.getString(COL_ASYNCH_JOB_ETAG));
			dbo.setJobState(JobState.valueOf(rs.getString(COL_ASYNCH_JOB_STATE)));
			dbo.setJobType(AsynchJobType.valueOf(rs.getString(COL_ASYNCH_JOB_TYPE)));
			dbo.setCanceling(rs.getBoolean(COL_ASYNCH_JOB_CANCELING));
			dbo.setException(rs.getString(COL_ASYNCH_JOB_EXCEPTION));
			dbo.setErrorMessage(rs.getString(COL_ASYNCH_JOB_ERROR_MESSAGE));
			dbo.setErrorDetails(rs.getString(COL_ASYNCH_JOB_ERROR_DETAILS));
			dbo.setProgressCurrent(rs.getLong(COL_ASYNCH_JOB_PROGRESS_CURRENT));
			if (rs.wasNull()) {
				dbo.setProgressCurrent(null);
			}
			dbo.setProgressTotal(rs.getLong(COL_ASYNCH_JOB_PROGRESS_TOTAL));
			if (rs.wasNull()) {
				dbo.setProgressTotal(null);
			}
			dbo.setProgressMessage(rs.getString(COL_ASYNCH_JOB_PROGRESS_MESSAGE));
			dbo.setStartedOn(rs.getTimestamp(COL_ASYNCH_JOB_STARTED_ON));
			dbo.setStartedByUserId(rs.getLong(COL_ASYNCH_JOB_STARTED_BY));
			dbo.setChangedOn(rs.getTimestamp(COL_ASYNCH_JOB_CHANGED_ON));
			dbo.setRequestBody(rs.getString(COL_ASYNCH_JOB_REQUEST_BODY));
			dbo.setResponseBody(rs.getString(COL_ASYNCH_JOB_RESPONSE_BODY));
			dbo.setRuntimeMS(rs.getLong(COL_ASYNCH_JOB_RUNTIME_MS));
			dbo.setRequestHash(rs.getString(COL_ASYNCH_JOB_REQUEST_HASH));
			dbo.setContext(rs.getString(COL_ASYNCH_JOB_CONTEXT));
			return dbo;
		}

		@Override
		public String getTableName() {
			return ASYNCH_JOB_STATUS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}
		
		@Override
		public String getDDLFileName() {
			return DDL_ASYNCH_JOB_STATUS;
		}

		@Override
		public Class<? extends DBOAsynchJobStatus> getDBOClass() {
			return DBOAsynchJobStatus.class;
		}
	};

	@Override
	public int hashCode() {
		return Objects.hash(canceling, changedOn, context, errorDetails, errorMessage, etag, exception, jobId, jobState,
				jobType, progressCurrent, progressMessage, progressTotal, requestBody, requestHash, responseBody,
				runtimeMS, startedByUserId, startedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOAsynchJobStatus other = (DBOAsynchJobStatus) obj;
		return Objects.equals(canceling, other.canceling) && Objects.equals(changedOn, other.changedOn)
				&& Objects.equals(context, other.context) && Objects.equals(errorDetails, other.errorDetails)
				&& Objects.equals(errorMessage, other.errorMessage) && Objects.equals(etag, other.etag)
				&& Objects.equals(exception, other.exception) && Objects.equals(jobId, other.jobId)
				&& jobState == other.jobState && jobType == other.jobType
				&& Objects.equals(progressCurrent, other.progressCurrent)
				&& Objects.equals(progressMessage, other.progressMessage)
				&& Objects.equals(progressTotal, other.progressTotal) && Objects.equals(requestBody, other.requestBody)
				&& Objects.equals(requestHash, other.requestHash) && Objects.equals(responseBody, other.responseBody)
				&& Objects.equals(runtimeMS, other.runtimeMS) && Objects.equals(startedByUserId, other.startedByUserId)
				&& Objects.equals(startedOn, other.startedOn);
	}

}
