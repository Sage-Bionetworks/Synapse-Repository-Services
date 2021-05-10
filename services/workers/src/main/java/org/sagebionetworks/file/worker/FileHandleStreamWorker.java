package org.sagebionetworks.file.worker;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Worker that streams file handle changes to S3 through kinesis for unlinked file handle detection
 */
public class FileHandleStreamWorker implements  BatchChangeMessageDrivenRunner {
	
	static final int UPDATED_ON_DAYS_FILTER = 30;

	private FileHandleDao fileHandleDao;
	private AwsKinesisFirehoseLogger kinesisLogger;
	
	@Autowired
	public FileHandleStreamWorker(FileHandleDao fileHandleDao, AwsKinesisFirehoseLogger kinesisLogger) {
		this.fileHandleDao = fileHandleDao;
		this.kinesisLogger = kinesisLogger;
	}
	
	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> messages) throws RecoverableMessageException, Exception {
		
		if (messages.isEmpty()) {
			return;
		}
		
		List<Long> fileHandleIds = messages.stream()
				.filter( message -> ObjectType.FILE.equals(message.getObjectType()) && !ChangeType.DELETE.equals(message.getChangeType()))
				.map(message -> Long.valueOf(message.getObjectId()))
				.collect(Collectors.toList());
		
		if (fileHandleIds.isEmpty()) {
			return;
		}
		
		List<DBOFileHandle> fileHandles = fileHandleDao.getDBOFileHandlesBatch(fileHandleIds, UPDATED_ON_DAYS_FILTER);
		
		List<FileHandleRecord> records = fileHandles.stream()
				.map(this::mapFileHandle)
				.collect(Collectors.toList());
		
		if (records.isEmpty()) {
			return;
		}
		
		kinesisLogger.logBatch(FileHandleRecord.STREAM_NAME, records);
		
	}
	
	FileHandleRecord mapFileHandle(DBOFileHandle file) {
		FileHandleRecord record = new FileHandleRecord()
				.withId(file.getId())
				.withCreatedOn(file.getCreatedOn().getTime())
				.withUpdatedOn(file.getUpdatedOn().getTime())
				.withStatus(file.getStatus())
				.withContentSize(file.getContentSize())
				.withBucket(file.getBucketName())
				.withKey(file.getKey())
				.withIsPreview(file.getIsPreview() == null ? false : file.getIsPreview());
		
		return record;
				
	}
	
}
