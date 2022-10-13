package org.sagebionetworks.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Adapter for an {@link AsyncJobRunner} that is driven by an SQS message that contains the id of an {@link AsynchronousRequestBody}.
 */
public class AsyncJobRunnerAdapter<RequestType extends AsynchronousRequestBody, ResponseType extends AsynchronousResponseBody> implements MessageDrivenRunner {
	 
	private AsyncJobRunner<RequestType, ResponseType> runner;
	private AsynchJobStatusManager jobManager;
	private UserManager userManager;

	public AsyncJobRunnerAdapter(AsynchJobStatusManager jobManager, UserManager userManager, AsyncJobRunner<RequestType, ResponseType> runner) {
		this.jobManager = jobManager;
		this.userManager = userManager;
		this.runner = runner;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		String jobId = message.getBody();
		AsynchronousJobStatus status = jobManager.lookupJobStatus(jobId);
		
		try {
			UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
			
			AsyncJobProgressCallback callbackWrapper = new AsyncJobProgressCallbackAdapter(jobManager, progressCallback, jobId);
			
			RequestType request = AsynchJobUtils.extractRequestBody(status, runner.getRequestType());
			ResponseType response = runner.run(jobId, user, request, callbackWrapper);
			
			jobManager.setComplete(jobId, response);
			
		} catch (RecoverableMessageException ex) {
			throw ex;
		} catch (Throwable ex) {
			jobManager.setJobFailed(jobId, ex);
		}
	}
	
}
