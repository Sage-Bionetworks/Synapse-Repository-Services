package org.sagebionetworks.repo.manager.sts;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StsManagerImplTest {
	private static final String FILE_HANDLE_ID = "file-handle-id";
	private static final String FOLDER_ID = "syn1111";
	private static final String PARENT_ENTITY_ID = "syn2222";
	private static final String NEW_PARENT_ID = "syn3333";
	private static final String OLD_PARENT_ID = "syn4444";
	private static final UserInfo USER_INFO = new UserInfo(false);

	private static final long STS_STORAGE_LOCATION_ID = 123;
	private static final long NON_STS_STORAGE_LOCATION_ID = 456;
	private static final long DIFFERENT_STS_STORAGE_LOCATION_ID = 789;

	@Mock
	private FileHandleManager mockFileHandleManager;

	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;

	@InjectMocks
	private StsManagerImpl stsManager;

	@Test
	public void validateCanAddFile_StsFileInSameStsParent() {
		setupFile(true);
		setupFolderWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanAddFile(USER_INFO, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void validateCanAddFile_StsFileInDifferentStsParent() {
		setupFile(true);
		setupFolderWithProjectSetting(true, DIFFERENT_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Files in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_StsFileInNonStsParent() {
		setupFile(true);
		setupFolderWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Files in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_StsFileInParentWithoutProjectSettings() {
		setupFile(true);
		setupFolderWithoutProjectSetting();
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Files in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_NonStsFileInStsParent() {
		setupFile(false);
		setupFolderWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Folders with STS-enabled storage locations can only accept files with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_NonStsFileInNonStsParent() {
		setupFile(false);
		setupFolderWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanAddFile(USER_INFO, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void validateCanAddFile_NonStsFileInParentWithoutProjectSettings() {
		setupFile(false);
		setupFolderWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanAddFile(USER_INFO, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void validateCanAddFile_FileWithoutStorageLocationInStsParent() {
		// Edge-case: Files can be created without a storage location. These go to Synapse default storage (which has
		// no STS).
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);
		fileHandle.setStorageLocationId(null);

		when(mockFileHandleManager.getRawFileHandle(USER_INFO, FILE_HANDLE_ID)).thenReturn(fileHandle);
		when(mockProjectSettingsManager.getStorageLocationSetting(null)).thenReturn(null);
		when(mockProjectSettingsManager.isStsStorageLocationSetting((StorageLocationSetting) null)).thenReturn(false);

		setupFolderWithProjectSetting(true, STS_STORAGE_LOCATION_ID);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Folders with STS-enabled storage locations can only accept files with the same storage location",
				ex.getMessage());
	}

	private void setupFile(boolean isSts) {
		long storageLocationId = isSts ? STS_STORAGE_LOCATION_ID : NON_STS_STORAGE_LOCATION_ID;

		// Mock file handle manager.
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);
		fileHandle.setStorageLocationId(storageLocationId);
		when(mockFileHandleManager.getRawFileHandle(USER_INFO, FILE_HANDLE_ID)).thenReturn(fileHandle);

		// Mock project settings manager.
		S3StorageLocationSetting fileStorageLocationSetting = new S3StorageLocationSetting();
		fileStorageLocationSetting.setStorageLocationId(storageLocationId);
		when(mockProjectSettingsManager.getStorageLocationSetting(storageLocationId)).thenReturn(
				fileStorageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(fileStorageLocationSetting)).thenReturn(isSts);
	}

	private void setupFolderWithoutProjectSetting() {
		when(mockProjectSettingsManager.getProjectSettingForNode(USER_INFO, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.empty());
	}

	private void setupFolderWithProjectSetting(boolean isSts, long folderStorageLocationId) {
		UploadDestinationListSetting folderProjectSetting = new UploadDestinationListSetting();
		folderProjectSetting.setLocations(ImmutableList.of(folderStorageLocationId));
		when(mockProjectSettingsManager.getProjectSettingForNode(USER_INFO, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(folderProjectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(folderProjectSetting)).thenReturn(isSts);
	}

	@Test
	public void validateCanMoveFolder_NotMoved() {
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, OLD_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(true, true);
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(true, true);
		setupNewParentWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToDifferentStsParent() {
		setupOldFolderWithProjectSetting(true, true);
		setupNewParentWithProjectSetting(true, DIFFERENT_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Cannot place an STS-enabled folder inside another STS-enabled folder", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToSameStsParent() {
		setupOldFolderWithProjectSetting(true, true);
		setupNewParentWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Cannot place an STS-enabled folder inside another STS-enabled folder", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveRootNonStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(true, false);
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootNonStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(true, false);
		setupNewParentWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootNonStsFolderToStsParent() {
		setupOldFolderWithProjectSetting(true, false);
		setupNewParentWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Non-STS-enabled folders cannot be placed inside STS-enabled folders", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(false, true);
		setupNewParentWithoutProjectSetting();
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Folders in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(false, true);
		setupNewParentWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Folders in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToDifferentStsParent() {
		setupOldFolderWithProjectSetting(false, true);
		setupNewParentWithProjectSetting(true, DIFFERENT_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Folders in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToSameStsParent() {
		setupOldFolderWithProjectSetting(false, true);
		setupNewParentWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveNonRootNonStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(false, false);
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveNonRootNonStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(false, false);
		setupNewParentWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveNonRootNonStsFolderToStsParent() {
		setupOldFolderWithProjectSetting(false, false);
		setupNewParentWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Non-STS-enabled folders cannot be placed inside STS-enabled folders", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveFolderWithoutProjectSettingsToParentWithoutProjectSettings() {
		setupOldFolderWithoutProjectSetting();
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveFolderWithoutProjectSettingsToNonStsParent() {
		setupOldFolderWithoutProjectSetting();
		setupNewParentWithProjectSetting(false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveFolderWithoutProjectSettingsToStsParent() {
		setupOldFolderWithoutProjectSetting();
		setupNewParentWithProjectSetting(true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Non-STS-enabled folders cannot be placed inside STS-enabled folders", ex.getMessage());
	}

	private void setupOldFolderWithoutProjectSetting() {
		// No project setting on neither the folder nor the old parent.
		when(mockProjectSettingsManager.getProjectSettingByEntityUnchecked(FOLDER_ID)).thenReturn(Optional.empty());
		when(mockProjectSettingsManager.getProjectSettingForNode(USER_INFO, OLD_PARENT_ID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.empty());
	}

	private void setupOldFolderWithProjectSetting(boolean isRoot, boolean isSts) {
		// If the old folder is the root, then the project setting is set on the FOLDER_ID. Otherwise, it's set on
		// the OLD_PARENT_ID.
		String settingProjectId = isRoot ? FOLDER_ID : OLD_PARENT_ID;
		long oldStorageLocationId = isSts ? STS_STORAGE_LOCATION_ID : NON_STS_STORAGE_LOCATION_ID;

		// Mock project settings manager.
		UploadDestinationListSetting oldProjectSetting = new UploadDestinationListSetting();
		oldProjectSetting.setProjectId(settingProjectId);
		oldProjectSetting.setLocations(ImmutableList.of(oldStorageLocationId));
		if (isRoot) {
			when(mockProjectSettingsManager.getProjectSettingByEntityUnchecked(FOLDER_ID)).thenReturn(
					Optional.of(oldProjectSetting));
		} else {
			when(mockProjectSettingsManager.getProjectSettingByEntityUnchecked(FOLDER_ID)).thenReturn(
					Optional.empty());
			when(mockProjectSettingsManager.getProjectSettingForNode(USER_INFO, OLD_PARENT_ID, ProjectSettingsType.upload,
					UploadDestinationListSetting.class)).thenReturn(Optional.of(oldProjectSetting));
		}

		when(mockProjectSettingsManager.isStsStorageLocationSetting(oldProjectSetting)).thenReturn(isSts);
	}

	private void setupNewParentWithoutProjectSetting() {
		when(mockProjectSettingsManager.getProjectSettingForNode(USER_INFO, NEW_PARENT_ID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.empty());
	}

	private void setupNewParentWithProjectSetting(boolean isSts, long newStorageLocationId) {
		// For simplicity of testing, the project setting is defined directly on the new parent.
		UploadDestinationListSetting newProjectSetting = new UploadDestinationListSetting();
		newProjectSetting.setProjectId(NEW_PARENT_ID);
		newProjectSetting.setLocations(ImmutableList.of(newStorageLocationId));
		when(mockProjectSettingsManager.getProjectSettingForNode(USER_INFO, NEW_PARENT_ID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(newProjectSetting));

		when(mockProjectSettingsManager.isStsStorageLocationSetting(newProjectSetting)).thenReturn(isSts);
	}
}
