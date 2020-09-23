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
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker will stream the results of a table SQL query to a local CSV file and upload the file
 * to S3 as a FileHandle.
 */
public class TableQueryNextPageWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(TableQueryNextPageWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableQueryManager tableQueryManger;
	@Autowired
	private UserManager userManger;
	@Autowired
	TableExceptionTranslator tableExceptionTranslator;

	@Override
	public void run(ProgressCallback progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			QueryNextPageToken request = AsynchJobUtils.extractRequestBody(status, QueryNextPageToken.class);
			QueryResult queryResult = tableQueryManger.queryNextPage(progressCallback, user, request);
			asynchJobStatusManager.setComplete(status.getJobId(), queryResult);
		}catch (TableUnavailableException e){
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// do not return the message because we do not want it to be deleted.
			// but we don't want to wait too long, so set the visibility timeout to something smaller
			throw new RecoverableMessageException();
		} catch (TableFailedException e) {
			// This means we cannot use this table
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}catch(Throwable e){
			// Attempt to translate the exception into a 'user-friendly' message.
			RuntimeException translatedException = tableExceptionTranslator.translateException(e);
			// The job failed
			asynchJobStatusManager.setJobFailed(status.getJobId(), translatedException);
			log.error("Worker failed:", e);
		}
	}
}
