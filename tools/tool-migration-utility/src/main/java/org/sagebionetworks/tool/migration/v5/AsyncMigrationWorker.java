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
import org.sagebionetworks.tool.migration.v4.CreateUpdateWorker;
import org.sagebionetworks.tool.progress.BasicProgress;

public class AsyncMigrationWorker implements Callable<AsyncMigrationResponse> {

	static private Log logger = LogFactory.getLog(AsyncMigrationWorker.class);

	private SynapseAdminClient client;
	AsyncMigrationRequest request;
	BasicProgress progress;
	long timeoutMs;

	public AsyncMigrationWorker(SynapseAdminClient client, AsyncMigrationRequest request, long timeoutMs, BasicProgress progress) {
		this.client = client;
		this.request = request;
		this.timeoutMs = timeoutMs;
		this.progress = progress;
		
	}
	
	@Override
	public AsyncMigrationResponse call() throws Exception {
		AsyncMigrationResponse response = execCall();
		return response;
	}
	
	private AsyncMigrationResponse execCall() {
		AsynchronousJobStatus status = client.startAsynchronousJob(request);
		while (state != null && state == AsynchJobState.PROCESSING) {
			if (state == AsynchJobState.FAILED) {
				throw new RuntimeException("Job " + status.getJobId() + " failed!");
			} else if (state == AsynchJobState.COMPLETE) {
				break;
			} else {
				Thread.sleep(1000L);
				state = client.getAsynchronousJobStatus(status.getJobId()).getJobState();
			}
		}
		return resp;
	}
	
	public AsynchJobState waitForJob(SynapseAdminClient client, AsynchronousJobStatus jobStatus) throws InterruptedException, JSONObjectAdapterException, SynapseException {
		String jobId = jobStatus.getJobId();
		long start = System.currentTimeMillis();
		while (true) {
			long now = System.currentTimeMillis();
			if (now-start > timeoutMs){
				logger.debug("Timeout waiting for job to complete");
				throw new InterruptedException("Timed out waiting for the job " + jobId + " to complete");
			}
			AsynchJobState state = client.getAsynchronousJobStatus(jobStatus.getJobId()).getJobState();
			if (state == AsynchJobState.FAILED) {
				logger.debug("Job " + jobId + " failed.");
				throw new DaemonFailedException("Failed: "+status.getType()+" message:"+status.getErrorMessage());
			}
			Thread.sleep(2000L);
		}
	}

}
