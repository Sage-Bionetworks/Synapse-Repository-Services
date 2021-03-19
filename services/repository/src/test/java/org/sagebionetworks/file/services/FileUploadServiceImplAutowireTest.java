package org.sagebionetworks.file.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
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
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.web.service.AdministrationService;
import org.sagebionetworks.repo.web.service.CertifiedUserService;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.ProjectSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileUploadServiceImplAutowireTest {
	@Autowired
	private AdministrationService adminService;

	// Bypass Auth Service to sign terms of use.
	@Autowired
	private AuthenticationManager authManager;

	@Autowired
	private CertifiedUserService certifiedUserService;

	@Autowired
	private EntityService entityService;

	@Autowired
	private FileUploadService fileUploadService;

	// Used only to test multipart upload.
	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private ProjectSettingsService projectSettingsService;

	@Autowired
	private SynapseS3Client s3Client;

	private List<Entity> entitiesToDelete;
	private List<S3FileHandle> fileHandlesToDelete;
	private List<File> filesToDelete;

	private String projectId;
	private Long userId;
	private String username;
	private Long user2Id;

	@BeforeEach
	public void beforeEach() throws Exception {
		// Set up lists of entities to delete.
		entitiesToDelete = new ArrayList<>();
		fileHandlesToDelete = new ArrayList<>();
		filesToDelete = new ArrayList<>();

		// Set up test user.
		Long adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		NewIntegrationTestUser user = new NewIntegrationTestUser();
		username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUsername(username);
		user.setSession(new Session().setAcceptsTermsOfUse(true));
		EntityId userEntityId = adminService.createOrGetTestUser(adminUserId, user);
		userId = Long.valueOf(userEntityId.getId());
		certifiedUserService.setUserCertificationStatus(adminUserId, userId, true);

		NewIntegrationTestUser user2 = new NewIntegrationTestUser();
		String user2name = UUID.randomUUID().toString();
		user2.setEmail(user2name + "@test.com");
		user2.setUsername(user2name);
		user2.setSession(new Session().setAcceptsTermsOfUse(true));
		EntityId user2EntityId = adminService.createOrGetTestUser(adminUserId, user2);
		user2Id = Long.valueOf(user2EntityId.getId());
		certifiedUserService.setUserCertificationStatus(adminUserId, user2Id, true);

		// Set up test project.
		Project project = new Project();
		String projectName = "project" + new Random().nextInt();
		project.setName(projectName);
		project = entityService.createEntity(userId, project, null);
		entitiesToDelete.add(project);
		projectId = project.getId();

		// Give ACL to user2.
		ResourceAccess userAccess = new ResourceAccess();
		userAccess.setPrincipalId(userId);
		userAccess.setAccessType(ModelConstants.ENTITY_ADMIN_ACCESS_PERMISSIONS);

		ResourceAccess user2Access = new ResourceAccess();
		user2Access.setPrincipalId(user2Id);
		user2Access.setAccessType(EnumSet.of(ACCESS_TYPE.CREATE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE));

		AccessControlList acl = entityService.getEntityACL(projectId, userId);
		acl.setResourceAccess(ImmutableSet.of(userAccess, user2Access));
		entityService.updateEntityACL(userId, acl);
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
	public void uploadWithSts() throws Exception {
		// Set up bucket and owner.txt.
		String externalS3Bucket = StackConfigurationSingleton.singleton().getExternalS3TestBucketName();
		String externalS3StorageBaseKey = "test-base-" + UUID.randomUUID();
		s3Client.createBucket(externalS3Bucket);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(username.length());
		s3Client.putObject(externalS3Bucket, externalS3StorageBaseKey + "/owner.txt",
				new StringInputStream(username), metadata);

		// Create Synapse Storage and External S3 Storage w/ STS.
		S3StorageLocationSetting synapseStorageLocationSetting = new S3StorageLocationSetting();
		synapseStorageLocationSetting.setStsEnabled(true);
		synapseStorageLocationSetting = (S3StorageLocationSetting) projectSettingsService
				.createStorageLocationSetting(userId, synapseStorageLocationSetting);
		long synapseStorageLocationId = synapseStorageLocationSetting.getStorageLocationId();
		String synapseStorageBaseKey = synapseStorageLocationSetting.getBaseKey();
		assertNotNull(synapseStorageBaseKey);
		assertNotEquals(externalS3StorageBaseKey, synapseStorageBaseKey);

		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(externalS3Bucket);
		externalS3StorageLocationSetting.setBaseKey(externalS3StorageBaseKey);
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) projectSettingsService
				.createStorageLocationSetting(userId, externalS3StorageLocationSetting);
		long externalS3StorageLocationId = externalS3StorageLocationSetting.getStorageLocationId();
		assertNotEquals(synapseStorageLocationId, externalS3StorageLocationId);
		assertEquals(externalS3StorageBaseKey, externalS3StorageLocationSetting.getBaseKey());

		// Upload to Synapse Storage.
		File synapseFile = File.createTempFile("uploadWithSts-Synapse", ".txt");
		filesToDelete.add(synapseFile);
		Files.asCharSink(synapseFile, StandardCharsets.UTF_8).write(
				"Test file in Synapse storage location with STS");
		LocalFileUploadRequest synapseUploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(synapseFile).withStorageLocationId(synapseStorageLocationId)
				.withUserId(userId.toString());

		S3FileHandle synapseFileHandle = fileHandleManager.uploadLocalFile(synapseUploadRequest);
		assertNotNull(synapseFileHandle);
		assertEquals(synapseStorageLocationId, synapseFileHandle.getStorageLocationId());
		fileHandlesToDelete.add(synapseFileHandle);
		assertNotNull(synapseFileHandle.getBucketName());
		assertTrue(synapseFileHandle.getKey().startsWith(synapseStorageBaseKey));

		// Verify file exists in S3.
		assertTrue(s3Client.doesObjectExist(synapseFileHandle.getBucketName(), synapseFileHandle.getKey()));

		// Upload to External S3 Storage.
		File externalS3File = File.createTempFile("uploadWithSts-ExternalS3", ".txt");
		filesToDelete.add(externalS3File);
		Files.asCharSink(externalS3File, StandardCharsets.UTF_8).write(
				"Test file in external S3 storage location with STS");
		LocalFileUploadRequest externalS3UploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(externalS3File).withStorageLocationId(externalS3StorageLocationId)
				.withUserId(userId.toString());

		S3FileHandle externalS3FileHandle = fileHandleManager.uploadLocalFile(externalS3UploadRequest);
		assertNotNull(externalS3FileHandle);
		assertEquals(externalS3StorageLocationId, externalS3FileHandle.getStorageLocationId());
		fileHandlesToDelete.add(externalS3FileHandle);
		assertEquals(externalS3Bucket, externalS3FileHandle.getBucketName());
		assertTrue(externalS3FileHandle.getKey().startsWith(externalS3StorageBaseKey));

		// Verify file exists in S3.
		assertTrue(s3Client.doesObjectExist(externalS3FileHandle.getBucketName(), externalS3FileHandle.getKey()));

		// Attempt to create a new file handle that points at the same file. Even though there's a copy API that does
		// exactly this, we're specifically testing the more general createExternalS3FileHandle() API.)
		S3FileHandle externalS3FileHandleCopy = (S3FileHandle) fileUploadService.getFileHandle(
				externalS3FileHandle.getId(), userId);
		externalS3FileHandleCopy = fileUploadService.createExternalS3FileHandle(userId, externalS3FileHandleCopy);
		assertNotNull(externalS3FileHandleCopy);
		fileHandlesToDelete.add(externalS3FileHandleCopy);
		assertNotEquals(externalS3FileHandle.getId(), externalS3FileHandleCopy.getId());
		assertEquals(externalS3Bucket, externalS3FileHandleCopy.getBucketName());
		assertEquals(externalS3FileHandle.getKey(), externalS3FileHandleCopy.getKey());

		// Create folders for the project.
		Folder synapseFolder = new Folder();
		synapseFolder.setParentId(projectId);
		synapseFolder = entityService.createEntity(userId, synapseFolder, null);
		entitiesToDelete.add(synapseFolder);

		Folder externalS3Folder = new Folder();
		externalS3Folder.setParentId(projectId);
		externalS3Folder = entityService.createEntity(userId, externalS3Folder, null);
		entitiesToDelete.add(externalS3Folder);

		// Add storage locations to the folders.
		UploadDestinationListSetting synapseProjectSetting = new UploadDestinationListSetting();
		synapseProjectSetting.setProjectId(synapseFolder.getId());
		synapseProjectSetting.setSettingsType(ProjectSettingsType.upload);
		synapseProjectSetting.setLocations(ImmutableList.of(synapseStorageLocationId));
		projectSettingsService.createProjectSetting(userId, synapseProjectSetting);

		UploadDestinationListSetting externalS3ProjectSetting = new UploadDestinationListSetting();
		externalS3ProjectSetting.setProjectId(externalS3Folder.getId());
		externalS3ProjectSetting.setSettingsType(ProjectSettingsType.upload);
		externalS3ProjectSetting.setLocations(ImmutableList.of(externalS3StorageLocationId));
		projectSettingsService.createProjectSetting(userId, externalS3ProjectSetting);

		// Before we can create file entities, we must agree to terms of use.
		authManager.setTermsOfUseAcceptance(userId, true);

		// Create file entities for each file handle.
		FileEntity synapseFileEntity = new FileEntity();
		synapseFileEntity.setDataFileHandleId(synapseFileHandle.getId());
		synapseFileEntity.setParentId(synapseFolder.getId());
		synapseFileEntity = entityService.createEntity(userId, synapseFileEntity, null);
		entitiesToDelete.add(synapseFileEntity);

		FileEntity externalS3FileEntity = new FileEntity();
		externalS3FileEntity.setDataFileHandleId(externalS3FileHandle.getId());
		externalS3FileEntity.setParentId(externalS3Folder.getId());
		externalS3FileEntity = entityService.createEntity(userId, externalS3FileEntity, null);
		entitiesToDelete.add(externalS3FileEntity);

		// Create a file handle and file entity in the default storage location (such as project root).
		File nonStsFile = File.createTempFile("nonStsFile", ".txt");
		filesToDelete.add(nonStsFile);
		Files.asCharSink(nonStsFile, StandardCharsets.UTF_8).write("Test file in without STS");
		LocalFileUploadRequest nonStsUploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(nonStsFile).withStorageLocationId(null).withUserId(userId.toString());

		S3FileHandle nonStsFileHandle = fileHandleManager.uploadLocalFile(nonStsUploadRequest);
		fileHandlesToDelete.add(nonStsFileHandle);

		FileEntity nonStsFileEntity = new FileEntity();
		nonStsFileEntity.setDataFileHandleId(nonStsFileHandle.getId());
		nonStsFileEntity.setParentId(projectId);
		nonStsFileEntity = entityService.createEntity(userId, nonStsFileEntity, null);
		entitiesToDelete.add(nonStsFileEntity);
	}

	// PLFM-6097
	@Test
	public void fileHandleNonOwnerCanMoveFileEntity() throws Exception {
		// Create folders for the project.
		Folder folderA = new Folder();
		folderA.setParentId(projectId);
		folderA = entityService.createEntity(userId, folderA, null);
		entitiesToDelete.add(folderA);

		Folder folderB = new Folder();
		folderB.setParentId(projectId);
		folderB = entityService.createEntity(userId, folderB, null);
		entitiesToDelete.add(folderB);

		// Create a file handle.
		File file = File.createTempFile("plfm-6097-test-file", ".txt");
		filesToDelete.add(file);
		Files.asCharSink(file, StandardCharsets.UTF_8).write("Test file for PLFM-6097");

		LocalFileUploadRequest fileUploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(file).withUserId(userId.toString());
		S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(fileUploadRequest);
		fileHandlesToDelete.add(fileHandle);

		// Before we can create file entities, we must agree to terms of use.
		authManager.setTermsOfUseAcceptance(userId, true);

		// Make a FileEntity out of that FileHandle in Folder A.
		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setParentId(folderA.getId());
		fileEntity = entityService.createEntity(userId, fileEntity, null);
		entitiesToDelete.add(fileEntity);

		// User2 can move the FileEntity to Folder B.
		fileEntity.setParentId(folderB.getId());
		entityService.updateEntity(user2Id, fileEntity, false, null);
	}
}
