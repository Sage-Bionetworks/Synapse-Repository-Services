package org.sagebionetworks.file.worker;

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerNotifier;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Worker that handles a range scan request for a given association type, process an SQS message with the details about the range to scan
 */
public class FileHandleAssociationScanRangeWorker implements MessageDrivenRunner {
	
	private static final String METRIC_PARSE_MESSAGE_ERROR_COUNT = "ParseMessageErrorCount";
	private static final String METRIC_JOB_FAILED_COUNT = "JobFailedCount";
	private static final String METRIC_JOB_RETRY_COUNT = "JobRetryCount";
	private static final String DIMENSION_ASSOCIATION_TYPE = "AssociationType";
	
	private static final Logger LOG = LogManager.getLogger(FileHandleAssociationScanRangeWorker.class);
	
	private FileHandleAssociationScannerJobManager manager;
	private FileHandleAssociationScannerNotifier notifier;
	private WorkerLogger workerLogger;

	@Autowired
	public FileHandleAssociationScanRangeWorker(FileHandleAssociationScannerJobManager manager, FileHandleAssociationScannerNotifier notifier, WorkerLogger workerLogger) {
		this.manager = manager;
		this.notifier = notifier;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		
		final FileHandleAssociationScanRangeRequest request;
		
		try {
			request = notifier.fromSqsMessage(message);
		} catch (Throwable e) {
			LOG.error("Could not process SQS message \n" + message.getBody() + "\n" + e.getMessage(), e);
			logWorkerCountMetric(METRIC_PARSE_MESSAGE_ERROR_COUNT);
			return;
		}

		long start = System.currentTimeMillis();
		
		try {
			LOG.info(requestToString(request) + " STARTING");
			
			int count = manager.processScanRangeRequest(request);
			
			LOG.info(requestToString(request) + " COMPLETED: " + count + " associations (Spent " + (System.currentTimeMillis() - start) + " ms)");
		} catch (NotFoundException e) {
			LOG.warn(e.getMessage(), e);
		} catch (RecoverableException e) {
			LOG.warn(requestToString(request) + " RETRYING: " + e.getMessage(), e);
			logWorkerCountMetric(METRIC_JOB_RETRY_COUNT);
			throw new RecoverableMessageException(e.getMessage(), e);
		} catch (Throwable e) {
			LOG.error(requestToString(request) + " FAILED: " + e.getMessage(), e);
			logWorkerCountMetric(METRIC_JOB_FAILED_COUNT);
		} finally {
			logWorkerTimeMetric(request, System.currentTimeMillis() - start);
		}
		
	}
	
	private String requestToString(FileHandleAssociationScanRangeRequest request) {
		return "FileHandleAssociationScanRangeRequest [jobId: " + request.getJobId() + ", associationType: " + request.getAssociationType() + ", range: " + request.getIdRange().getMinId() + "-" + request.getIdRange().getMaxId() + "]";
	}
	
	private void logWorkerCountMetric(String metricName) {
		workerLogger.logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, metricName);
	}
	
	private void logWorkerTimeMetric(FileHandleAssociationScanRangeRequest request, long timeMillis) {
		Map<String, String> dimensions = Collections.singletonMap(DIMENSION_ASSOCIATION_TYPE, request.getAssociationType().name());
		
		workerLogger.logWorkerTimeMetric(FileHandleAssociationScanRangeWorker.class, timeMillis, dimensions);
	}

}
