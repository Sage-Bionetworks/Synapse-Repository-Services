package org.sagebionetworks.tool.migration.v5;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v3.DaemonFailedException;
import org.sagebionetworks.tool.progress.BasicProgress;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.DefaultClock;

public class AsyncMigrationWorker implements Callable<AsyncMigrationResponse> {

	static private Log logger = LogFactory.getLog(AsyncMigrationWorker.class);

	private SynapseAdminClient client;
	private AsyncMigrationRequest request;
	private BasicProgress progress;
	long timeoutMs;
	private Clock clock;

	public AsyncMigrationWorker(SynapseAdminClient client, AsyncMigrationRequest request, long timeoutMs, BasicProgress progress) {
		this.client = client;
		this.request = request;
		this.timeoutMs = timeoutMs;
		this.progress = progress;
		this.clock = new DefaultClock();
	}
	
	@Override
	public AsyncMigrationResponse call() throws SynapseException, InterruptedException, JSONObjectAdapterException {
		AsynchronousJobStatus status = client.startAsynchronousJob(request);
		status = waitForJobToComplete(status.getJobId());
		return (AsyncMigrationResponse)status.getResponseBody();
	}
	
	private AsynchronousJobStatus waitForJobToComplete(String jobId) throws InterruptedException, JSONObjectAdapterException, SynapseException {
		long start = clock.currentTimeMillis();
		AsynchronousJobStatus status;
		while (true) {
			long now = clock.currentTimeMillis();
			if (now-start > timeoutMs){
				logger.debug("Timeout waiting for job to complete");
				throw new InterruptedException("Timed out waiting for the job " + jobId + " to complete");
			}
			status = client.getAsynchronousJobStatus(jobId);
			AsynchJobState state = status.getJobState();
			if (state == AsynchJobState.FAILED) {
				logger.debug("Job " + jobId + " failed.");
				throw new WorkerFailedException("Failed: " + status.getErrorDetails() + " message:" + status.getErrorMessage());
			}
			if (state == AsynchJobState.PROCESSING) {
				progress.setCurrent(status.getProgressCurrent());
				progress.setTotal(status.getProgressTotal());
			}
			if (state == AsynchJobState.COMPLETE) {
				break;
			}
			clock.sleep(2000L);
		}
		return status;
	}

}
