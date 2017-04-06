package org.sagebionetworks.repo.model.dbo.asynch;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.asynch.DBOAsynchJobStatus.JobState;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Basic implementation for a job status CRUD.
 * 
 * @author jmhill
 *
 */
public class AsynchJobStatusDAOImpl implements AsynchronousJobStatusDAO {
	
	private static final String SQL_SELECT_BY_HASH_ETAG_STARTED_BY = "SELECT * FROM "+ASYNCH_JOB_STATUS+" WHERE "+COL_ASYNCH_JOB_REQUEST_HASH+" = ? AND "+COL_ASYNCH_JOB_STARTED_BY+" = ? AND "+COL_ASYNCH_JOB_STATE+" = ? LIMIT 5";
	private static final String SQL_UPDATE_PROGRESS = "UPDATE " + ASYNCH_JOB_STATUS + " SET " + COL_ASYNCH_JOB_PROGRESS_CURRENT + " = ?, "
			+ COL_ASYNCH_JOB_PROGRESS_TOTAL + " = ?, " + COL_ASYNCH_JOB_PROGRESS_MESSAGE + " = ?, " + COL_ASYNCH_JOB_CHANGED_ON
			+ " = ?  WHERE " + COL_ASYNCH_JOB_ID + " = ? AND " + COL_ASYNCH_JOB_STATE + " = 'PROCESSING'";
	private static final String SQL_SET_FAILED = "UPDATE " + ASYNCH_JOB_STATUS + " SET " + COL_ASYNCH_JOB_EXCEPTION + " = ?, "
			+ COL_ASYNCH_JOB_ERROR_MESSAGE + " = ?, " + COL_ASYNCH_JOB_ERROR_DETAILS + " = ?, " + COL_ASYNCH_JOB_STATE + " = ?, "
			+ COL_ASYNCH_JOB_ETAG + " = ?, " + COL_ASYNCH_JOB_CHANGED_ON + " = ?  WHERE " + COL_ASYNCH_JOB_ID + " = ?";
	private static final String SQL_SET_CANCELING = "UPDATE " + ASYNCH_JOB_STATUS + " SET " + COL_ASYNCH_JOB_CANCELING + " = true WHERE "
			+ COL_ASYNCH_JOB_ID + " = ?";

	private static final String TRUNCATE_ALL = "DELETE FROM "+ASYNCH_JOB_STATUS+" WHERE "+COL_ASYNCH_JOB_ID+" > -1";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	RowMapper<DBOAsynchJobStatus> statusRowMapper = new DBOAsynchJobStatus().getTableMapping();


	@Override
	public AsynchronousJobStatus getJobStatus(String jobId) throws DatastoreException, NotFoundException {
		if(jobId == null){
			throw new IllegalArgumentException("Job id cannot be null");
		}
		Long jobIdLong;
		try {
			jobIdLong = Long.parseLong(jobId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Cannot read job id: "+e.getMessage());
		}
		DBOAsynchJobStatus dbo =  basicDao.getObjectByPrimaryKey(DBOAsynchJobStatus.class, new SinglePrimaryKeySqlParameterSource(jobIdLong));
		return AsynchJobStatusUtils.createDTOFromDBO(dbo);
	}

	@WriteTransaction
	@Override
	public void truncateAllAsynchTableJobStatus() {
		jdbcTemplate.update(TRUNCATE_ALL);
	}
	
	/**
	 * This is set to Propagation.REQUIRES_NEW because the transaction
	 * must be committed before a message is sent to the worker.
	 */
	@NewWriteTransaction
	@Override
	public AsynchronousJobStatus startJob(Long userId, AsynchronousRequestBody body) {
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(body == null) throw new IllegalArgumentException("body cannot be null");
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		long now = System.currentTimeMillis();
		status.setStartedByUserId(userId);
		status.setJobId(idGenerator.generateNewId(IdType.ASYNCH_JOB_STATUS_ID).toString());
		status.setEtag(UUID.randomUUID().toString());
		status.setChangedOn(new Date(now));
		status.setStartedOn(new Date(now));
		status.setJobState(AsynchJobState.PROCESSING);
		status.setJobCanceling(false);
		status.setRuntimeMS(0L);
		status.setRequestBody(body);
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		dbo = basicDao.createNew(dbo);
		return AsynchJobStatusUtils.createDTOFromDBO(dbo);
	}

	@NewWriteTransaction
	@Override
	public void updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage) {
		if(jobId == null) throw new IllegalArgumentException("JobId cannot be null");
		progressMessage = AsynchJobStatusUtils.truncateMessageStringIfNeeded(progressMessage);
		long now = System.currentTimeMillis();
		jdbcTemplate.update(SQL_UPDATE_PROGRESS, progressCurrent, progressTotal, progressMessage, now, jobId);
	}

	@WriteTransaction
	@Override
	public String setJobFailed(String jobId, Throwable error) {
		if(jobId == null) throw new IllegalArgumentException("JobId cannot be null");
		if(error == null) throw new IllegalArgumentException("Error cannot be null");
		String newEtag = UUID.randomUUID().toString();
		String errorMessage = AsynchJobStatusUtils.truncateMessageStringIfNeeded(error.getMessage());
		byte[] errorDetails = AsynchJobStatusUtils.stringToBytes(ExceptionUtils.getStackTrace(error));
		long now = System.currentTimeMillis();
		String exceptionClass = null;
		try {
			if(error.getClass().getConstructor(String.class)!=null){
				exceptionClass = error.getClass().getName();
			}
		} catch (NoSuchMethodException e) {
			// ignore
		} catch (SecurityException e) {
			// ignore
		}
		jdbcTemplate.update(SQL_SET_FAILED, exceptionClass, errorMessage, errorDetails, JobState.FAILED.name(), newEtag, now, jobId);
		return newEtag;
	}

	@WriteTransaction
	@Override
	public void setJobCanceling(String jobId) {
		if (jobId == null)
			throw new IllegalArgumentException("JobId cannot be null");
		jdbcTemplate.update(SQL_SET_CANCELING, jobId);
	}

	@WriteTransaction
	@Override
	public String setComplete(String jobId, AsynchronousResponseBody body,
			String requestHash) throws DatastoreException, NotFoundException {
		if(jobId == null) throw new IllegalArgumentException("JobId cannot be null");
		if(body == null) throw new IllegalArgumentException("Body cannot be null");
		// Get the current value for this job
		DBOAsynchJobStatus dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOAsynchJobStatus.class, new SinglePrimaryKeySqlParameterSource(jobId));
		// Calculate the runtime
		long now = System.currentTimeMillis();
		long runtimeMS = now - dbo.getStartedOn().getTime();
		dbo.setRuntimeMS(runtimeMS);
		String newEtag = UUID.randomUUID().toString();
		dbo.setEtag(newEtag);
		dbo.setProgressMessage("Complete");
		dbo.setChangedOn(new Date(now));
		dbo.setException(null);
		dbo.setErrorDetails(null);
		dbo.setErrorMessage(null);
		dbo.setJobState(JobState.COMPLETE);
		dbo.setProgressCurrent(dbo.getProgressTotal());
		dbo.setResponseBody(AsynchJobStatusUtils.getBytesForResponseBody(dbo.getJobType(), body));
		dbo.setRequestHash(requestHash);
		basicDao.update(dbo);
		return newEtag;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO#findCompletedJobStatus(java.lang.String, java.lang.String, java.lang.Long)
	 */
	@Override
	public List<AsynchronousJobStatus> findCompletedJobStatus(String requestHash, Long userId) {
		if(requestHash == null){
			throw new IllegalArgumentException("requestHash cannot be null");
		}
		if(userId == null){
			throw new IllegalArgumentException("userId cannot be null");
		}
		List<DBOAsynchJobStatus> dbos = jdbcTemplate.query(SQL_SELECT_BY_HASH_ETAG_STARTED_BY, statusRowMapper, requestHash, userId, AsynchJobState.COMPLETE.name() );
		List<AsynchronousJobStatus> results = new LinkedList<AsynchronousJobStatus>();
		for(DBOAsynchJobStatus dbo: dbos){
			results.add(AsynchJobStatusUtils.createDTOFromDBO(dbo));
		}
		return results;
	}
	
}
