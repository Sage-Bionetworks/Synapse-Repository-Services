package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker will stream the results of a table SQL query to a local CSV file
 * and upload the file to S3 as a FileHandle.
 * 
 */
public class TableQueryWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(TableQueryWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableQueryManager tableQueryManager;
	@Autowired
	private UserManager userManger;


	@Override
	public void run(final ProgressCallback progressCallback, final Message message) throws JSONObjectAdapterException, RecoverableMessageException{
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			QueryBundleRequest request = AsynchJobUtils.extractRequestBody(status, QueryBundleRequest.class);
			QueryResultBundle queryBundle = tableQueryManager.queryBundle(progressCallback, user, request);
			asynchJobStatusManager.setComplete(status.getJobId(), queryBundle);
		} catch (TableUnavailableException e) {
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// This just means we cannot do this right now. We can try again
			// later.
			throw new RecoverableMessageException();
		}catch (LockUnavilableException e) {
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// This just means we cannot do this right now. We can try again
			// later.
			throw new RecoverableMessageException();
		} catch (TableFailedException e) {
			// This means we cannot use this table
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		} catch (Throwable e) {
			// The job failed
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			log.error("Failed", e);
		}
	}
}
