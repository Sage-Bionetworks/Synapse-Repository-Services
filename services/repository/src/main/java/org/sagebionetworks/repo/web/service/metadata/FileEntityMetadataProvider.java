package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.events.EventsCollector;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEventUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;

public class FileEntityMetadataProvider implements EntityValidator<FileEntity>, TypeSpecificCreateProvider<FileEntity>,
		TypeSpecificUpdateProvider<FileEntity> {

	private static final String FILE_NAME_OVERRIDE_DEPRECATED_REASON = "fileNameOverride field is deprecated and should not be set.";

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private EventsCollector statisticsCollector;

	@Override
	public void validateEntity(FileEntity entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		if (EventType.CREATE == event.getType() || EventType.UPDATE == event.getType()
				|| EventType.NEW_VERSION == event.getType()) {
			// This is for PLFM-1754.
			if (entity.getDataFileHandleId() == null) {
				throw new IllegalArgumentException("FileEntity.dataFileHandleId cannot be null");
			}
			// PLFM-4108 - deprecate fileNameOverride
			if (entity.getFileNameOverride() != null) {
				throw new IllegalArgumentException(FILE_NAME_OVERRIDE_DEPRECATED_REASON);
			}

			validateFileEntityStsRestrictions(event.getUserInfo(), entity);
		}
	}

	@Override
	public void entityCreated(UserInfo userInfo, FileEntity entity) {
		sendFileUploadEvent(userInfo.getId(), entity);
	}

	@Override
	public void entityUpdated(UserInfo userInfo, FileEntity entity, boolean wasNewVersionCreated) {
		// A new file handle is attached only if a new version was created
		if (wasNewVersionCreated) {
			sendFileUploadEvent(userInfo.getId(), entity);
		}
	}

	private void sendFileUploadEvent(Long userId, FileEntity entity) {
		StatisticsFileEvent event = StatisticsFileEventUtils.buildFileUploadEvent(userId, entity.getDataFileHandleId(),
				entity.getId(), FileHandleAssociateType.FileEntity);
		statisticsCollector.collectEvent(event);
	}

	// Validates whether a FileEntity satisfies the STS restrictions of its parent.
	// Package-scoped to facilitate unit tests.
	void validateFileEntityStsRestrictions(UserInfo userInfo, FileEntity fileEntity) {
		// Is the file STS-enabled?
		// Note that getRawFileHandle throws if the file handle exists, but the storage location ID might be null.
		FileHandle fileHandle = fileHandleManager.getRawFileHandle(userInfo, fileEntity.getDataFileHandleId());
		Long fileStorageLocationId = fileHandle.getStorageLocationId();
		StorageLocationSetting fileStorageLocationSetting = projectSettingsManager.getStorageLocationSetting(
				fileStorageLocationId);
		boolean fileStsEnabled = projectSettingsManager.isStsStorageLocationSetting(fileStorageLocationSetting);

		// Is the parent STS-enabled? (Parent ID is validated as non-null by AllTypesValidatorImpl.)
		Long parentStorageLocationId = null;
		boolean parentStsEnabled = false;
		String parentId = fileEntity.getParentId();
		Optional<UploadDestinationListSetting> projectSetting = projectSettingsManager.getProjectSettingForNode(
				userInfo, parentId, ProjectSettingsType.upload, UploadDestinationListSetting.class);
		if (projectSetting.isPresent()) {
			// Short-cut: Just grab the first storage location ID. We only compare storage location IDs if STS is
			// enabled, and folders with STS enabled can't have multiple storage locations.
			parentStsEnabled = projectSettingsManager.isStsStorageLocationSetting(projectSetting.get());
			parentStorageLocationId = projectSetting.get().getLocations().get(0);
		}

		// If either the file's storage location or the parent's storage location has STS enabled, then the storage
		// locations must be the same. ie, Files in STS-enabled Storage Locations must be placed in a folder with the
		// same storage location, and folders with STS-enabled Storage Locations can only contain files from that
		// storage location.
		if ((fileStsEnabled || parentStsEnabled) && !Objects.equals(fileStorageLocationId, parentStorageLocationId)) {
			// Determine which error message to throw depending on whether the file is STS-enabled or the parent.
			if (fileStsEnabled) {
				throw new IllegalArgumentException("Files in STS-enabled storage locations can only be placed in " +
						"folders with the same storage location");
			}
			//noinspection ConstantConditions
			if (parentStsEnabled) {
				throw new IllegalArgumentException("Folders with STS-enabled storage locations can only accept " +
						"files with the same storage location");
			}
		}
	}
}
