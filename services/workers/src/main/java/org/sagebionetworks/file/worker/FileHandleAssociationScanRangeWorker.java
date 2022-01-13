package org.sagebionetworks.file.worker;

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Worker that handles a range scan request for a given association type, process an SQS message with the details about the range to scan
 */
public class FileHandleAssociationScanRangeWorker implements TypedMessageDrivenRunner<FileHandleAssociationScanRangeRequest> {
	
	private static final String METRIC_JOB_FAILED_COUNT = "JobFailedCount";
	private static final String METRIC_JOB_COMPLETED_COUNT = "JobCompletedCount";
	private static final String METRIC_ALL_JOBS_COMPLETED_COUNT = "AllJobsCompletedCount";
	private static final String METRIC_JOB_RETRY_COUNT = "JobRetryCount";
	private static final String DIMENSION_ASSOCIATION_TYPE = "AssociationType";
	
	private static final Logger LOG = LogManager.getLogger(FileHandleAssociationScanRangeWorker.class);
	
	private FileHandleAssociationScannerJobManager manager;
	private WorkerLogger workerLogger;

	@Autowired
	public FileHandleAssociationScanRangeWorker(FileHandleAssociationScannerJobManager manager, WorkerLogger workerLogger) {
		this.manager = manager;
		this.workerLogger = workerLogger;
	}
	
	@Override
	public Class<FileHandleAssociationScanRangeRequest> getObjectClass() {
		return FileHandleAssociationScanRangeRequest.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, FileHandleAssociationScanRangeRequest request) throws RecoverableMessageException, Exception {
		
		long start = System.currentTimeMillis();
		
		try {
			LOG.info(requestToString(request) + " STARTING");
			
			int count = manager.processScanRangeRequest(request);
			
			LOG.info(requestToString(request) + " COMPLETED: " + count + " associations (Spent " + (System.currentTimeMillis() - start) + " ms)");
			
			logWorkerCountMetric(METRIC_JOB_COMPLETED_COUNT);
			
			if (manager.isScanJobCompleted(request.getJobId())) {
				logWorkerCountMetric(METRIC_ALL_JOBS_COMPLETED_COUNT);
			}
			
		} catch (NotFoundException e) {
			LOG.warn(e.getMessage(), e);
		} catch (RecoverableMessageException e) {
			LOG.warn(requestToString(request) + " RETRYING: " + e.getMessage(), e);
			logWorkerCountMetric(METRIC_JOB_RETRY_COUNT);
			throw e;
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
