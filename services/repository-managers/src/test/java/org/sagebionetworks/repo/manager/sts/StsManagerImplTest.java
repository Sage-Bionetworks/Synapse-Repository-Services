package org.sagebionetworks.repo.manager.sts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class StsManagerImplTest {
	private static final String AWS_ACCESS_KEY = "dummy-access-key";
	private static final String AWS_ROLE_ARN = "dummy-role-arn";
	private static final String AWS_SECRET_KEY = "dummy-secret-key";
	private static final String AWS_SESSION_TOKEN = "dummy-session-token";
	private static final Date AWS_EXPIRATION_DATE = DateTime.parse("2020-02-17T17:26:17.379-0800").toDate();
	private static final String BASE_KEY = "my-base-key";
	private static final String BUCKET = "my-bucket";
	private static final String EXPECTED_BUCKET_WITH_BASE_KEY = "my-bucket/my-base-key";
	private static final String FILE_HANDLE_ID = "file-handle-id";
	private static final String FOLDER_ID = "syn1111";
	private static final String PARENT_ENTITY_ID = "syn2222";
	private static final String NEW_PARENT_ID = "syn3333";
	private static final String OLD_PARENT_ID = "syn4444";
	private static final long USER_ID = 1234;

	private static final UserInfo USER_INFO = new UserInfo(false, USER_ID);
	private static final String EXPECTED_STS_SESSION_NAME = "sts-" + USER_ID + "-" + PARENT_ENTITY_ID;

	private static final long STS_STORAGE_LOCATION_ID = 123;
	private static final long NON_STS_STORAGE_LOCATION_ID = 456;
	private static final long DIFFERENT_STS_STORAGE_LOCATION_ID = 789;

	@Mock
	private EntityAuthorizationManager mockAuthManager;

	@Mock
	private AuthorizationStatus mockAuthStatus;

	@Mock
	private FileHandleManager mockFileHandleManager;

	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;

	@Mock
	private StackConfiguration mockStackConfiguration;

	@Mock
	private AWSSecurityTokenService mockStsClient;

	@InjectMocks
	private StsManagerImpl stsManager;

	@Test
	public void getTemporaryCredentials_noProjectSetting() {
		setupFolderWithoutProjectSetting();

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.getTemporaryCredentials(USER_INFO,
				PARENT_ENTITY_ID, StsPermission.read_only));
		assertEquals("Entity must have a project setting", ex.getMessage());
	}

	@Test
	public void getTemporaryCredentials_notSts() {
		setupFolderWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.getTemporaryCredentials(USER_INFO,
				PARENT_ENTITY_ID, StsPermission.read_only));
		assertEquals("Entity must have an STS-enabled storage location", ex.getMessage());
	}

	@Test
	public void getTemporaryCredentials_readOnly() {
		// Mock dependencies.
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);

		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(BUCKET);
		storageLocationSetting.setStsEnabled(true);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);

		when(mockAuthManager.hasAccess(same(USER_INFO), eq(PARENT_ENTITY_ID),any()))
				.thenReturn(mockAuthStatus);

		mockSts();

		// Method under test - Does not throw.
		StsCredentials result = stsManager.getTemporaryCredentials(USER_INFO, PARENT_ENTITY_ID,
				StsPermission.read_only);
		assertStsCredentials(result);
		assertEquals(BUCKET, result.getBucket());
		assertNull(result.getBaseKey());

		// Verify policy document.
		ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(
				AssumeRoleRequest.class);
		verify(mockStsClient).assumeRole(requestCaptor.capture());
		AssumeRoleRequest request = requestCaptor.getValue();
		assertEquals(EXPECTED_STS_SESSION_NAME, request.getRoleSessionName());
		assertEquals(StsManagerImpl.DURATION_SECONDS, request.getDurationSeconds());
		assertEquals(AWS_ROLE_ARN, request.getRoleArn());

		String policy = request.getPolicy();
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "\""));
		assertTrue(policy.contains("{\"s3:prefix\":[\"\"]}"));
		assertTrue(policy.contains("{\"s3:prefix\":[\"*\"]}"));
		assertTrue(policy.contains("\"s3:GetObject\",\"s3:ListBucket\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "/*\""));

		// Verify auth.
		verify(mockAuthManager).hasAccess(USER_INFO, PARENT_ENTITY_ID, ACCESS_TYPE.DOWNLOAD);
		verifyNoMoreInteractions(mockAuthManager);
		verify(mockAuthStatus).checkAuthorizationOrElseThrow();
	}

	@Test
	public void getTemporaryCredentials_readWrite() {
		// Mock dependencies.
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);

		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(BUCKET);
		storageLocationSetting.setStsEnabled(true);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);

		when(mockAuthManager.hasAccess(same(USER_INFO), eq(PARENT_ENTITY_ID), any()))
				.thenReturn(mockAuthStatus);

		mockSts();

		// Method under test - Does not throw.
		StsCredentials result = stsManager.getTemporaryCredentials(USER_INFO, PARENT_ENTITY_ID,
				StsPermission.read_write);
		assertStsCredentials(result);
		assertEquals(BUCKET, result.getBucket());
		assertNull(result.getBaseKey());

		// Verify policy document.
		ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(
				AssumeRoleRequest.class);
		verify(mockStsClient).assumeRole(requestCaptor.capture());
		AssumeRoleRequest request = requestCaptor.getValue();
		assertEquals(EXPECTED_STS_SESSION_NAME, request.getRoleSessionName());
		assertEquals(StsManagerImpl.DURATION_SECONDS, request.getDurationSeconds());
		assertEquals(AWS_ROLE_ARN, request.getRoleArn());

		String policy = request.getPolicy();
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "\""));
		assertTrue(policy.contains("{\"s3:prefix\":[\"\"]}"));
		assertTrue(policy.contains("{\"s3:prefix\":[\"*\"]}"));
		assertTrue(policy.contains("\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListBucket\"," +
				"\"s3:ListMultipartUploadParts\",\"s3:PutObject\",\"s3:ListBucketMultipartUploads\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "/*\""));

		// Verify auth.
		verify(mockAuthManager).hasAccess(USER_INFO, PARENT_ENTITY_ID, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE);
		verifyNoMoreInteractions(mockAuthManager);
		verify(mockAuthStatus, times(1)).checkAuthorizationOrElseThrow();
	}

	@Test
	public void getTemporaryCredentials_withBaseKey() {
		// Mock dependencies.
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);

		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(BUCKET);
		storageLocationSetting.setBaseKey(BASE_KEY);
		storageLocationSetting.setStsEnabled(true);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);

		when(mockAuthManager.hasAccess(same(USER_INFO), eq(PARENT_ENTITY_ID), any()))
				.thenReturn(mockAuthStatus);

		mockSts();

		// Method under test - Does not throw.
		StsCredentials result = stsManager.getTemporaryCredentials(USER_INFO, PARENT_ENTITY_ID,
				StsPermission.read_only);
		assertStsCredentials(result);
		assertEquals(BUCKET, result.getBucket());
		assertEquals(BASE_KEY, result.getBaseKey());

		// Verify policy document.
		ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(
				AssumeRoleRequest.class);
		verify(mockStsClient).assumeRole(requestCaptor.capture());
		AssumeRoleRequest request = requestCaptor.getValue();
		assertEquals(EXPECTED_STS_SESSION_NAME, request.getRoleSessionName());
		assertEquals(StsManagerImpl.DURATION_SECONDS, request.getDurationSeconds());
		assertEquals(AWS_ROLE_ARN, request.getRoleArn());

		String policy = request.getPolicy();
		assertTrue(policy.contains("\"arn:aws:s3:::" + BUCKET + "\""));
		assertTrue(policy.contains("{\"s3:prefix\":[\"" + BASE_KEY + "\"]}"));
		assertTrue(policy.contains("{\"s3:prefix\":[\"" + BASE_KEY + "/*\"]}"));
		assertTrue(policy.contains("\"s3:GetObject\",\"s3:ListBucket\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + EXPECTED_BUCKET_WITH_BASE_KEY + "\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + EXPECTED_BUCKET_WITH_BASE_KEY + "/*\""));

		// Verify auth.
		verify(mockAuthManager).hasAccess(USER_INFO, PARENT_ENTITY_ID, ACCESS_TYPE.DOWNLOAD);
		verifyNoMoreInteractions(mockAuthManager);
		verify(mockAuthStatus).checkAuthorizationOrElseThrow();
	}

	@Test
	public void getTemporaryCredentials_synapseStorage() {
		// Mock dependencies.
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);

		S3StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setBaseKey(BASE_KEY);
		storageLocationSetting.setStsEnabled(true);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);

		when(mockAuthManager.hasAccess(same(USER_INFO), eq(PARENT_ENTITY_ID), any()))
				.thenReturn(mockAuthStatus);

		mockSts();

		String expectedBucket = StackConfigurationSingleton.singleton().getS3Bucket();
		String expectedBucketWithBaseKey = expectedBucket + "/" + BASE_KEY;

		// Method under test - Does not throw.
		StsCredentials result = stsManager.getTemporaryCredentials(USER_INFO, PARENT_ENTITY_ID,
				StsPermission.read_only);
		assertStsCredentials(result);
		assertEquals(expectedBucket, result.getBucket());
		assertEquals(BASE_KEY, result.getBaseKey());

		// Verify policy document.
		ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(
				AssumeRoleRequest.class);
		verify(mockStsClient).assumeRole(requestCaptor.capture());
		AssumeRoleRequest request = requestCaptor.getValue();
		assertEquals(EXPECTED_STS_SESSION_NAME, request.getRoleSessionName());
		assertEquals(StsManagerImpl.DURATION_SECONDS, request.getDurationSeconds());
		assertEquals(AWS_ROLE_ARN, request.getRoleArn());

		String policy = request.getPolicy();
		assertTrue(policy.contains("\"arn:aws:s3:::" + expectedBucket + "\""));
		assertTrue(policy.contains("{\"s3:prefix\":[\"" + BASE_KEY + "\"]}"));
		assertTrue(policy.contains("{\"s3:prefix\":[\"" + BASE_KEY + "/*\"]}"));
		assertTrue(policy.contains("\"s3:GetObject\",\"s3:ListBucket\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + expectedBucketWithBaseKey + "\""));
		assertTrue(policy.contains("\"arn:aws:s3:::" + expectedBucketWithBaseKey + "/*\""));

		// Verify auth.
		verify(mockAuthManager).hasAccess(USER_INFO, PARENT_ENTITY_ID, ACCESS_TYPE.DOWNLOAD);
		verifyNoMoreInteractions(mockAuthManager);
		verify(mockAuthStatus).checkAuthorizationOrElseThrow();
	}

	@Test
	public void getTemporaryCredentials_synapseStorageCantReadWrite() {
		// Mock dependencies.
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);

		S3StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setBaseKey(BASE_KEY);
		storageLocationSetting.setStsEnabled(true);
		when(mockProjectSettingsManager.getStorageLocationSetting(STS_STORAGE_LOCATION_ID)).thenReturn(
				storageLocationSetting);

		// Method under test - Throws.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> stsManager.getTemporaryCredentials(USER_INFO, PARENT_ENTITY_ID, StsPermission.read_write));
		assertEquals("STS write access is not allowed in Synapse storage", ex.getMessage());
	}

	private void mockSts() {
		// Mock config needed to set up the call.
		when(mockStackConfiguration.getTempCredentialsIamRoleArn()).thenReturn(AWS_ROLE_ARN);

		// Mock the actual STS call.
		Credentials credentials = new Credentials(AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_SESSION_TOKEN,
				AWS_EXPIRATION_DATE);
		AssumeRoleResult result = new AssumeRoleResult().withCredentials(credentials);
		when(mockStsClient.assumeRole(any())).thenReturn(result);
	}

	private void assertStsCredentials(StsCredentials credentials) {
		assertEquals(AWS_ACCESS_KEY, credentials.getAccessKeyId());
		assertEquals(AWS_SECRET_KEY, credentials.getSecretAccessKey());
		assertEquals(AWS_SESSION_TOKEN, credentials.getSessionToken());
		assertEquals(AWS_EXPIRATION_DATE, credentials.getExpiration());
	}

	@Test
	public void validateCanAddFile_StsFileInSameStsParent() {
		setupFile(/*isSts*/ true);
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanAddFile(USER_INFO, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void validateCanAddFile_StsFileInDifferentStsParent() {
		setupFile(/*isSts*/ true);
		setupFolderWithProjectSetting(/*isSts*/ true, DIFFERENT_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Files in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_StsFileInNonStsParent() {
		setupFile(/*isSts*/ true);
		setupFolderWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Files in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_StsFileInParentWithoutProjectSettings() {
		setupFile(/*isSts*/ true);
		setupFolderWithoutProjectSetting();
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Files in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_NonStsFileInStsParent() {
		setupFile(/*isSts*/ false);
		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanAddFile(USER_INFO,
				FILE_HANDLE_ID, PARENT_ENTITY_ID));
		assertEquals("Folders with STS-enabled storage locations can only accept files with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanAddFile_NonStsFileInNonStsParent() {
		setupFile(/*isSts*/ false);
		setupFolderWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanAddFile(USER_INFO, FILE_HANDLE_ID, PARENT_ENTITY_ID);
	}

	@Test
	public void validateCanAddFile_NonStsFileInParentWithoutProjectSettings() {
		setupFile(/*isSts*/ false);
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

		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);
		when(mockProjectSettingsManager.getStorageLocationSetting(null)).thenReturn(null);
		when(mockProjectSettingsManager.isStsStorageLocationSetting((StorageLocationSetting) null)).thenReturn(false);

		setupFolderWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);

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
		when(mockFileHandleManager.getRawFileHandleUnchecked(FILE_HANDLE_ID)).thenReturn(fileHandle);

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
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, OLD_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ true);
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ true);
		setupNewParentWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToDifferentStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ true);
		setupNewParentWithProjectSetting(/*isSts*/ true, DIFFERENT_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Cannot place an STS-enabled folder inside another STS-enabled folder", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveRootStsFolderToSameStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ true);
		setupNewParentWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Cannot place an STS-enabled folder inside another STS-enabled folder", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveRootNonStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ false);
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootNonStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ false);
		setupNewParentWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveRootNonStsFolderToStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ true, /*isSts*/ false);
		setupNewParentWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Non-STS-enabled folders cannot be placed inside STS-enabled folders", ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ true);
		setupNewParentWithoutProjectSetting();
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Folders in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ true);
		setupNewParentWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Folders in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToDifferentStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ true);
		setupNewParentWithProjectSetting(/*isSts*/ true, DIFFERENT_STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Folders in STS-enabled storage locations can only be placed in folders with the same storage location",
				ex.getMessage());
	}

	@Test
	public void validateCanMoveFolder_moveNonRootStsFolderToSameStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ true);
		setupNewParentWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveNonRootNonStsFolderToParentWithoutProjectSettings() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ false);
		setupNewParentWithoutProjectSetting();
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveNonRootNonStsFolderToNonStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ false);
		setupNewParentWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveNonRootNonStsFolderToStsParent() {
		setupOldFolderWithProjectSetting(/*isRoot*/ false, /*isSts*/ false);
		setupNewParentWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
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
		setupNewParentWithProjectSetting(/*isSts*/ false, NON_STS_STORAGE_LOCATION_ID);
		// Method under test - Does not throw.
		stsManager.validateCanMoveFolder(USER_INFO, FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID);
	}

	@Test
	public void validateCanMoveFolder_moveFolderWithoutProjectSettingsToStsParent() {
		setupOldFolderWithoutProjectSetting();
		setupNewParentWithProjectSetting(/*isSts*/ true, STS_STORAGE_LOCATION_ID);
		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> stsManager.validateCanMoveFolder(USER_INFO,
				FOLDER_ID, OLD_PARENT_ID, NEW_PARENT_ID));
		assertEquals("Non-STS-enabled folders cannot be placed inside STS-enabled folders", ex.getMessage());
	}

	private void setupOldFolderWithoutProjectSetting() {
		// No project setting on neither the folder nor the old parent.
		when(mockProjectSettingsManager.getProjectSettingByProjectAndType(USER_INFO, FOLDER_ID,
				ProjectSettingsType.upload)).thenReturn(Optional.empty());
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
			when(mockProjectSettingsManager.getProjectSettingByProjectAndType(USER_INFO, FOLDER_ID,
					ProjectSettingsType.upload)).thenReturn(Optional.of(oldProjectSetting));
		} else {
			when(mockProjectSettingsManager.getProjectSettingByProjectAndType(USER_INFO, FOLDER_ID,
					ProjectSettingsType.upload)).thenReturn(Optional.empty());
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
