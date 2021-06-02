package org.sagebionetworks.file.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The worker periodically dispatches SQS messages to scan ranges for all the association types
 */
public class FileHandleAssociationScanDispatcherWorker implements ProgressingRunner {
	
	private static final String METRIC_JOB_FAILED_COUNT = "JobFailedCount";
	
	private static final Logger LOG = LogManager.getLogger(FileHandleAssociationScanDispatcherWorker.class);
	
	
	static final int START_INTERVAL_DAYS = 5;
	
	private FileHandleAssociationScannerJobManager manager;
	private WorkerLogger workerLogger;
	
	@Autowired
	public FileHandleAssociationScanDispatcherWorker(FileHandleAssociationScannerJobManager manager, WorkerLogger workerLogger) {
		this.manager = manager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		try {
			if (manager.isScanJobIdle(START_INTERVAL_DAYS)) {
				manager.startScanJob();
			}
		} catch (Throwable e) {
			LOG.error("Failed to start new job: " + e.getMessage(), e);
			workerLogger.logWorkerCountMetric(FileHandleAssociationScanDispatcherWorker.class, METRIC_JOB_FAILED_COUNT);
		}
	}

}
