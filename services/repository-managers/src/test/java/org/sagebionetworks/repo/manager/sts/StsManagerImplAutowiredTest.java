package org.sagebionetworks.repo.manager.sts;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.file.MultipartManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.util.ContentDispositionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StsManagerImplAutowiredTest {
	private static final StackConfiguration CONFIG = StackConfigurationSingleton.singleton();
	private static final String EXTERNAL_S3_BUCKET = CONFIG.getExternalS3TestBucketName();
	private static final String SYNAPSE_BUCKET = CONFIG.getS3Bucket();

	@Autowired
	private AuthenticationManager authManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private MultipartManager multipartManager;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private StsManager stsManager;

	@Autowired
	public UserManager userManager;

	private List<FileHandle> fileHandlesToDelete;
	private List<File> filesToDelete;
	private String folderId;
	private String projectId;
	private UserInfo userInfo;
	private String username;

	@BeforeEach
	public void beforeEach() {
		// Create test user.
		NewUser user = new NewUser();
		username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		// User must agree to terms of use to get download privileges (and hence access the STS API).
		authManager.setTermsOfUseAcceptance(userInfo.getId(), true);

		// Create a test project which we will need.
		Project project = new Project();
		projectId = entityManager.createEntity(userInfo, project, null);

		// Create folder, which is required for STS.
		Folder folder = new Folder();
		folder.setParentId(projectId);
		folderId = entityManager.createEntity(userInfo, folder, null);

		// Initialize lists of things to delete.
		fileHandlesToDelete = new ArrayList<>();
		filesToDelete = new ArrayList<>();
	}

	@AfterEach
	public void afterEach() {
		// Delete project.
		//noinspection deprecation
		entityManager.deleteEntity(userInfo, projectId);

		// Delete file handles.
		for (FileHandle fileHandle : fileHandlesToDelete) {
			fileHandleManager.deleteFileHandle(userInfo, fileHandle.getId());
		}

		// Delete local files.
		for (File file : filesToDelete) {
			file.delete();
		}

		// Delete test user.
		UserInfo adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
				.getPrincipalId());
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo.getId().toString()));
	}

	@Test
	public void externalS3() throws Exception {
		// Setup - In our test folder, create an inner folder. STS will be configured on the inner folder. STS
		// credentials will be able to access files in the inner folder, but not the outer folder.
		String outerFolderPath = "integration-test/" + "StsManagerImplAutowiredTest-" + UUID.randomUUID().toString();
		String innerFolderPath = outerFolderPath + "/storage-location-root";
		uploadFileToS3(outerFolderPath, "inaccessible.txt", "Dummy file content");
		uploadFileToS3(innerFolderPath, "owner.txt", username);

		// Create StsStorageLocation.
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBaseKey(innerFolderPath);
		storageLocationSetting.setBucket(EXTERNAL_S3_BUCKET);
		storageLocationSetting.setStsEnabled(true);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, storageLocationSetting);

		applyStorageLocationToFolder(storageLocationSetting.getStorageLocationId());

		// Get read-only credentials.
		AmazonS3 readOnlyTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_only);

		// Validate we can list the bucket from the base key. This call does not throw.
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(EXTERNAL_S3_BUCKET)
				.withDelimiter("/").withPrefix(innerFolderPath);
		readOnlyTempClient.listObjects(listObjectsRequest);

		// Validate can read the owner.txt. The get call will not throw.
		readOnlyTempClient.getObject(EXTERNAL_S3_BUCKET, innerFolderPath + "/owner.txt");

		// Validate cannot get the inaccessible.txt in the outer folder. This call will throw.
		AmazonServiceException ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.getObject(
				EXTERNAL_S3_BUCKET, outerFolderPath + "/inaccessible.txt"));
		assertEquals(403, ex.getStatusCode());

		// Validate cannot write to S3. This call will throw.
		String filenameToWrite = RandomStringUtils.randomAlphabetic(4) + ".txt";
		ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.putObject(
				EXTERNAL_S3_BUCKET, innerFolderPath + "/" + filenameToWrite, "lorem ipsum"));
		assertEquals(403, ex.getStatusCode());

		// Get read-write credentials.
		AmazonS3 readWriteTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_write);

		// Validate we can list the bucket from the base key. This call does not throw.
		listObjectsRequest = new ListObjectsRequest().withBucketName(EXTERNAL_S3_BUCKET)
				.withDelimiter("/").withPrefix(innerFolderPath);
		readWriteTempClient.listObjects(listObjectsRequest);

		// Validate can read the owner.txt. The get call will not throw.
		readWriteTempClient.getObject(EXTERNAL_S3_BUCKET, innerFolderPath + "/owner.txt");

		// Validate cannot get the inaccessible.txt in the outer folder. This call will throw.
		ex = assertThrows(AmazonServiceException.class, () -> readWriteTempClient.getObject(
				EXTERNAL_S3_BUCKET, outerFolderPath + "/inaccessible.txt"));
		assertEquals(403, ex.getStatusCode());

		// Validate can write to S3. This call will not throw.
		readWriteTempClient.putObject(EXTERNAL_S3_BUCKET, innerFolderPath + "/" + filenameToWrite,
				"lorem ipsum");
	}

	@Test
	public void synapseStorage() throws Exception {
		// Create StsStorageLocation.
		S3StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setStsEnabled(true);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, storageLocationSetting);
		String baseKey = storageLocationSetting.getBaseKey();

		long storageLocationId = storageLocationSetting.getStorageLocationId();
		applyStorageLocationToFolder(storageLocationId);

		// Upload a file to our storage location.
		File file = File.createTempFile("ITStsTest_synapseStorage", ".txt");
		filesToDelete.add(file);
		Files.asCharSink(file, StandardCharsets.UTF_8).write(
				"Test file in Synapse storage location with STS");

		LocalFileUploadRequest uploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(file).withStorageLocationId(storageLocationId)
				.withUserId(userInfo.getId().toString());
		S3FileHandle fileHandle = multipartManager.multipartUploadLocalFile(uploadRequest);
		fileHandlesToDelete.add(fileHandle);

		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setParentId(folderId);
		entityManager.createEntity(userInfo, fileEntity, null);

		// Get read-only credentials.
		AmazonS3 readOnlyTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_only);

		// Validate we can list the bucket from the base key. This call does not throw.
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(SYNAPSE_BUCKET)
				.withDelimiter("/").withPrefix(baseKey);
		readOnlyTempClient.listObjects(listObjectsRequest);

		// Validate that we can read our file. This call will not throw.
		readOnlyTempClient.getObject(SYNAPSE_BUCKET, fileHandle.getKey());

		// Validate that we cannot write to S3. This call will throw.
		String filenameToWrite = RandomStringUtils.randomAlphabetic(4) + ".txt";
		AmazonServiceException ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.putObject(
				SYNAPSE_BUCKET, baseKey + "/" + filenameToWrite, "lorem ipsum"));
		assertEquals(403, ex.getStatusCode());

		// Get read-write credentials.
		AmazonS3 readWriteTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_write);

		// Validate we can list the bucket from the base key. This call does not throw.
		listObjectsRequest = new ListObjectsRequest().withBucketName(SYNAPSE_BUCKET)
				.withDelimiter("/").withPrefix(baseKey);
		readWriteTempClient.listObjects(listObjectsRequest);

		// Validate that we can read our file. This call will not throw.
		readWriteTempClient.getObject(SYNAPSE_BUCKET, fileHandle.getKey());

		// Validate that we can write to S3. This call will not throw.
		readWriteTempClient.putObject(SYNAPSE_BUCKET, baseKey + "/" + filenameToWrite, "lorem ipsum");
	}

	private void applyStorageLocationToFolder(long storageLocationId) {
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(ImmutableList.of(storageLocationId));
		projectSetting.setProjectId(folderId);
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSettingsManager.createProjectSetting(userInfo, projectSetting);
	}

	private AmazonS3 createS3ClientFromTempStsCredentials(StsPermission permission) {
		StsCredentials stsCredentials = stsManager.getTemporaryCredentials(userInfo, folderId, permission);

		AWSCredentials awsCredentials = new BasicSessionCredentials(stsCredentials.getAccessKeyId(),
				stsCredentials.getSecretAccessKey(), stsCredentials.getSessionToken());
		AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);

		return AmazonS3ClientBuilder.standard().withCredentials(awsCredentialsProvider).build();
	}

	private void uploadFileToS3(String folder, String filename, String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("text/plain");
		om.setContentEncoding("UTF-8");
		om.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(folder));
		om.setContentLength(bytes.length);
		s3Client.putObject(EXTERNAL_S3_BUCKET, folder + "/" + filename, new ByteArrayInputStream(bytes),
				om);
	}
}
