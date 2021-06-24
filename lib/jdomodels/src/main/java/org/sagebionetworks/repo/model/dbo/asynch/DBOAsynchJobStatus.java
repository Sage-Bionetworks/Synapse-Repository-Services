package org.sagebionetworks.repo.model.dbo.asynch;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ASYNCH_JOB_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CANCELING;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CHANGED_ON;
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
			new FieldColumn("requestHash", COL_ASYNCH_JOB_REQUEST_HASH) };

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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((canceling == null) ? 0 : canceling.hashCode());
		result = prime * result + ((changedOn == null) ? 0 : changedOn.hashCode());
		result = prime * result + ((errorDetails == null) ? 0 : errorDetails.hashCode());
		result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((exception == null) ? 0 : exception.hashCode());
		result = prime * result + ((jobId == null) ? 0 : jobId.hashCode());
		result = prime * result + ((jobState == null) ? 0 : jobState.hashCode());
		result = prime * result + ((jobType == null) ? 0 : jobType.hashCode());
		result = prime * result + ((progressCurrent == null) ? 0 : progressCurrent.hashCode());
		result = prime * result + ((progressMessage == null) ? 0 : progressMessage.hashCode());
		result = prime * result + ((progressTotal == null) ? 0 : progressTotal.hashCode());
		result = prime * result + ((requestBody == null) ? 0 : requestBody.hashCode());
		result = prime * result + ((requestHash == null) ? 0 : requestHash.hashCode());
		result = prime * result + ((responseBody == null) ? 0 : responseBody.hashCode());
		result = prime * result + ((runtimeMS == null) ? 0 : runtimeMS.hashCode());
		result = prime * result + ((startedByUserId == null) ? 0 : startedByUserId.hashCode());
		result = prime * result + ((startedOn == null) ? 0 : startedOn.hashCode());
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
		DBOAsynchJobStatus other = (DBOAsynchJobStatus) obj;
		if (canceling == null) {
			if (other.canceling != null)
				return false;
		} else if (!canceling.equals(other.canceling))
			return false;
		if (changedOn == null) {
			if (other.changedOn != null)
				return false;
		} else if (!changedOn.equals(other.changedOn))
			return false;
		if (errorDetails == null) {
			if (other.errorDetails != null)
				return false;
		} else if (!errorDetails.equals(other.errorDetails))
			return false;
		if (errorMessage == null) {
			if (other.errorMessage != null)
				return false;
		} else if (!errorMessage.equals(other.errorMessage))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (exception == null) {
			if (other.exception != null)
				return false;
		} else if (!exception.equals(other.exception))
			return false;
		if (jobId == null) {
			if (other.jobId != null)
				return false;
		} else if (!jobId.equals(other.jobId))
			return false;
		if (jobState != other.jobState)
			return false;
		if (jobType != other.jobType)
			return false;
		if (progressCurrent == null) {
			if (other.progressCurrent != null)
				return false;
		} else if (!progressCurrent.equals(other.progressCurrent))
			return false;
		if (progressMessage == null) {
			if (other.progressMessage != null)
				return false;
		} else if (!progressMessage.equals(other.progressMessage))
			return false;
		if (progressTotal == null) {
			if (other.progressTotal != null)
				return false;
		} else if (!progressTotal.equals(other.progressTotal))
			return false;
		if (requestBody == null) {
			if (other.requestBody != null)
				return false;
		} else if (!requestBody.equals(other.requestBody))
			return false;
		if (requestHash == null) {
			if (other.requestHash != null)
				return false;
		} else if (!requestHash.equals(other.requestHash))
			return false;
		if (responseBody == null) {
			if (other.responseBody != null)
				return false;
		} else if (!responseBody.equals(other.responseBody))
			return false;
		if (runtimeMS == null) {
			if (other.runtimeMS != null)
				return false;
		} else if (!runtimeMS.equals(other.runtimeMS))
			return false;
		if (startedByUserId == null) {
			if (other.startedByUserId != null)
				return false;
		} else if (!startedByUserId.equals(other.startedByUserId))
			return false;
		if (startedOn == null) {
			if (other.startedOn != null)
				return false;
		} else if (!startedOn.equals(other.startedOn))
			return false;
		return true;
	}

}
