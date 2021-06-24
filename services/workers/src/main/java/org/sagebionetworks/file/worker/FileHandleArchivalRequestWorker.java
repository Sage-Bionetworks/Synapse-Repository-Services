package org.sagebionetworks.file.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Worker to process the archival request, the worker will only submit a subset of file handle keys to a queue for processing
 */
public class FileHandleArchivalRequestWorker implements MessageDrivenRunner {
	
	private static final Logger LOG = LogManager.getLogger(FileHandleArchivalRequestWorker.class);

	private AsynchJobStatusManager asynchJobStatusManager;
	private UserManager userManager;
	private FileHandleArchivalManager archivalManager;

	@Autowired
	public FileHandleArchivalRequestWorker(AsynchJobStatusManager asynchJobStatusManager, UserManager userManager, FileHandleArchivalManager archivalManager) {
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.userManager = userManager;
		this.archivalManager = archivalManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		
		try {
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Starting job...");
			
			FileHandleArchivalRequest request = (FileHandleArchivalRequest) status.getRequestBody();
			
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			
			FileHandleArchivalResponse response = archivalManager.processFileHandleArchivalRequest(userInfo, request);
			
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (Throwable e) {
			LOG.error("Job failed:", e);
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
		
	}

}
