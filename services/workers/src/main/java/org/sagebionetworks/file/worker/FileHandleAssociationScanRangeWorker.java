package org.sagebionetworks.file.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileHandleAssociationScanRangeWorker implements MessageDrivenRunner {
	
	private static final Logger LOG = LogManager.getLogger(FileHandleAssociationScanRangeWorker.class);
	
	private FileHandleAssociationScannerJobManager manager;
	private ObjectMapper objectMapper;
	private WorkerLogger workerLogger;

	@Autowired
	public FileHandleAssociationScanRangeWorker(FileHandleAssociationScannerJobManager manager, ObjectMapper objectMapper, WorkerLogger workerLogger) {
		this.manager = manager;
		this.objectMapper = objectMapper;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		
		FileHandleAssociationScanRangeRequest request;
		
		try {
			request = objectMapper.readValue(message.getBody(), FileHandleAssociationScanRangeRequest.class);
		} catch (Throwable e) {
			LOG.error("Could not process SQS message \n" + message.getBody() + "\n" + e.getMessage(), e);
			boolean willRetry = false;
			workerLogger.logWorkerFailure(FileHandleAssociationScanRangeWorker.class.getName(), e, willRetry);
			return;
		}
		
		try {
			LOG.info(requestToString(request) + " STARTING");
			
			long start = System.currentTimeMillis();
			
			int count = manager.processScanRangeRequest(request);
			
			LOG.info(requestToString(request) + " COMPLETED: " + count + " associations (Spent " + (System.currentTimeMillis() - start) + " ms)");
		} catch (RecoverableException e) {
			LOG.warn(requestToString(request) + " RETRING: " + e.getMessage(), e);
			boolean willRetry = true;
			workerLogger.logWorkerFailure(FileHandleAssociationScanRangeWorker.class.getName(), e, willRetry);
			throw new RecoverableMessageException(e.getMessage(), e);
		} catch (Throwable e) {
			LOG.error(requestToString(request) + " FAILED: " + e.getMessage(), e);
			boolean willRetry = false;
			workerLogger.logWorkerFailure(FileHandleAssociationScanRangeWorker.class.getName(), e, willRetry);
		}
		
	}
	
	private String requestToString(FileHandleAssociationScanRangeRequest request) {
		return "FileHandleAssociationScanRangeRequest [jobId: " + request.getJobId() + ", associationType: " + request.getAssociationType() + ", range: " + request.getIdRange().getMinId() + "-" + request.getIdRange().getMaxId() + "]";
	}

}
