package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisDeliveryException;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationRecord;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleScannerUtils;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.files.DBOFilesScannerStatus;
import org.sagebionetworks.repo.model.dbo.dao.files.FilesScannerStatusDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileHandleAssociationScannerJobManagerImpl implements FileHandleAssociationScannerJobManager {
	
	private static final Logger LOG = LogManager.getLogger(FileHandleAssociationScannerJobManagerImpl.class);
		
	static final long FLUSH_DELAY_MS = 250;
	static final int KINESIS_BATCH_SIZE = 10000;
	static final int MAX_DELAY_SEC = 60;
	
	private FileHandleAssociationManager associationManager;
	private AwsKinesisFirehoseLogger kinesisLogger;
	private StackStatusDao stackStatusDao;
	private FilesScannerStatusDao statusDao;
	private FileHandleDao fileHandleDao;
	private FileHandleAssociationScannerNotifier notifier;
	private Clock clock;
	
	@Autowired
	public FileHandleAssociationScannerJobManagerImpl(FileHandleAssociationManager associationManager, AwsKinesisFirehoseLogger kinesisLogger, StackStatusDao stackStatusDao, FilesScannerStatusDao statusDao, FileHandleDao fileHandleDao, FileHandleAssociationScannerNotifier notifier, Clock clock) {
		this.associationManager = associationManager;
		this.kinesisLogger = kinesisLogger;
		this.stackStatusDao = stackStatusDao;
		this.statusDao = statusDao;
		this.fileHandleDao = fileHandleDao;
		this.notifier = notifier;
		this.clock = clock;
	}

	@Override
	public int processScanRangeRequest(FileHandleAssociationScanRangeRequest request) throws RecoverableMessageException {
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getJobId(), "The request.jobId");
		ValidateArgument.required(request.getAssociationType(), "The request.associationType");
		ValidateArgument.required(request.getIdRange(), "The request.idRange");
		
		// The job does not exist anymore
		if (!statusDao.exist(request.getJobId())) {
			throw new NotFoundException("A job with id " + request.getJobId() + " does not exist.");
		}
		
		validateStackReadWrite();
		
		// Using a set to handle duplicates in the batch
		Set<FileHandleAssociationRecord> recordsBatch = new HashSet<>(KINESIS_BATCH_SIZE);
		// We use a common timestamp for the whole batch so that we can de-duplicate
		long batchTimestamp = clock.currentTimeMillis();
		
		Iterable<ScannedFileHandleAssociation> iterable = associationManager.scanRange(request.getAssociationType(), request.getIdRange());

		int totalRecords = 0;
		int relinkedRecords = 0;

		for (ScannedFileHandleAssociation association : iterable) {

			Set<FileHandleAssociationRecord> associationRecords = FileHandleScannerUtils.mapAssociation(request.getAssociationType(), association, batchTimestamp);

			if (associationRecords.isEmpty()) {
				continue;
			}

			recordsBatch.addAll(associationRecords);
			
			if (recordsBatch.size() >= KINESIS_BATCH_SIZE) {
				validateStackReadWrite();
				relinkedRecords += relinkRecords(recordsBatch);
				totalRecords += flushRecordsBatch(recordsBatch);
				recordsBatch.clear();
				batchTimestamp = clock.currentTimeMillis();

				// Put a small sleep before continuing with the next batch to avoid saturating kinesis
				try {
					clock.sleep(FLUSH_DELAY_MS);
				} catch (InterruptedException e) {
					LOG.warn(e.getMessage(), e);
				}
			}
		}

		if (!recordsBatch.isEmpty()) {
			validateStackReadWrite();
			relinkedRecords += relinkRecords(recordsBatch);
			totalRecords += flushRecordsBatch(recordsBatch);
		}

		statusDao.increaseJobCompletedCount(request.getJobId(), totalRecords, relinkedRecords);

		return totalRecords;
		
	}
	
	@Override
	public boolean isScanJobIdle(int daysNum) {
		ValidateArgument.requirement(daysNum > 0, "The number of days must be greater than zero.");
		return !statusDao.existsWithinLast(daysNum);
	}

	@Override
	public boolean isScanJobCompleted(long jobId) {
		DBOFilesScannerStatus status = statusDao.get(jobId);
		
		return status.getJobsStartedCount() > 0 && status.getJobsCompletedCount() >= status.getJobsStartedCount();
	}
	
	@Override
	public void startScanJob() {
		DBOFilesScannerStatus status = statusDao.create();
		
		long totalJobs = 0L;
		
		for (FileHandleAssociateType associationType : FileHandleAssociateType.values()) {
			try {
				totalJobs += dispatchScanRequests(status.getId(), associationType);
			} catch (Throwable e) {
				// Note: we cannot wrap the whole method in a transaction since there is a chance that while sending out the SQS notifications
				// those are picked up by the processing worker before the transaction is committed and therefore the status is not visible.
				// Instead we simply cleanup on any exception while dispatching so that current ones are interrupted an tried again later 
				statusDao.delete(status.getId());
				throw e;
			}
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
			
			notifier.sendScanRequest(request, getMessageDelay());
			
			requestMinId += scanRangeRequestSize;
			requestMaxId += scanRangeRequestSize;
		}
		
		return requestsCount;
	}
	
	private static int getMessageDelay() {
		return ThreadLocalRandom.current().nextInt(MAX_DELAY_SEC + 1);
	}
	
	private int flushRecordsBatch(Set<FileHandleAssociationRecord> recordsBatch) throws RecoverableMessageException {
		if (recordsBatch.isEmpty()) {
			return 0;
		}
		
		int size = recordsBatch.size();
		
		try {
			kinesisLogger.logBatch(FileHandleAssociationRecord.STREAM_NAME, new ArrayList<>(recordsBatch));
		} catch (AwsKinesisDeliveryException e) {
			throw new RecoverableMessageException(e);
		}
		
		return size;
	}
	
	private int relinkRecords(Set<FileHandleAssociationRecord> recordsBatch) {
		
		int relinkedCount = 0;
		
		if (recordsBatch.isEmpty()) {
			return relinkedCount;
		}
		
		List<Long> fileHandleIds = new ArrayList<>(recordsBatch.stream()
				.map(FileHandleAssociationRecord::getFileHandleId)
				.collect(Collectors.toSet()));
		
		if (fileHandleDao.hasStatusBatch(fileHandleIds, FileHandleStatus.UNLINKED)) {

			List<Long> updatedIds = fileHandleDao.updateStatusForBatch(fileHandleIds, FileHandleStatus.AVAILABLE, FileHandleStatus.UNLINKED, 0);

			relinkedCount = updatedIds.size();
			
			if (!updatedIds.isEmpty()) {
				LOG.warn("{} scanned records were UNLINKED and set to AVAILABLE: {}", relinkedCount, updatedIds);
			}
			
		}
		
		return relinkedCount;
	}
	
	private void validateStackReadWrite() throws RecoverableMessageException {
		if (stackStatusDao.isStackReadWrite()) {
			return;
		}
		throw new RecoverableMessageException("The stack was in read-only mode.");
	}

}
