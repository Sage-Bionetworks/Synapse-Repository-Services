package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.FileHandleSnapshot;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileHandleSnapshotRecordWriter implements ObjectRecordWriter {

	static private Logger log = LogManager.getLogger(FileHandleSnapshotRecordWriter.class);
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;

	FileHandleSnapshotRecordWriter(){}

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
		snapshot.setFileName(fileHandle.getFileName());
		snapshot.setId(fileHandle.getId());
		snapshot.setStorageLocationId(fileHandle.getStorageLocationId());
		if (fileHandle instanceof CloudProviderFileHandleInterface) {
			CloudProviderFileHandleInterface s3FH = (CloudProviderFileHandleInterface) fileHandle;
			snapshot.setBucket(s3FH.getBucketName());
			snapshot.setKey(s3FH.getKey());
		} else if (fileHandle instanceof ExternalFileHandle) {
			ExternalFileHandle externalFH = (ExternalFileHandle) fileHandle;
			snapshot.setKey(externalFH.getExternalURL());
		} else if (fileHandle instanceof ProxyFileHandle) {
			ProxyFileHandle proxyFH = (ProxyFileHandle) fileHandle;
			snapshot.setKey(proxyFH.getFilePath());
		} else if (fileHandle instanceof ExternalObjectStoreFileHandle) {
			ExternalObjectStoreFileHandle externalObjectStoreFH = (ExternalObjectStoreFileHandle) fileHandle;
			snapshot.setKey(externalObjectStoreFH.getFileKey());
		}else{
			throw new IllegalArgumentException("Unexpected FileHandle Type:" + fileHandle.getClass().getName());
		}
		return snapshot;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<ObjectRecord> toWrite = new LinkedList<ObjectRecord>();
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.FILE) {
				throw new IllegalArgumentException();
			}
			// skip delete messages
			if (message.getChangeType() == ChangeType.DELETE) {
				continue;
			}
			try {
				FileHandle fileHandle = fileHandleDao.get(message.getObjectId());
				FileHandleSnapshot snapshot = buildFileHandleSnapshot(fileHandle);
				ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(snapshot, message.getTimestamp().getTime());
				toWrite.add(objectRecord);
			} catch (NotFoundException e) {
				log.error("Cannot find FileHandle for a " + message.getChangeType() + " message: " + message.toString()) ;
			}
		}
		if (!toWrite.isEmpty()) {
			objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
		}
	}

}
