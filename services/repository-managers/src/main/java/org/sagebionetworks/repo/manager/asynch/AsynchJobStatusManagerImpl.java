package org.sagebionetworks.repo.manager.asynch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Snapshotable;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.asynch.ReadOnlyRequestBody;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class AsynchJobStatusManagerImpl implements AsynchJobStatusManager {
	
	public static final String JOB_TYPE = "JobType";

	public static final String METRIC_NAME = "Job elapse time";

	private static final String CACHED_MESSAGE_TEMPLATE = "Returning a cached job for user: %d, requestHash: %s, and jobId: %s";

	public static final String METRIC_NAMESPACE_PREFIX = "Asynchronous-Jobs-";

	static private Log log = LogFactory.getLog(AsynchJobStatusManagerImpl.class);	
	
	
	private static final String JOB_ABORTED_MESSAGE = "Job aborted because the stack was not in: "+StatusEnum.READ_WRITE;

	@Autowired
	AsynchronousJobStatusDAO asynchJobStatusDao;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	AsynchJobQueuePublisher asynchJobQueuePublisher;
	@Autowired
	JobHashProvider jobHashProvider;
	@Autowired
	ObjectRecordDAO objectRecordDAO;
	@Autowired
	StackConfiguration stackConfig;
	@Autowired
	Consumer cloudeWatch;
	String metricNamespace;
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager#lookupJobStatus(java.lang.String)
	 */
	@Override
	public AsynchronousJobStatus lookupJobStatus(String jobId)
			throws DatastoreException, NotFoundException {
		// Get the status
		AsynchronousJobStatus status = asynchJobStatusDao.getJobStatus(jobId);
		
		// If a job is running and the stack is not in READ-WRITE mode then the job is failed.
		if(AsynchJobState.PROCESSING.equals(status.getJobState())){
			if (! (status.getRequestBody() instanceof ReadOnlyRequestBody)) {
				// Since the job is processing check the state of the stack.
				checkStackReadWrite();
			}
		}
		return status;
	}
	
	@Override
	public AsynchronousJobStatus getJobStatus(UserInfo userInfo, String jobId) throws DatastoreException, NotFoundException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		// Get the status
		AsynchronousJobStatus status = lookupJobStatus(jobId);
		// Only the user that started a job can read it
		if(!authorizationManager.isUserCreatorOrAdmin(userInfo, status.getStartedByUserId().toString())){
			throw new UnauthorizedException("Only the user that created a job can access the job's status.");
		}
		return status;
	}

	@Override
	public void cancelJob(UserInfo userInfo, String jobId) throws DatastoreException, NotFoundException {
		if (userInfo == null)
			throw new IllegalArgumentException("UserInfo cannot be null");
		// Get the status
		AsynchronousJobStatus status = asynchJobStatusDao.getJobStatus(jobId);
		// Only the user that started a job can read it
		if (!authorizationManager.isUserCreatorOrAdmin(userInfo, status.getStartedByUserId().toString())) {
			throw new UnauthorizedException("Only the user that created a job can stop the job.");
		}
		asynchJobStatusDao.setJobCanceling(jobId);
	}

	@Override
	public AsynchronousJobStatus startJob(UserInfo user, AsynchronousRequestBody body) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(body == null) throw new IllegalArgumentException("Body cannot be null");
		if(body instanceof CacheableRequestBody){
			/*
			 *  Before we start a CacheableRequestBody job, we need to determine if a job already exists
			 *  for this request and user.
			 */
			String requestHash = jobHashProvider.getJobHash((CacheableRequestBody) body);
			// if the requestHash is null the job cannot be cached.
			if(requestHash != null){
				// Does this job already exist
				AsynchronousJobStatus status = findJobsMatching(requestHash, body, user.getId());
				if(status != null){
					/*
					 * If here then the caller has already made this exact request
					 * and the object has not changed since the last request.
					 * Therefore, we return the same job status as before without
					 * starting a new job.
					 */
					log.info(String.format(CACHED_MESSAGE_TEMPLATE, user.getId(), requestHash, status.getJobId()));
					return status;
				}
			}
		}
		
		// Start the job.
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(user.getId(), body);
		// publish a message to get the work started
		asynchJobQueuePublisher.publishMessage(status);
		return status;
	}
	
	/**
	 * Find a job that matches the given requestHash, objectEtag, body and userId.
	 * 
	 * @param requestHash
	 * @param objectEtag
	 * @param body
	 * @param userId
	 * @return
	 */
	private AsynchronousJobStatus findJobsMatching(String requestHash, AsynchronousRequestBody body, Long userId){
		// Find all jobs that match this request.
		List<AsynchronousJobStatus> matches = asynchJobStatusDao.findCompletedJobStatus(requestHash, userId);
		if (matches != null) {
			for(AsynchronousJobStatus match: matches){
				if(body.equals(match.getRequestBody())){
					return match;
				}
			}
		}
		// no match found
		return null;
	}


	@NewWriteTransaction
	@Override
	public void updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage) {
		// Progress can only be updated if the stack is in read-write mode.
		checkStackReadWrite();
		asynchJobStatusDao.updateJobProgress(jobId, progressCurrent, progressTotal, progressMessage);
	}

	/**
	 * If the stack is not in read-write mode an IllegalStateException will be thrown.
	 */
	private void checkStackReadWrite() {
		if(!StatusEnum.READ_WRITE.equals(stackStatusDao.getCurrentStatus())){
			throw new IllegalStateException(JOB_ABORTED_MESSAGE);
		}
	}


	@WriteTransaction
	@Override
	public String setJobFailed(String jobId, Throwable error) {
		// We allow a job to fail even if the stack is not in read-write mode.
		return asynchJobStatusDao.setJobFailed(jobId, error);
	}

	@WriteTransaction
	@Override
	public void setJobCanceling(String jobId) {
		// We allow a job to cancel even if the stack is not in read-write mode.
		asynchJobStatusDao.setJobCanceling(jobId);
	}

	@WriteTransaction
	@Override
	public void setComplete(String jobId, AsynchronousResponseBody body)
			throws DatastoreException, NotFoundException, IOException {
		/*
		 *  For a cacheable requests we need to calculate a request hash.
		 *  This hash can be used to find jobs that already match an existing request.
		 */
		AsynchronousJobStatus status = lookupJobStatus(jobId);
		String requestHash = null;
		if(status.getRequestBody() instanceof CacheableRequestBody){
			CacheableRequestBody request = (CacheableRequestBody) status.getRequestBody();
			requestHash = jobHashProvider.getJobHash(request);
		}
		// capture the body of the response
		if (body instanceof Snapshotable) {
			ObjectRecord record = ObjectRecordBuilderUtils.buildObjectRecord(body, System.currentTimeMillis());
			objectRecordDAO.saveBatch(Arrays.asList(record), record.getJsonClassName());
		}
		long runtimeMS = asynchJobStatusDao.setComplete(jobId, body, requestHash);
		// Record the runtime for this job.
		AsynchJobType type = AsynchJobType.findTypeFromRequestClass(status.getRequestBody().getClass());
		pushCloudwatchMetric(runtimeMS, type);
	}
	

	/**
	 * Push a cloudwatch metric to recored the elapse time of the given job type.
	 * @param runtimeMS
	 * @param type
	 */
	void pushCloudwatchMetric(long runtimeMS, AsynchJobType type) {
		ProfileData profileData = new ProfileData();
		profileData.setNamespace(getMetricNamespace());
		profileData.setName(METRIC_NAME);
		profileData.setValue((double) runtimeMS);
		profileData.setUnit(StandardUnit.Milliseconds.name());
		profileData.setTimestamp(new Date());
		profileData.setDimension(Collections.singletonMap(JOB_TYPE, type.name()));
		this.cloudeWatch.addProfileData(profileData);
	}
	
	public String getMetricNamespace() {
		if(this.metricNamespace == null) {
			this.metricNamespace = METRIC_NAMESPACE_PREFIX+stackConfig.getStackInstance();
		}
		return this.metricNamespace;
	}

	@Override
	public void emptyAllQueues() {
		asynchJobQueuePublisher.emptyAllQueues();
	}
	

}
