package org.sagebionetworks.schema.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class CreateJsonSchemaWorker implements MessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(CreateJsonSchemaWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private UserManager userManger;
	@Autowired
	private JsonSchemaManager schemaManager;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
		try {
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Starting job...");
			CreateSchemaRequest requestBody = (CreateSchemaRequest) status.getRequestBody();
			CreateSchemaResponse response = schemaManager.createJsonSchema(user, requestBody);
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (RecoverableMessageException e) {
			log.info("RecoverableMessageException: "+e.getMessage());
			// reset the job progress.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, e.getMessage());
			throw e;
		}catch (Throwable e) {
			log.error("Job failed:", e);
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
	}

}
