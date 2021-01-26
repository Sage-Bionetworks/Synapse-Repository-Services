package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.file.services.FileUploadService;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TrashServiceImplAutowiredTest {
	@Autowired
	private AdministrationService adminService;

	// Bypass Auth Service to sign terms of use.
	@Autowired
	private AuthenticationManager authManager;

	@Autowired
	private CertifiedUserService certifiedUserService;

	@Autowired
	private FileUploadService fileUploadService;

	@Autowired
	private EntityService entityService;

	// Used only to create test file handles.
	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private ProjectSettingsService projectSettingsService;

	@Autowired
	private TrashService trashService;

	private List<Entity> entitiesToDelete;
	private List<S3FileHandle> fileHandlesToDelete;
	private List<File> filesToDelete;

	private Long adminUserId;
	private String projectId;
	private Long userId;

	@BeforeEach
	public void beforeEach() {
		// Set up lists of entities to delete.
		entitiesToDelete = new ArrayList<>();
		fileHandlesToDelete = new ArrayList<>();
		filesToDelete = new ArrayList<>();

		// Set up test user.
		adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		userId = createUser();

		// Set up test project.
		Project project = new Project();
		String projectName = "project" + new Random().nextInt();
		project.setName(projectName);
		project = entityService.createEntity(userId, project, null);
		entitiesToDelete.add(project);
		projectId = project.getId();
	}

	private long createUser() {
		// Create the user.
		NewIntegrationTestUser user = new NewIntegrationTestUser();
		String username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUsername(username);
		EntityId userEntityId = adminService.createOrGetTestUser(adminUserId, user);
		long createdUserId = Long.valueOf(userEntityId.getId());
		certifiedUserService.setUserCertificationStatus(adminUserId, createdUserId, true);

		// Before we can create file entities, we must agree to terms of use.
		authManager.setTermsOfUseAcceptance(createdUserId, true);

		return createdUserId;
	}

	@AfterEach
	public void afterEach() {
		// Delete entities.
		for (Entity entity : Lists.reverse(entitiesToDelete)) {
			entityService.deleteEntity(userId, entity.getId());
		}

		// Delete file handles.
		for (S3FileHandle fileHandle : fileHandlesToDelete) {
			fileUploadService.deleteFileHandle(fileHandle.getId(), userId);
		}

		// Delete local files.
		for (File file : filesToDelete) {
			file.delete();
		}
	}

	@Test
	public void fileHandleNonOwnerCanDeleteAndRestoreFileEntity() throws Exception {
		// Create another user and give them access to the project.
		long user2Id = createUser();

		ResourceAccess userAccess = new ResourceAccess();
		userAccess.setPrincipalId(userId);
		userAccess.setAccessType(ModelConstants.ENTITY_ADMIN_ACCESS_PERMISSIONS);

		ResourceAccess user2Access = new ResourceAccess();
		user2Access.setPrincipalId(user2Id);
		user2Access.setAccessType(EnumSet.of(ACCESS_TYPE.CREATE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE,
				ACCESS_TYPE.DELETE));

		AccessControlList acl = entityService.getEntityACL(projectId, userId);
		acl.setResourceAccess(ImmutableSet.of(userAccess, user2Access));
		entityService.updateEntityACL(userId, acl);

		// Upload a file to the project (with user 1).
		FileEntity fileEntity = uploadFile(projectId, null);
		String fileEntityId = fileEntity.getId();

		// User 2 can delete and restore the file.
		trashService.moveToTrash(user2Id, fileEntityId, false);
		assertThrows(EntityInTrashCanException.class, () -> entityService.getEntity(user2Id, fileEntityId));

		trashService.restoreFromTrash(user2Id, fileEntityId, null);
		FileEntity restored = entityService.getEntity(user2Id, fileEntityId, FileEntity.class);
		assertNotNull(restored);
	}

	@Test
	public void trashAndRestoreStsFile() throws Exception {
		// Create folder A, which is STS-enabled.
		Folder folderA = createFolder(projectId);
		long storageLocationId = createStsStorageLocation();
		applyStorageLocationToFolder(folderA, storageLocationId);

		// Create folder B, with the same STS storage location.
		Folder folderB = createFolder(projectId);
		applyStorageLocationToFolder(folderB, storageLocationId);

		// Upload file to folder A.
		FileEntity fileEntity = uploadFile(folderA.getId(), storageLocationId);
		String fileEntityId = fileEntity.getId();

		// Trash the file entity. Getting the file entity will now throw an EntityInTrashCanException.
		trashService.moveToTrash(userId, fileEntityId, false);
		assertThrows(EntityInTrashCanException.class, () -> entityService.getEntity(userId, fileEntityId));

		// Restore the file entity. It is gettable now.
		trashService.restoreFromTrash(userId, fileEntityId, null);
		FileEntity restored = entityService.getEntity(userId, fileEntityId, FileEntity.class);
		assertNotNull(restored);

		// Trash the file entity again and attempt to restore to folder B. This fails, because you cannot restore a
		// file to an STS-enabled folder, unless it was the original parent.
		trashService.moveToTrash(userId, fileEntityId, false);
		assertThrows(IllegalArgumentException.class, () -> trashService.restoreFromTrash(userId, fileEntityId,
				folderB.getId()));

		// Delete the storage location from folder B. Now restoring to it works because we don't have the restriction
		// on non-STS-enabled folders.
		deleteStorageLocationFromFolder(folderB);
		trashService.restoreFromTrash(userId, fileEntityId, folderB.getId());
		restored = entityService.getEntity(userId, fileEntityId, FileEntity.class);
		assertNotNull(restored);
	}

	@Test
	public void trashAndRestoreStsFolder() throws Exception {
		// Create folder A, which is STS-enabled.
		Folder folderA = createFolder(projectId);
		long storageLocationId = createStsStorageLocation();
		applyStorageLocationToFolder(folderA, storageLocationId);

		// Create folder B, with the same STS storage location.
		Folder folderB = createFolder(projectId);
		applyStorageLocationToFolder(folderB, storageLocationId);

		// Create a subfolder in folder A.
		Folder subfolder = createFolder(folderA.getId());
		String subfolderId = subfolder.getId();

		// Trash the subfolder. Getting the subfolder will now throw an EntityInTrashCanException.
		trashService.moveToTrash(userId, subfolderId, false);
		assertThrows(EntityInTrashCanException.class, () -> entityService.getEntity(userId, subfolderId));

		// Restore the subfolder. It is gettable now.
		trashService.restoreFromTrash(userId, subfolderId, null);
		Folder restored = entityService.getEntity(userId, subfolderId, Folder.class);
		assertNotNull(restored);

		// Trash the subfolder again and attempt to restore to folder B. This fails, because you cannot restore a
		// file to an STS-enabled folder, unless it was the original parent.
		trashService.moveToTrash(userId, subfolderId, false);
		assertThrows(IllegalArgumentException.class, () -> trashService.restoreFromTrash(userId, subfolderId,
				folderB.getId()));

		// Delete the storage location from folder B. Now restoring to it works because we don't have the restriction
		// on non-STS-enabled folders.
		deleteStorageLocationFromFolder(folderB);
		trashService.restoreFromTrash(userId, subfolderId, folderB.getId());
		restored = entityService.getEntity(userId, subfolderId, Folder.class);
		assertNotNull(restored);
	}

	private Folder createFolder(String parentId) {
		Folder folder = new Folder();
		folder.setParentId(parentId);
		folder = entityService.createEntity(userId, folder, null);
		entitiesToDelete.add(folder);
		return folder;
	}

	private long createStsStorageLocation() throws Exception {
		S3StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setStsEnabled(true);
		storageLocationSetting = (S3StorageLocationSetting) projectSettingsService.createStorageLocationSetting(userId,
				storageLocationSetting);
		return storageLocationSetting.getStorageLocationId();
	}

	private void applyStorageLocationToFolder(Folder folder, long storageLocationId) {
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(ImmutableList.of(storageLocationId));
		projectSetting.setProjectId(folder.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSettingsService.createProjectSetting(userId, projectSetting);
	}

	private void deleteStorageLocationFromFolder(Folder folder) {
		ProjectSetting projectSetting = projectSettingsService.getProjectSettingByProjectAndType(userId,
				folder.getId(), ProjectSettingsType.upload);
		projectSettingsService.deleteProjectSetting(userId, projectSetting.getId());
	}

	private FileEntity uploadFile(String parentId, Long storageLocationId) throws Exception {
		// Create the file.
		File file = File.createTempFile("TrashServiceImplAutowiredTest", ".txt");
		filesToDelete.add(file);
		Files.asCharSink(file, StandardCharsets.UTF_8).write("dummy content");

		// Create file handle.
		LocalFileUploadRequest uploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(file).withStorageLocationId(storageLocationId).withUserId(userId.toString());
		S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(uploadRequest);
		fileHandlesToDelete.add(fileHandle);

		// Create file entity.
		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setParentId(parentId);
		fileEntity = entityService.createEntity(userId, fileEntity, null);
		entitiesToDelete.add(fileEntity);
		return fileEntity;
	}
}
