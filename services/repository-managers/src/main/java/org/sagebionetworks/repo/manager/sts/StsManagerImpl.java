package org.sagebionetworks.repo.manager.sts;

import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class StsManagerImpl implements StsManager {
	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Override
	public void validateCanAddFile(UserInfo userInfo, String fileHandleId, String parentId) {
		// Is the file STS-enabled?
		// Note that getRawFileHandle throws if the file handle exists, but the storage location ID might be null.
		FileHandle fileHandle = fileHandleManager.getRawFileHandleUnchecked(fileHandleId);
		Long fileStorageLocationId = fileHandle.getStorageLocationId();
		StorageLocationSetting fileStorageLocationSetting = projectSettingsManager.getStorageLocationSetting(
				fileStorageLocationId);
		boolean fileStsEnabled = projectSettingsManager.isStsStorageLocationSetting(fileStorageLocationSetting);

		// Is the parent STS-enabled?
		Long parentStorageLocationId = null;
		boolean parentStsEnabled = false;
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

	@Override
	public void validateCanMoveFolder(UserInfo userInfo, String moveCandidateId, String oldParentId, String newParentId) {
		if (oldParentId.equals(newParentId)) {
			// Folder is not being moved. Trivial.
			return;
		}

		// Folder is being moved. STS restrictions may apply.
		boolean isCandidateStsRoot = false;
		boolean oldStsEnabled = false;
		Long oldStorageLocationId = null;
		boolean newStsEnabled = false;
		Long newStorageLocationId = null;

		// If the project setting is defined on the folder directly (ie the folder is a "root folder"), special logic
		// applies.
		Optional<ProjectSetting> folderProjectSetting = projectSettingsManager.getProjectSettingByProjectAndType(
				userInfo, moveCandidateId, ProjectSettingsType.upload);
		if (folderProjectSetting.isPresent()) {
			isCandidateStsRoot = true;

			// Short-cut: Just grab the first storage location ID. We only compare storage location IDs if STS is
			// enabled, and folders with STS enabled can't have multiple storage locations.
			oldStsEnabled = projectSettingsManager.isStsStorageLocationSetting(folderProjectSetting.get());
			oldStorageLocationId = ((UploadDestinationListSetting) folderProjectSetting.get()).getLocations().get(0);
		} else {
			// If the project setting isn't defined on the folder directly, check the project settings in the old
			// parent's hierarchy. Note that we do it like this because this validation is called for both folder moves
			// and for restoring from the trash can, so the folder's current parent hierarchy might not match the
			// original parent hierarchy.
			Optional<UploadDestinationListSetting> oldParentProjectSetting = projectSettingsManager
					.getProjectSettingForNode(userInfo, oldParentId, ProjectSettingsType.upload,
							UploadDestinationListSetting.class);
			if (oldParentProjectSetting.isPresent()) {
				// Similar shortcut as per above.
				oldStsEnabled = projectSettingsManager.isStsStorageLocationSetting(oldParentProjectSetting.get());
				oldStorageLocationId = oldParentProjectSetting.get().getLocations().get(0);
			}
		}

		// Check new parent project settings.
		Optional<UploadDestinationListSetting> newProjectSetting = projectSettingsManager
				.getProjectSettingForNode(userInfo, newParentId, ProjectSettingsType.upload,
						UploadDestinationListSetting.class);
		if (newProjectSetting.isPresent()) {
			// Similar shortcut as per above.
			newStsEnabled = projectSettingsManager.isStsStorageLocationSetting(newProjectSetting.get());
			newStorageLocationId = newProjectSetting.get().getLocations().get(0);
		}

		if (isCandidateStsRoot && oldStsEnabled) {
			// The folder that we are moving is itself an STS-enabled folder. This is fine, as long as we
			// aren't moving it into another STS-enabled folder. Note that even if the other STS-enabled folder
			// is the same storage location, this is still a problem because it violates the "can't override
			// project settings" constraint and causes weird things to happen.
			if (newStsEnabled) {
				throw new IllegalArgumentException("Cannot place an STS-enabled folder inside another " +
						"STS-enabled folder");
			}
		} else if ((oldStsEnabled || newStsEnabled) && !Objects.equals(oldStorageLocationId,
				newStorageLocationId)) {
			// If the storage location is different, this means we're moving into or out of an STS-enabled
			// folder, which is not allowed.
			// Determine which error message to show depending on whether we're moving into our out of an
			// STS-enabled folder.
			if (oldStsEnabled) {
				throw new IllegalArgumentException("Folders in STS-enabled storage locations can only " +
						"be placed in folders with the same storage location");
			}
			//noinspection ConstantConditions
			if (newStsEnabled) {
				throw new IllegalArgumentException("Non-STS-enabled folders cannot be placed inside " +
						"STS-enabled folders");
			}
		}
	}
}
