package org.sagebionetworks.file.worker;

import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.SingletonWorker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.SyncFolderMessage;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.BinaryUtils;

public class SyncFolderWorker extends SingletonWorker {

	static private Logger log = LogManager.getLogger(SyncFolderWorker.class);

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AmazonS3Client s3Client;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ProjectSettingsManager projectSettingsManager;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private FileHandleDao fileHandleDao;

	private boolean autoSyncSubfoldersEnabled = StackConfiguration.singleton().getAutoSyncSubFoldersAllowed();

	private CountingSemaphoreDao syncFolderGate;

	@Required
	public void setSyncFolderGate(CountingSemaphoreDao syncFolderGate) {
		this.syncFolderGate = syncFolderGate;
	}

	/**
	 * This is where the real work happens
	 */
	@Override
	protected Message processMessage(Message message, WorkerProgress workerProgress) throws Throwable {
		SyncFolderMessage syncFolderMessage = extractStatus(message);
		try {
			UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
			ExternalSyncSetting externalSyncSetting = projectSettingsManager.getProjectSettingForNode(adminUserInfo,
					syncFolderMessage.getEntityId(), ProjectSettingsType.external_sync, ExternalSyncSetting.class);
			if (externalSyncSetting != null && BooleanUtils.isTrue(externalSyncSetting.getAutoSync())) {

				// only one worker can work on a project at a time
				String lockToken = syncFolderGate.attemptToAcquireLock(externalSyncSetting.getProjectId());
				if (lockToken == null) {
					// try again later
					return null;
				}
				try {
					handleAutoSync(externalSyncSetting, message, workerProgress);
				} finally {
					syncFolderGate.releaseLock(lockToken, externalSyncSetting.getProjectId());
				}
			}
		} catch (NotFoundException e) {
			// the entity no longer exists, no need to log that
		}
		return message;
	}

	private void handleAutoSync(ExternalSyncSetting externalSyncSetting, Message message, WorkerProgress workerProgress) {
		Long storageLocationId = externalSyncSetting.getLocationId();
		StorageLocationSetting storageLocationSetting = projectSettingsManager.getStorageLocationSetting(storageLocationId);
		if (!(storageLocationSetting instanceof ExternalS3StorageLocationSetting)) {
			// we only handle s3 external locations
			throw new IllegalArgumentException("Cannot handle storage location setting: " + storageLocationSetting);
		}
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
		externalS3StorageLocationSetting.getBucket();

		Node topNode = nodeDao.getNode(externalSyncSetting.getProjectId());

		// because creating the files checks the upload permissions of the user, but we explicitly deny upload
		// permissions to an autosync folder, we set the admin bit to true to go around all authorization checks (which
		// is correct, because any file created here just inherits all authorization from the parent)
		UserInfo owner = new UserInfo(true, topNode.getCreatedByPrincipalId());

		// list all keys under this bucket
		ObjectListing listObjects = s3Client.listObjects(externalS3StorageLocationSetting.getBucket(),
				externalS3StorageLocationSetting.getBaseKey());
		for (;;) {
			workerProgress.progressMadeForMessage(message);
			for (S3ObjectSummary objectSummary : listObjects.getObjectSummaries()) {
				syncNode(topNode, owner, externalSyncSetting.getLocationId(), externalSyncSetting.getProjectId(),
						externalS3StorageLocationSetting.getBaseKey(), objectSummary);
			}
			if (!listObjects.isTruncated()) {
				break;
			}
			s3Client.listNextBatchOfObjects(listObjects);
		}
	}

	private void syncNode(Node topNode, UserInfo owner, Long locationId, String nodeId, String baseKey, S3ObjectSummary objectSummary) {
		String key = objectSummary.getKey();
		if (!key.startsWith(baseKey)) {
			throw new IllegalStateException("The key " + key + " does not start with the prefix " + baseKey);
		}
		key = key.substring(baseKey.length());
		if (key.equals(ProjectSettingsManager.OWNER_MARKER)) {
			// we skip the owner marker here, assuming it should not show up
			return;
		}
		String[] path = key.split("/");

		int fileIndex = 0;
		if (path.length > 1) {
			if (!autoSyncSubfoldersEnabled) {
				// we don't support sub folders, ignore this key
				return;
			}

			// we support subfolders, let's create them
			while (fileIndex < path.length - 1) {
				String childName = path[fileIndex];
				nodeId = getOrCreateFolder(owner, nodeId, childName);
				fileIndex++;
			}
		}
		String fileName = path[fileIndex];
		fileCreateOrUpdateVersion(owner, locationId, nodeId, objectSummary, fileName);
	}

	private String getOrCreateFolder(UserInfo owner, String parentId, String childName) {
		try {
			EntityHeader childNode = nodeDao.getEntityHeaderByChildName(parentId, childName);
			if (!childNode.getType().equals(Folder.class.getName())) {
				throw new IllegalStateException("The child node " + childName + " of node " + parentId
						+ " is expected to be a Folder entity, but is a " + childNode.getType() + ". Cannot procede with auto-sync");
			}
			return childNode.getId();
		} catch (NotFoundException e) {
			// this means a necessary folder was not found at this location, let's create it
			Folder folder = new Folder();
			folder.setName(childName);
			folder.setParentId(parentId);
			String folderId = entityManager.createEntity(owner, folder, null);
			return folderId;
		}
	}

	private void fileCreateOrUpdateVersion(UserInfo owner, Long locationId, String parentId, S3ObjectSummary objectSummary, String fileName) {
		EntityHeader fileNode;
		try {
			fileNode = nodeDao.getEntityHeaderByChildName(parentId, fileName);
		} catch (NotFoundException e) {
			createFile(owner, locationId, parentId, objectSummary, fileName);
			return;
		}

		if (!fileNode.getType().equals(FileEntity.class.getName())) {
			throw new IllegalStateException("The child node " + fileName + " of node " + parentId
					+ " is expected to be a File entity, but is a " + fileNode.getType() + ". Cannot procede with auto-sync");
		}
		FileEntity fileEntity = entityManager.getEntity(owner, fileNode.getId(), FileEntity.class);
		FileHandle fileHandle = fileHandleDao.get(fileEntity.getDataFileHandleId());
		if (!(fileHandle instanceof S3FileHandle)) {
			throw new IllegalStateException("The child node " + fileName + " of node " + parentId
					+ " is expected to be an S3FileHandle, but is a " + fileHandle.getClass().getName() + ". Cannot procede with auto-sync");
		}
		if (!ObjectUtils.equals(((S3FileHandle) fileHandle).getContentMd5(), objectSummary.getETag())) {
			fileUpdate(fileEntity, owner, locationId, objectSummary, fileName);
		}
	}

	private void createFile(UserInfo owner, Long locationId, String parentId, S3ObjectSummary objectSummary, String fileName) {
		S3FileHandle fileHandle = createFileHandle(owner, locationId, objectSummary, fileName);

		FileEntity file = new FileEntity();
		file.setName(fileName);
		file.setParentId(parentId);
		file.setDataFileHandleId(fileHandle.getId());
		entityManager.createEntity(owner, file, null);
	}

	private void fileUpdate(FileEntity fileEntity, UserInfo owner, Long locationId, S3ObjectSummary objectSummary, String fileName) {
		S3FileHandle fileHandle = createFileHandle(owner, locationId, objectSummary, fileName);
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setVersionLabel(Long.toString(fileEntity.getVersionNumber() + 1));
		entityManager.updateEntity(owner, fileEntity, true, null);
	}

	private S3FileHandle createFileHandle(UserInfo owner, Long locationId, S3ObjectSummary objectSummary, String fileName) {
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setBucketName(objectSummary.getBucketName());
		fileHandle.setKey(objectSummary.getKey());
		String md5 = objectSummary.getETag();
		fileHandle.setContentMd5(md5);
		fileHandle.setFileName(fileName);
		fileHandle.setContentSize(objectSummary.getSize());
		fileHandle.setCreatedBy(owner.getId().toString());
		fileHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setStorageLocationId(locationId);
		fileHandle = fileHandleDao.createFile(fileHandle, false);
		return fileHandle;
	}

	/**
	 * Extract the AsynchUploadRequestBody from the message.
	 * 
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	SyncFolderMessage extractStatus(Message message) throws JSONObjectAdapterException {
		ValidateArgument.required(message, "message");
		if (message == null) {
			throw new IllegalArgumentException("Message cannot be null");
		}
		SyncFolderMessage syncFolderMessage = MessageUtils.readMessageBody(message, SyncFolderMessage.class);
		return syncFolderMessage;
	}
}
