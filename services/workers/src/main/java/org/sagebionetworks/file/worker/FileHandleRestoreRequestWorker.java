package org.sagebionetworks.file.worker;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.FileHandleRestoreRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleRestoreStatus;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class FileHandleRestoreRequestWorker implements MessageDrivenRunner {

	static final int MAX_BATCH_SIZE = 1000;
	private static final Logger LOG = LogManager.getLogger(FileHandleRestoreRequestWorker.class);

	private AsynchJobStatusManager asynchJobStatusManager;
	private UserManager userManager;
	private FileHandleArchivalManager archivalManager;
	
	@Autowired
	public FileHandleRestoreRequestWorker(AsynchJobStatusManager asynchJobStatusManager, UserManager userManager, FileHandleArchivalManager archivalManager) {
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.userManager = userManager;
		this.archivalManager = archivalManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		
		try {
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Starting job...");
			
			FileHandleRestoreRequest request = (FileHandleRestoreRequest) status.getRequestBody();
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			
			ValidateArgument.requiredNotEmpty(request.getFileHandleIds(), "The fileHandleIds list");
			ValidateArgument.requirement(request.getFileHandleIds().size() <= MAX_BATCH_SIZE, "The number of file handles exceed the maximum allowed (Was: " + request.getFileHandleIds().size() + ", Max:" +MAX_BATCH_SIZE + ").");
			
			List<FileHandleRestoreResult> results = new ArrayList<>(request.getFileHandleIds().size());
			
			for (String id : request.getFileHandleIds()) {
				FileHandleRestoreResult result;
				
				try {
					result = archivalManager.restoreFileHandle(userInfo, id);
				} catch (Exception e) {
					LOG.error("Restore request for file handle " + id + " FAILED: " + e.getMessage(), e);
					result = new FileHandleRestoreResult().setFileHandleId(id).setStatus(FileHandleRestoreStatus.FAILED).setStatusMessage(e.getMessage());
				}
				
				results.add(result);
			}
			
			FileHandleRestoreResponse response = new FileHandleRestoreResponse()
					.setRestoreResults(results);
			
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (Throwable e) {
			LOG.error("Job failed:", e);
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
		
	}

}
