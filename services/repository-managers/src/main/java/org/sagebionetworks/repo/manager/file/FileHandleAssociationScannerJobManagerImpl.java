package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationRecord;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleScannerUtils;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.files.DBOFilesScannerStatus;
import org.sagebionetworks.repo.model.dbo.dao.files.FilesScannerStatusDao;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileHandleAssociationScannerJobManagerImpl implements FileHandleAssociationScannerJobManager {
		
	static final int KINESIS_BATCH_SIZE = 5000;
	
	private FileHandleAssociationManager associationManager;
	private AwsKinesisFirehoseLogger kinesisLogger;
	private StackStatusDao stackStatusDao;
	private FilesScannerStatusDao statusDao;
	private FileHandleAssociationScannerNotifier notifier;
	private Clock clock;
	
	@Autowired
	public FileHandleAssociationScannerJobManagerImpl(FileHandleAssociationManager associationManager, AwsKinesisFirehoseLogger kinesisLogger, StackStatusDao stackStatusDao, FilesScannerStatusDao statusDao, FileHandleAssociationScannerNotifier notifier, Clock clock) {
		this.associationManager = associationManager;
		this.kinesisLogger = kinesisLogger;
		this.stackStatusDao = stackStatusDao;
		this.statusDao = statusDao;
		this.notifier = notifier;
		this.clock = clock;
	}

	@Override
	public int processScanRangeRequest(FileHandleAssociationScanRangeRequest request) throws RecoverableException {
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getJobId(), "The request.jobId");
		ValidateArgument.required(request.getAssociationType(), "The request.associationType");
		ValidateArgument.required(request.getIdRange(), "The request.idRange");
		
		validateStackReadWrite();
		
		// Using a set to handle duplicates in the batch
		Set<FileHandleAssociationRecord> recordsBatch = new HashSet<>(KINESIS_BATCH_SIZE);
		// We use a common timestamp for the whole batch so that we can de-duplicate
		long batchTimestamp = clock.currentTimeMillis();
		
		Iterable<ScannedFileHandleAssociation> iterable = associationManager.scanRange(request.getAssociationType(), request.getIdRange());
		
		int totalRecords = 0;
		
		for (ScannedFileHandleAssociation association : iterable) {
			Set<FileHandleAssociationRecord> associationRecords = FileHandleScannerUtils.mapAssociation(request.getAssociationType(), association, batchTimestamp);
			
			if (associationRecords.isEmpty()) {
				continue;
			}
			
			recordsBatch.addAll(associationRecords);
			
			if (recordsBatch.size() >= KINESIS_BATCH_SIZE) {
				totalRecords += flushRecordsBatch(recordsBatch);
				batchTimestamp = clock.currentTimeMillis();
			}
		}
		
		if (!recordsBatch.isEmpty()) {
			totalRecords += flushRecordsBatch(recordsBatch);
		}
		
		statusDao.increaseJobCompletedCount(request.getJobId(), totalRecords);
		
		return totalRecords;
		
	}
	
	@Override
	public boolean isScanJobIdle(int daysNum) {
		ValidateArgument.requirement(daysNum > 0, "The number of days must be greater than zero.");
		return !statusDao.exists(daysNum);
	}
	
	@Override
	@WriteTransaction
	public void startScanJob() {
		DBOFilesScannerStatus status = statusDao.create();
		
		long totalJobs = 0L;
		
		for (FileHandleAssociateType associationType : FileHandleAssociateType.values()) {
			totalJobs += dispatchScanRequests(status.getId(), associationType);
		}
		
		statusDao.setStartedJobsCount(status.getId(), totalJobs);
	}
	
	private long dispatchScanRequests(long jobId, FileHandleAssociateType associationType) {
		IdRange range = associationManager.getIdRange(associationType);
		
		long requestsCount = 0;
		
		if (range.getMinId() < 0 || range.getMaxId() < 0) {
			return requestsCount;
		}
		
		long scanRangeRequestSize = associationManager.getMaxIdRangeSize(associationType);
		
		requestsCount = (range.getMaxId() - range.getMinId()) / scanRangeRequestSize + 1;
		
		long requestMinId = range.getMinId();
		long requestMaxId = requestMinId + scanRangeRequestSize - 1;
		
		for (int requestNum = 0; requestNum < requestsCount; requestNum++) {
			IdRange requestRange = new IdRange(requestMinId, requestMaxId);
			
			FileHandleAssociationScanRangeRequest request = new FileHandleAssociationScanRangeRequest()
					.withAssociationType(associationType)
					.withJobId(jobId)
					.withIdRange(requestRange);
			
			notifier.sendScanRequest(request);
			
			requestMinId += scanRangeRequestSize;
			requestMaxId += scanRangeRequestSize;
		}
		
		return requestsCount;
	}
	
	private int flushRecordsBatch(Set<FileHandleAssociationRecord> recordsBatch) {
		validateStackReadWrite();
		int size = recordsBatch.size();
		kinesisLogger.logBatch(FileHandleAssociationRecord.KINESIS_STREAM_NAME, new ArrayList<>(recordsBatch));
		recordsBatch.clear();
		return size;
	}
	
	private void validateStackReadWrite() {
		if (stackStatusDao.isStackReadWrite()) {
			return;
		}
		throw new ReadOnlyException("The stack was in read-only mode.");
	}

}
