package org.sagebionetworks.file.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.FileHandleSupport;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class BulkFileDownloadWorker implements MessageDrivenRunner {

	private AsynchJobStatusManager asynchJobStatusManager;
	private UserManager userManger;
	private FileHandleSupport fileHandleSupport;

	@Autowired
	public BulkFileDownloadWorker(AsynchJobStatusManager asynchJobStatusManager, UserManager userManger,
			FileHandleSupport fileHandleSupport) {
		super();
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.userManger = userManger;
		this.fileHandleSupport = fileHandleSupport;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {

		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {
			if (!(status.getRequestBody() instanceof BulkFileDownloadRequest)) {
				throw new IllegalArgumentException("Unexpected request body: " + status.getRequestBody());
			}
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			BulkFileDownloadRequest request = AsynchJobUtils.extractRequestBody(status, BulkFileDownloadRequest.class);
			// build the zip from the results
			BulkFileDownloadResponse response = fileHandleSupport.buildZip(user, request);
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (Throwable e) {
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
	}

}
