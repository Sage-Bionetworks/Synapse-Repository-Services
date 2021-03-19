package org.sagebionetworks.repo.manager.sts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.DBOStorageLocationDAOImpl;
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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

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
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private StackConfiguration stackConfiguration;

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
		userInfo.setAcceptsTermsOfUse(true);

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
		// Only run this test if the STS Arn is set up.
		Assumptions.assumeTrue(stackConfiguration.getTempCredentialsIamRoleArn() != null);

		// Setup - Create 4 files:
		//   [bucket]/inaccessible.txt
		//   [bucket]/[sts root]/owner.txt
		//   [bucket]/[sts root]/file.txt
		//   [bucket]/[sts root]/subfolder/file.txt
		// This is to verify that we can't list or read the bucket root, but we can list and read the STS folder root
		// and the subfolder.
		String testFolderPath = "StsManagerImplAutowiredTest-" + UUID.randomUUID().toString();
		String subFolderPath = testFolderPath + "/subfolder";
		uploadFileToS3(null, "inaccessible.txt", "Dummy file content");
		uploadFileToS3(testFolderPath, "owner.txt", username);
		uploadFileToS3(testFolderPath, "file.txt", "Additional dummy file content");
		uploadFileToS3(subFolderPath, "file.txt", "More dummy file content");

		// Create StsStorageLocation.
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBaseKey(testFolderPath);
		storageLocationSetting.setBucket(EXTERNAL_S3_BUCKET);
		storageLocationSetting.setStsEnabled(true);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, storageLocationSetting);

		applyStorageLocationToFolder(storageLocationSetting.getStorageLocationId());

		// Get read-only credentials.
		AmazonS3 readOnlyTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_only,
				EXTERNAL_S3_BUCKET, testFolderPath);

		// Cannot list or read at the bucket root.
		// Note that we call getObjectMetadata() instead of getObject(). This is because getObject() opens up an HTTP
		// connection and has to be closed manually by the caller. We don't want to incur that overhead in these tests.
		// GetObject includes both the S3 object itself and the metadata, so the permissions are the same on both.
		AmazonServiceException ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.listObjects(
				EXTERNAL_S3_BUCKET));
		assertEquals(403, ex.getStatusCode());
		ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.getObjectMetadata(
				EXTERNAL_S3_BUCKET, "inaccessible.txt"));
		assertEquals(403, ex.getStatusCode());

		// Can list and read at the STS root.
		readOnlyTempClient.listObjects(EXTERNAL_S3_BUCKET, testFolderPath);
		readOnlyTempClient.getObjectMetadata(EXTERNAL_S3_BUCKET, testFolderPath + "/file.txt");

		// Cannot read owner.txt.
		ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.getObjectMetadata(EXTERNAL_S3_BUCKET,
				testFolderPath + "/owner.txt"));
		assertEquals(403, ex.getStatusCode());

		// Can list and read in a subfolder.
		readOnlyTempClient.listObjects(EXTERNAL_S3_BUCKET, subFolderPath);
		readOnlyTempClient.getObjectMetadata(EXTERNAL_S3_BUCKET, subFolderPath + "/file.txt");

		// Validate cannot write to S3. This call will throw.
		String filenameToWrite = RandomStringUtils.randomAlphabetic(4) + ".txt";
		ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.putObject(
				EXTERNAL_S3_BUCKET, testFolderPath + "/" + filenameToWrite, "lorem ipsum"));
		assertEquals(403, ex.getStatusCode());

		// Get read-write credentials.
		AmazonS3 readWriteTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_write,
				EXTERNAL_S3_BUCKET, testFolderPath);

		// Cannot list or read at the bucket root.
		ex = assertThrows(AmazonServiceException.class, () -> readWriteTempClient.listObjects(
				EXTERNAL_S3_BUCKET));
		assertEquals(403, ex.getStatusCode());
		ex = assertThrows(AmazonServiceException.class, () -> readWriteTempClient.getObjectMetadata(
				EXTERNAL_S3_BUCKET, "inaccessible.txt"));
		assertEquals(403, ex.getStatusCode());

		// Can list and read at the STS root.
		readWriteTempClient.listObjects(EXTERNAL_S3_BUCKET, testFolderPath);
		readWriteTempClient.getObjectMetadata(EXTERNAL_S3_BUCKET, testFolderPath + "/file.txt");

		// Cannot read owner.txt.
		ex = assertThrows(AmazonServiceException.class, () -> readWriteTempClient.getObjectMetadata(EXTERNAL_S3_BUCKET,
				testFolderPath + "/owner.txt"));
		assertEquals(403, ex.getStatusCode());

		// Can list and read in a subfolder.
		readWriteTempClient.listObjects(EXTERNAL_S3_BUCKET, subFolderPath);
		readWriteTempClient.getObjectMetadata(EXTERNAL_S3_BUCKET, subFolderPath + "/file.txt");

		// Validate can write to S3. This call will not throw.
		readWriteTempClient.putObject(EXTERNAL_S3_BUCKET, testFolderPath + "/" + filenameToWrite,
				"lorem ipsum");

		// Cannot write to owner.txt.
		ex = assertThrows(AmazonServiceException.class, () -> readWriteTempClient.putObject(
				EXTERNAL_S3_BUCKET, testFolderPath + "/owner.txt", "lorem ipsum"));
		assertEquals(403, ex.getStatusCode());
	}

	@Test
	public void synapseStorage() throws Exception {
		// Only run this test if the STS Arn is set up.
		Assumptions.assumeTrue(stackConfiguration.getTempCredentialsIamRoleArn() != null);

		// Create StsStorageLocation.
		S3StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setStsEnabled(true);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, storageLocationSetting);
		String baseKey = storageLocationSetting.getBaseKey();

		long storageLocationId = storageLocationSetting.getStorageLocationId();
		applyStorageLocationToFolder(storageLocationId);

		// Upload a file to our STS storage location and another file to default Synapse storage location.
		S3FileHandle stsFileHandle = uploadFileToSynapseStorage(folderId, storageLocationId);
		S3FileHandle defaultFileHandle = uploadFileToSynapseStorage(projectId,
				DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);

		// Get read-only credentials.
		AmazonS3 readOnlyTempClient = createS3ClientFromTempStsCredentials(StsPermission.read_only, SYNAPSE_BUCKET,
				baseKey);

		// Can list and read files inside of our base key.
		readOnlyTempClient.listObjects(SYNAPSE_BUCKET, baseKey);
		readOnlyTempClient.getObjectMetadata(SYNAPSE_BUCKET, stsFileHandle.getKey());

		// Cannot list or read files outside of our base key.
		AmazonServiceException ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.listObjects(
				SYNAPSE_BUCKET));
		assertEquals(403, ex.getStatusCode());
		ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.getObjectMetadata(SYNAPSE_BUCKET,
				defaultFileHandle.getKey()));
		assertEquals(403, ex.getStatusCode());

		// Validate that we cannot write to S3. This call will throw.
		String filenameToWrite = RandomStringUtils.randomAlphabetic(4) + ".txt";
		ex = assertThrows(AmazonServiceException.class, () -> readOnlyTempClient.putObject(
				SYNAPSE_BUCKET, baseKey + "/" + filenameToWrite, "lorem ipsum"));
		assertEquals(403, ex.getStatusCode());

		// Get read-write credentials.
		// Read-write credentials are not allowed for Synapse storage.
		IllegalArgumentException synapseException = assertThrows(IllegalArgumentException.class,
				() -> stsManager.getTemporaryCredentials(userInfo, folderId, StsPermission.read_write));
		assertEquals("STS write access is not allowed in Synapse storage", synapseException.getMessage());
	}

	private void applyStorageLocationToFolder(long storageLocationId) {
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(ImmutableList.of(storageLocationId));
		projectSetting.setProjectId(folderId);
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSettingsManager.createProjectSetting(userInfo, projectSetting);
	}

	private AmazonS3 createS3ClientFromTempStsCredentials(StsPermission permission, String expectedBucket,
			String expectedBaseKey) {
		StsCredentials stsCredentials = stsManager.getTemporaryCredentials(userInfo, folderId, permission);
		assertEquals(expectedBucket, stsCredentials.getBucket());
		assertEquals(expectedBaseKey, stsCredentials.getBaseKey());

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
		if (folder != null) {
			om.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(folder));
		}
		om.setContentLength(bytes.length);

		String key;
		if (folder != null) {
			key = folder + "/" + filename;
		} else {
			key = filename;
		}
		s3Client.putObject(EXTERNAL_S3_BUCKET, key, new ByteArrayInputStream(bytes), om);
	}

	private S3FileHandle uploadFileToSynapseStorage(String parentId, long storageLocationId) throws Exception {
		File file = File.createTempFile("StsManagerImplAutowiredTest-", ".txt");
		filesToDelete.add(file);
		Files.asCharSink(file, StandardCharsets.UTF_8).write("Dummy test file content");

		LocalFileUploadRequest uploadRequest = new LocalFileUploadRequest().withContentType("text/plain")
				.withFileToUpload(file).withStorageLocationId(storageLocationId)
				.withUserId(userInfo.getId().toString());
		S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(uploadRequest);
		fileHandlesToDelete.add(fileHandle);

		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setParentId(parentId);
		entityManager.createEntity(userInfo, fileEntity, null);

		return fileHandle;
	}
}
