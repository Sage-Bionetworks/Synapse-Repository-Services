package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.mail.Folder;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.events.EventsCollector;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEvent;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;

@ExtendWith(MockitoExtension.class)
public class FileEntityMetadataProviderTest {
	private static final String FILE_HANDLE_ID = "file-handle-id";
	private static final String PARENT_ENTITY_ID = "parent-entity-id";

	private static final long STS_STORAGE_LOCATION_ID = 123;
	private static final long NON_STS_STORAGE_LOCATION_ID = 456;
	private static final long DIFFERENT_STS_STORAGE_LOCATION_ID = 789;

	@Mock
	private FileHandleManager mockFileHandleManager;

	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;

	@Mock
	private EventsCollector mockStatisticsCollector;

	@InjectMocks
	@Spy
	private FileEntityMetadataProvider provider;

	private FileEntity fileEntity;
	private S3FileHandle fileHandle;
	private S3StorageLocationSetting storageLocationSetting;
	private UploadDestinationListSetting projectSetting;
	private UserInfo userInfo;
	private List<EntityHeader> path;

	@BeforeEach
	public void before() {

		fileEntity = new FileEntity();
		fileEntity.setId("syn789");
		fileEntity.setDataFileHandleId(FILE_HANDLE_ID);
		fileEntity.setParentId(PARENT_ENTITY_ID);

		fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);

		projectSetting = new UploadDestinationListSetting();

		storageLocationSetting = new S3StorageLocationSetting();

		userInfo = new UserInfo(false, 55L);

		// root
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId("123");
		grandparentHeader.setName("gp");
		grandparentHeader.setType(Folder.class.getName());
		path = new ArrayList<>();
		path.add(grandparentHeader);

		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId("456");
		parentHeader.setName("p");
		parentHeader.setType(Folder.class.getName());
		path.add(parentHeader);
	}

	@Test
	public void testValidateCreateWithoutDataFileHandleId() {
		fileEntity.setDataFileHandleId(null);
		assertThrows(IllegalArgumentException.class, () -> provider.validateEntity(fileEntity,
				new EntityEvent(EventType.CREATE, path, userInfo)), "FileEntity.dataFileHandleId cannot be null");
	}

	@Test
	public void testValidateCreateWithFileNameOverride() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			fileEntity.setDataFileHandleId("1");
			fileEntity.setFileNameOverride("fileNameOverride");
			provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
		});
	}

	@Test
	public void testValidateCreate() {
		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		doNothing().when(provider).validateFileEntityStsRestrictions(userInfo, fileEntity);

		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.CREATE, path, userInfo));
	}

	@Test
	public void testValidateCreate_ValidateFileEntityStsRestrictions() {
		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		String testErrorMessage = "test exception from validateFileEntityStsRestrictions";
		doThrow(new IllegalArgumentException(testErrorMessage)).when(provider).validateFileEntityStsRestrictions(
				userInfo, fileEntity);

		fileEntity.setDataFileHandleId("1");

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> provider.validateEntity(fileEntity,
				new EntityEvent(EventType.CREATE, path, userInfo)), testErrorMessage);
	}

	@Test
	public void testValidateUpdateWithoutDataFileHandleId() {
		fileEntity.setDataFileHandleId(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> provider.validateEntity(fileEntity,
				new EntityEvent(EventType.UPDATE, path, userInfo)), "FileEntity.dataFileHandleId cannot be null");
	}

	@Test
	public void testValidateUpdateWithNullOriginalFileNameOverride() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			fileEntity.setDataFileHandleId("1");
			fileEntity.setFileNameOverride("fileNameOverride");
			provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
		});
	}

	@Test
	public void testValidateUpdateWithNewFileNameOverride() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			fileEntity.setDataFileHandleId("1");
			fileEntity.setFileNameOverride("fileNameOverride");
			provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
		});
	}

	@Test
	public void testValidateUpdateWithoutFileNameOverride() {
		// Spy validateFileEntityStsRestrictions(). This is tested elsewhere and involves mocking a bunch of other
		// mocks that we don't want to deal with here.
		doNothing().when(provider).validateFileEntityStsRestrictions(userInfo, fileEntity);

		fileEntity.setDataFileHandleId("1");
		provider.validateEntity(fileEntity, new EntityEvent(EventType.UPDATE, path, userInfo));
	}

	@Test
	public void testEntityCreated() {
		fileEntity.setDataFileHandleId("1");
		provider.entityCreated(userInfo, fileEntity);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}

	@Test
	public void testEntityUpdatedWithNewVersion() {
		fileEntity.setDataFileHandleId("1");
		boolean wasNewVersionCreated = true;
		provider.entityUpdated(userInfo, fileEntity, wasNewVersionCreated);
		verify(mockStatisticsCollector, times(1)).collectEvent(any(StatisticsFileEvent.class));
	}

	@Test
	public void testEntityUpdatedWithoutNewVersion() {
		boolean wasNewVersionCreated = false;
		provider.entityUpdated(userInfo, fileEntity, wasNewVersionCreated);
		verify(mockStatisticsCollector, never()).collectEvent(any());
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInSameStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		projectSetting.setLocations(ImmutableList.of(STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Does not throw.
		provider.validateFileEntityStsRestrictions(userInfo, fileEntity);
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInDifferentStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		projectSetting.setLocations(ImmutableList.of(DIFFERENT_STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> provider.validateFileEntityStsRestrictions(userInfo,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInNonStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		projectSetting.setLocations(ImmutableList.of(NON_STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> provider.validateFileEntityStsRestrictions(userInfo,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_StsFileInParentWithoutProjectSettings() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(true);

		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.empty());

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> provider.validateFileEntityStsRestrictions(userInfo,
				fileEntity), "Files in STS-enabled storage locations can only be placed in " +
				"folders with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_NonStsFileInStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(NON_STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(false);

		projectSetting.setLocations(ImmutableList.of(STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> provider.validateFileEntityStsRestrictions(userInfo,
				fileEntity), "Folders with STS-enabled storage locations can only accept " +
				"files with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_FileWithoutStorageLocationInStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(null);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		when(mockProjectSettingsManager.getStorageLocationSetting(null)).thenReturn(null);
		when(mockProjectSettingsManager.isStsStorageLocationSetting((StorageLocationSetting) null)).thenReturn(false);

		projectSetting.setLocations(ImmutableList.of(STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> provider.validateFileEntityStsRestrictions(userInfo,
				fileEntity), "Folders with STS-enabled storage locations can only accept " +
				"files with the same storage location");
	}

	@Test
	public void validateFileEntityStsRestrictions_NonStsFileInNonStsParent() {
		// Mock dependencies.
		fileHandle.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

		storageLocationSetting.setStorageLocationId(NON_STS_STORAGE_LOCATION_ID);
		when(mockProjectSettingsManager.getStorageLocationSetting(NON_STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);
		when(mockProjectSettingsManager.isStsStorageLocationSetting(storageLocationSetting)).thenReturn(false);

		projectSetting.setLocations(ImmutableList.of(NON_STS_STORAGE_LOCATION_ID));
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Method under test - Does not throw.
		provider.validateFileEntityStsRestrictions(userInfo, fileEntity);
	}
}
