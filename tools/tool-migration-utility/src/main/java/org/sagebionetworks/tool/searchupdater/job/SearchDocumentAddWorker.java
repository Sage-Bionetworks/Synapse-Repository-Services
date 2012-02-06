package org.sagebionetworks.tool.searchupdater.job;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.job.WorkerResult;
import org.sagebionetworks.tool.searchupdater.CloudSearchClient;
import org.sagebionetworks.tool.searchupdater.SearchUpdaterConfigurationImpl;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

/**
 * A worker that will execute a single search index update.
 * 
 * @author deflaux
 * 
 */
public class SearchDocumentAddWorker implements Callable<WorkerResult> {

	static private Log log = LogFactory.getLog(SearchDocumentAddWorker.class);

	private static final String S3_DOMAIN = "https://s3.amazonaws.com/";

	SearchUpdaterConfigurationImpl configuration = null;
	Set<String> entities = null;
	BasicProgress progress = null;

	/**
	 * @param configuration
	 * @param clientFactory
	 * @param entities
	 * @param progress
	 */
	public SearchDocumentAddWorker(Configuration configuration,
			ClientFactory clientFactory, Set<String> entities,
			BasicProgress progress) {
		super();
		this.configuration = (SearchUpdaterConfigurationImpl) configuration;
		this.entities = entities;
		this.progress = progress;
	}

	/**
	 * Wait for a daemon to finish.
	 * 
	 * @param daemonId
	 * @param client
	 * @return the status when the daemon is finished
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 * @throws InterruptedException
	 */
	public BackupRestoreStatus waitForDaemon(String daemonId,
			SynapseAdministration client) throws SynapseException,
			JSONObjectAdapterException, InterruptedException {
		// Wait for the daemon to finish.
		long start = System.currentTimeMillis();
		while (true) {
			long now = System.currentTimeMillis();
			if (now - start > configuration.getWorkerTimeoutMs()) {
				throw new InterruptedException(
						"Timed out waiting for the daemon to complete");
			}
			BackupRestoreStatus status = client.getDaemonStatus(daemonId);
			// Check to see if we failed.
			if (DaemonStatus.FAILED == status.getStatus()) {
				throw new InterruptedException("Failed: " + status.getType()
						+ " message:" + status.getErrorMessage()
						+ ", error details: " + status.getErrorDetails());
			}
			// Are we done?
			if (DaemonStatus.COMPLETED == status.getStatus()) {
				logStatus(status);
				return status;
			}
			logStatus(status);
			// Wait.
			Thread.sleep(2000);
		}
	}

	/**
	 * Log the status if trace is enabled.
	 * 
	 * @param status
	 */
	public void logStatus(BackupRestoreStatus status) {
		if (log.isTraceEnabled()) {
			String format = "Worker: %1$-10d : %2$s";
			String statString = DaemonStatusUtil.printStatus(status);
			log.trace(String.format(format, Thread.currentThread().getId(),
					statString));
		}
	}

	@Override
	public WorkerResult call() throws Exception {
		BackupRestoreStatus status = null;
		try {
			// First get a connection to the source
			SynapseAdministration client = configuration.createSynapseClient();
			BackupSubmission sumbission = new BackupSubmission();
			sumbission.setEntityIdsToBackup(this.entities);
			// Start a search document batch.
			status = client.startSearchDocumentDaemon(sumbission);
			// Wait for the search document batch to complete
			status = waitForDaemon(status.getId(), client);

			log.info("Adding to search index " + entities
					+ " in document batch " + status.getBackupUrl());

			// Extra safety checks, these are redundant due to the templated
			// configuration, but it never hurts to be extra paranoid
			String s3UrlPrefix = S3_DOMAIN + configuration.getStack();
			if (!status.getBackupUrl().startsWith(s3UrlPrefix)) {
				throw new IllegalArgumentException(
						"Attempted send search documents from "
								+ status.getBackupUrl() + " to stack "
								+ configuration.getStack());
			}

			// Extract the s3Key
			String s3WorkflowBucket = configuration.getS3WorkflowBucket();
			String s3WorkflowUrlPrefix = S3_DOMAIN + s3WorkflowBucket + "/";
			String searchDocumentS3Key = status.getBackupUrl().substring(
					s3WorkflowUrlPrefix.length());

			// Download the search documents placed in the S3 bucket by the repo
			// svc
			// TODO later on we expect that CloudSearch will just watch an S3
			// bucket waiting for new documents, for now they have implemented
			// this client-side via their command line tools, but we are not
			// using those for this
			AmazonS3Client s3Client = configuration.createAmazonS3Client();
			S3Object s3Object = s3Client.getObject(s3WorkflowBucket,
					searchDocumentS3Key);

			// Upload the search documents to CloudSearch
			CloudSearchClient csClient = configuration
					.createCloudSearchClient();
			csClient.sendDocuments(s3Object.getObjectContent(), s3Object
					.getObjectMetadata().getContentLength());

			// We've processed this search update correctly, so go ahead and
			// clean up the file
			s3Client.deleteObject(s3WorkflowBucket, searchDocumentS3Key);

			// Success
			// set the progress to done.
			progress.setDone();
			return new WorkerResult(this.entities.size(),
					WorkerResult.JobStatus.SUCCEDED);

		} catch (Exception e) {
			// set the progress to done.
			progress.setDone();
			// Log any errors
			log.error("SearchDocumentAddWorker Failed to run job: "
					+ entities.toString() + " for backup file "
					+ status.getBackupUrl(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}
}
