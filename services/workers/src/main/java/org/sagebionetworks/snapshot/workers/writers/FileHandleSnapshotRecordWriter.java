package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.FileHandleSnapshot;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileHandleSnapshotRecordWriter implements ObjectRecordWriter {

	private static final String KINESIS_STREAM = "fileSnapshots";

	static private Logger log = LogManager.getLogger(FileHandleSnapshotRecordWriter.class);

	private FileHandleDao fileHandleDao;
	private AwsKinesisFirehoseLogger kinesisLogger;

	@Autowired
	public FileHandleSnapshotRecordWriter(FileHandleDao fileHandleDao, AwsKinesisFirehoseLogger kinesisLogger) {
		this.fileHandleDao = fileHandleDao;
		this.kinesisLogger = kinesisLogger;
	}

	/**
	 * Build a FileHandleSnapshot that captures all common fields in all FileHandle implementations.
	 * 
	 * @param fileHandle
	 * @return
	 */
	public static FileHandleSnapshot buildFileHandleSnapshot(FileHandle fileHandle) {
		FileHandleSnapshot snapshot = new FileHandleSnapshot();
		snapshot.setConcreteType(fileHandle.getConcreteType());
		snapshot.setContentMd5(fileHandle.getContentMd5());
		snapshot.setContentSize(fileHandle.getContentSize());
		snapshot.setCreatedBy(fileHandle.getCreatedBy());
		snapshot.setCreatedOn(fileHandle.getCreatedOn());
		snapshot.setModifiedOn(fileHandle.getModifiedOn());
		snapshot.setFileName(fileHandle.getFileName());
		snapshot.setId(fileHandle.getId());
		snapshot.setStorageLocationId(fileHandle.getStorageLocationId());
		snapshot.setContentType(fileHandle.getContentType());
		snapshot.setStatus(fileHandle.getStatus());
		snapshot.setIsPreview(false);
		if (fileHandle instanceof CloudProviderFileHandleInterface) {
			CloudProviderFileHandleInterface s3FH = (CloudProviderFileHandleInterface) fileHandle;
			snapshot.setBucket(s3FH.getBucketName());
			snapshot.setKey(s3FH.getKey());
			snapshot.setPreviewId(s3FH.getPreviewId());
			snapshot.setIsPreview(s3FH.getIsPreview());
		} else if (fileHandle instanceof ExternalFileHandle) {
			ExternalFileHandle externalFH = (ExternalFileHandle) fileHandle;
			snapshot.setKey(externalFH.getExternalURL());
		} else if (fileHandle instanceof ProxyFileHandle) {
			ProxyFileHandle proxyFH = (ProxyFileHandle) fileHandle;
			snapshot.setKey(proxyFH.getFilePath());
		} else if (fileHandle instanceof ExternalObjectStoreFileHandle) {
			ExternalObjectStoreFileHandle externalObjectStoreFH = (ExternalObjectStoreFileHandle) fileHandle;
			snapshot.setKey(externalObjectStoreFH.getFileKey());
		} else {
			throw new IllegalArgumentException("Unexpected FileHandle Type:" + fileHandle.getClass().getName());
		}
		return snapshot;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<KinesisObjectSnapshotRecord<FileHandleSnapshot>> kinesisRecords = new ArrayList<>(messages.size());
		for (ChangeMessage message : messages) {
			
			if (message.getObjectType() != ObjectType.FILE) {
				throw new IllegalArgumentException();
			}
			
			if (message.getChangeType() == ChangeType.DELETE) {
				kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, new FileHandleSnapshot().setId(message.getObjectId())));
			} else {
				try {
					FileHandle fileHandle = fileHandleDao.get(message.getObjectId());
					FileHandleSnapshot snapshot = buildFileHandleSnapshot(fileHandle);
					kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, snapshot));
				} catch (NotFoundException e) {
					log.error("Cannot find FileHandle for a " + message.getChangeType() + " message: " + message.toString()) ;
				}
			}
		}
		if (!kinesisRecords.isEmpty()) {
			kinesisLogger.logBatch(KINESIS_STREAM, kinesisRecords);
		}
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.FILE;
	}

}
