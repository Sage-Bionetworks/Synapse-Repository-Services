package org.sagebionetworks.migration.worker;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AdminRequest;
import org.sagebionetworks.repo.model.migration.AdminResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class MigrationWorker implements MessageDrivenRunner {

	public static final long AUTO_PROGRESS_FREQUENCY_MS = 5*1000; // 5 seconds

	static private Logger log = LogManager.getLogger(MigrationWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private MigrationManager migrationManager;


	@Override
	public void run(ProgressCallback progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		
		try {
			final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
			final UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
			final AsyncMigrationRequest req = AsynchJobUtils.extractRequestBody(status, AsyncMigrationRequest.class);
			processAsyncMigrationRequest(progressCallback, user, req, status.getJobId());
		} catch (Throwable e) {
			log.error("Failed", e);
		}
	}
	
	public void processAsyncMigrationRequest(
			final ProgressCallback progressCallback, final UserInfo user,
			final AsyncMigrationRequest mReq, final String jobId) throws Throwable {

		try {
			AdminResponse resp = processRequest(user, mReq.getAdminRequest(), jobId);
			AsyncMigrationResponse mResp = new AsyncMigrationResponse();
			mResp.setAdminResponse(resp);
			asynchJobStatusManager.setComplete(jobId, mResp);
		} catch (Throwable e) {
			// Record the error
			asynchJobStatusManager.setJobFailed(jobId, e);
			throw e;
		}
	}
	
	AdminResponse processRequest(final UserInfo user, final AdminRequest req, final String jobId) throws DatastoreException, NotFoundException, IOException {
		if (req instanceof AsyncMigrationTypeCountRequest) {
			return migrationManager.processAsyncMigrationTypeCountRequest(user, (AsyncMigrationTypeCountRequest)req);
		} else if (req instanceof AsyncMigrationTypeCountsRequest) {
			return migrationManager.processAsyncMigrationTypeCountsRequest(user, (AsyncMigrationTypeCountsRequest)req);
		} else if (req instanceof AsyncMigrationTypeChecksumRequest) {
			return migrationManager.processAsyncMigrationTypeChecksumRequest(user, (AsyncMigrationTypeChecksumRequest)req);
		} else if (req instanceof AsyncMigrationRangeChecksumRequest) {
			return migrationManager.processAsyncMigrationRangeChecksumRequest(user, (AsyncMigrationRangeChecksumRequest)req);
		} else if (req instanceof BackupTypeRangeRequest) {
			return migrationManager.backupRequest(user, (BackupTypeRangeRequest)req);
		} else if (req instanceof RestoreTypeRequest) {
			return migrationManager.restoreRequest(user, (RestoreTypeRequest)req);
		} else if (req instanceof CalculateOptimalRangeRequest) {
			return migrationManager.calculateOptimalRanges(user, (CalculateOptimalRangeRequest)req);
		} else {
			throw new IllegalArgumentException("AsyncMigrationRequest not supported.");
		}
	}

}
