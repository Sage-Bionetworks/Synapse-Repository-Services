package org.sagebionetworks.repo.model.dbo.asynch;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ASYNCH_JOB_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CHANGED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_TOTAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STATE;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.asynch.DBOAsynchJobStatus.JobState;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Basic implementation for a job status CRUD.
 * 
 * @author jmhill
 *
 */
public class AsynchJobStatusDAOImpl implements AsynchronousJobStatusDAO {
	
	private static final String SQL_UPDATE_PROGRESS = "UPDATE "+ASYNCH_JOB_STATUS+" SET "+COL_ASYNCH_JOB_PROGRESS_CURRENT+" = ?, "+COL_ASYNCH_JOB_PROGRESS_TOTAL+" = ?, "+COL_ASYNCH_JOB_PROGRESS_MESSAGE+" = ?, "+COL_ASYNCH_JOB_ETAG+" = ?, "+COL_ASYNCH_JOB_CHANGED_ON+" = ?  WHERE "+COL_ASYNCH_JOB_ID+" = ?";
	private static final String SQL_SET_FAILED = "UPDATE "+ASYNCH_JOB_STATUS+" SET "+COL_ASYNCH_JOB_ERROR_MESSAGE+" = ?, "+COL_ASYNCH_JOB_ERROR_DETAILS+" = ?, "+COL_ASYNCH_JOB_STATE+" = ?, "+COL_ASYNCH_JOB_ETAG+" = ?, "+COL_ASYNCH_JOB_CHANGED_ON+" = ?  WHERE "+COL_ASYNCH_JOB_ID+" = ?";

	private static final String TRUNCATE_ALL = "DELETE FROM "+ASYNCH_JOB_STATUS+" WHERE "+COL_ASYNCH_JOB_ID+" > -1";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;


	@Override
	public <T extends AsynchronousJobStatus> T getJobStatus(String jobId, Class<? extends T> clazz) throws DatastoreException, NotFoundException {
		DBOAsynchJobStatus dbo =  basicDao.getObjectByPrimaryKey(DBOAsynchJobStatus.class, new SinglePrimaryKeySqlParameterSource(jobId));
		return AsynchJobStatusUtils.createDTOFromDBO(dbo, clazz);
	}

	@Override
	public void truncateAllAsynchTableJobStatus() {
		jdbcTemplate.update(TRUNCATE_ALL);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends AsynchronousJobStatus> T startJob(T status) {
		if(status == null) throw new IllegalArgumentException("Status cannot be null");
		if(status.getStartedByUserId() == null) throw new IllegalArgumentException("Status.startedByUserId cannot be null");
		long now = System.currentTimeMillis();
		status.setJobId(idGenerator.generateNewId(TYPE.ASYNCH_JOB_STATUS_ID).toString());
		status.setEtag(UUID.randomUUID().toString());
		status.setChangedOn(new Date(now));
		status.setStartedOn(new Date(now));
		status.setJobState(AsynchJobState.PROCESSING);
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		dbo = basicDao.createNew(dbo);
		return (T) AsynchJobStatusUtils.createDTOFromDBO(dbo, status.getClass());
	}

	@Override
	public String updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage) {
		if(jobId == null) throw new IllegalArgumentException("JobId cannot be null");
		String newEtag = UUID.randomUUID().toString();
		progressMessage = AsynchJobStatusUtils.truncateMessageStringIfNeeded(progressMessage);
		long now = System.currentTimeMillis();
		jdbcTemplate.update(SQL_UPDATE_PROGRESS, progressCurrent, progressTotal, progressMessage, newEtag, now, jobId);
		return newEtag;
	}

	@Override
	public String setJobFailed(String jobId, Throwable error) {
		if(jobId == null) throw new IllegalArgumentException("JobId cannot be null");
		if(error == null) throw new IllegalArgumentException("Error cannot be null");
		String newEtag = UUID.randomUUID().toString();
		String errorMessage = AsynchJobStatusUtils.truncateMessageStringIfNeeded(error.getMessage());
		byte[] errorDetails = AsynchJobStatusUtils.stringToBytes(ExceptionUtils.getStackTrace(error));
		long now = System.currentTimeMillis();
		jdbcTemplate.update(SQL_SET_FAILED, errorMessage, errorDetails, JobState.FAILED.name(), newEtag, now, jobId);
		return newEtag;
	}	

}
