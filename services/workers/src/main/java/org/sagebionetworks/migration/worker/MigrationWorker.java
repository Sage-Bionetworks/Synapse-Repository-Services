package org.sagebionetworks.migration.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class MigrationWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(MigrationWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private UserManager userManager;
	// For easier testing
	@Autowired
	private AsyncMigrationRequestProcessor requestProcessor;

	@Override
	public void run(ProgressCallback<Void> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		
		try {
			processRequest(progressCallback, message);
		} catch (Throwable e) {
			log.error("Failed", e);
		}
	}
	
	public void processRequest(final ProgressCallback<Void> progressCallback, final Message message) throws Throwable {
		final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		final UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
		final AsyncMigrationRequest req = AsynchJobUtils.extractRequestBody(status, AsyncMigrationRequest.class);
		requestProcessor.processAsyncMigrationRequest(progressCallback, user, req, status.getJobId());
	}
	
}
