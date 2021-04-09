package org.sagebionetworks.file.worker;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Worker that streams file handle changes to S3 through kinesis
 */
public class FileHandleStreamWorker implements  BatchChangeMessageDrivenRunner {

	// TODO: This will need to be replaced with some status column
	private static final String STATUS_AVAILABLE = "AVAILABLE";
	private static final String STREAM_NAME = "fileHandleData";
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
		
		List<String> fileHandleIds = messages.stream()
				.filter( message -> ObjectType.FILE.equals(message.getObjectType()) && !ChangeType.DELETE.equals(message.getChangeType()))
				.map(ChangeMessage::getObjectId)
				.collect(Collectors.toList());
		
		if (fileHandleIds.isEmpty()) {
			return;
		}
		
		Map<String, FileHandle> fileHandles = fileHandleDao.getAllFileHandlesBatch(fileHandleIds);
		
		List<FileHandleRecord> records = fileHandles.values().stream()
				.map(this::mapFileHandle)
				.collect(Collectors.toList());
		
		if (records.isEmpty()) {
			return;
		}
		
		kinesisLogger.logBatch(STREAM_NAME, records);
		
	}
	
	FileHandleRecord mapFileHandle(FileHandle file) {
		FileHandleRecord record = new FileHandleRecord()
				.withId(Long.parseLong(file.getId()))
				.withCreatedOn(file.getCreatedOn().getTime())
				.withStatus(STATUS_AVAILABLE)
				.withContentSize(file.getContentSize());
		
		if (file instanceof CloudProviderFileHandleInterface) {
			CloudProviderFileHandleInterface cloudFile = (CloudProviderFileHandleInterface) file;
			if (cloudFile.getIsPreview() != null) {
				record.withIsPreview(cloudFile.getIsPreview());
			} else {
				record.withIsPreview(false);
			}
			record.withBucket(cloudFile.getBucketName());
			record.withKey(cloudFile.getKey());
		} else {
			record.withIsPreview(false);
		}
		
		return record;
				
	}
	
}
