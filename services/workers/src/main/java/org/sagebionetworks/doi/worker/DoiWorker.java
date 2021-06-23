package org.sagebionetworks.doi.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class DoiWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(DoiWorker.class);

	@Autowired
	private DoiManager doiManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;

	@Override
	public void run(final ProgressCallback progressCallback, Message message)
			throws RecoverableMessageException {
		final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try{
			DoiRequest request = AsynchJobUtils.extractRequestBody(status, DoiRequest.class);
			ValidateArgument.required(request, "DoiRequest");
			// The manager does the rest of the work.
			DoiResponse responseBody = new DoiResponse();
			responseBody.setDoi(doiManager.createOrUpdateDoi(userManager.getUserInfo(status.getStartedByUserId()), request.getDoi()));
			// Set the job complete.
			asynchJobStatusManager.setComplete(status.getJobId(), responseBody);
			log.info("JobId: "+status.getJobId()+" complete");

		}catch (RecoverableMessageException e){
			log.info("RecoverableMessageException: "+e.getMessage());
			throw e;
		}catch (Throwable e){
			log.error("Job failed:", e);
			// job failed.
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
	}
}