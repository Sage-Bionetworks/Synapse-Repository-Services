package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.amazonaws.services.s3.model.S3Object;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class ProjectSettingsManagerImplUnitTest {
	private UserInfo userInfo;

	@InjectMocks
	private ProjectSettingsManagerImpl projectSettingsManagerImpl;

	private static final String PROJECT_ID = "3523";
	private static final String PROJECT_SETTINGS_ID = "21521";

	private static final String NODE_ID = "3524";

	private static final String USER_NAME = "user-name";
	private static final Long USER_ID = 101L;
	private static final String bucketName = "bucket.name";

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private AuthorizationManager authorizationManager;

	@Mock
	private UserProfileManager userProfileManager;

	@Mock
	private SynapseS3Client synapseS3Client;

	@Mock
	private SynapseGoogleCloudStorageClient synapseGoogleCloudStorageClient;

	@Mock
	private StorageLocationDAO mockStorageLocationDAO;

	@Mock
	private FileHandleDao mockFileHandleDao;

	@Mock
	private ProjectSettingsDAO mockProjectSettingDao;

	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;

	@Mock
	private BufferedReader mockBufferedReader;

	private UserProfile userProfile;
	private UploadDestinationListSetting uploadDestinationListSetting;
	private ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	private ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, USER_ID);
		userProfile = new UserProfile();
		userProfile.setUserName(USER_NAME);
		userProfile.setEmails(Arrays.asList("personal-email@mysite.net", "institutional-email@institution.edu"));

		uploadDestinationListSetting = new UploadDestinationListSetting();
		uploadDestinationListSetting.setProjectId(PROJECT_ID);
		uploadDestinationListSetting.setSettingsType(ProjectSettingsType.upload);
		uploadDestinationListSetting.setId(PROJECT_SETTINGS_ID);
		uploadDestinationListSetting.setEtag("etag");

		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(bucketName);

		externalGoogleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		externalGoogleCloudStorageLocationSetting.setBucket(bucketName);
	}

	@Test
	public void testGetBySettingId() {
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		// Call under test
		ProjectSetting actual = projectSettingsManagerImpl.getProjectSetting(userInfo, PROJECT_SETTINGS_ID);
		verify(authorizationManager).canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockProjectSettingDao).get(PROJECT_SETTINGS_ID);
		assertSame(uploadDestinationListSetting, actual);
	}

	@Test
	public void testGetBySettingIdNullProjectSettings() {
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(null);

		// Call under test
		assertThrows(NotFoundException.class, () -> projectSettingsManagerImpl.getProjectSetting(userInfo, PROJECT_SETTINGS_ID));
		verify(mockProjectSettingDao).get(PROJECT_SETTINGS_ID);
	}

	@Test
	public void testGetBySettingIdUnauthorized() {
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied("User doesn't have READ access on the project"));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> projectSettingsManagerImpl.getProjectSetting(userInfo, PROJECT_SETTINGS_ID));
		verify(authorizationManager).canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockProjectSettingDao).get(PROJECT_SETTINGS_ID);
	}

	@Test
	public void getSettingByProjectAndType() {
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockProjectSettingDao.get(PROJECT_ID, ProjectSettingsType.upload)).thenReturn(Optional.of(uploadDestinationListSetting));

		// Call under test
		ProjectSetting actual = projectSettingsManagerImpl.getProjectSettingByProjectAndType(userInfo, PROJECT_ID, ProjectSettingsType.upload).get();
		verify(authorizationManager).canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockProjectSettingDao).get(PROJECT_ID, ProjectSettingsType.upload);
		assertSame(uploadDestinationListSetting, actual);
	}

	@Test
	public void getSettingByProjectAndTypeUnauthorized() {
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied("User doesn't have READ access on the project"));

		// Call under test
		assertThrows(UnauthorizedException.class, () -> projectSettingsManagerImpl.getProjectSettingByProjectAndType(userInfo, PROJECT_ID, ProjectSettingsType.upload));
		verifyZeroInteractions(mockProjectSettingDao);
	}

	@Test
	public void getProjectSettingForNode() {
		EntityHeader projectHeader = new EntityHeader();
		projectHeader.setId(PROJECT_ID);
		when(mockNodeManager.getNodePathAsAdmin(NODE_ID)).thenReturn(Collections.singletonList(projectHeader));
		when(mockProjectSettingDao.get(eq(Collections.singletonList(Long.valueOf(PROJECT_ID))), eq(ProjectSettingsType.upload))).thenReturn(uploadDestinationListSetting);

		// Call under test
		ProjectSetting actual = projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID, ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertSame(uploadDestinationListSetting, actual);
	}

	@Test
	public void getProjectSettingForNodeEmptyPath() {
		EntityHeader projectHeader = new EntityHeader();
		projectHeader.setId(PROJECT_ID);
		when(mockNodeManager.getNodePathAsAdmin(NODE_ID)).thenReturn(Collections.emptyList());

		// Call under test
		assertThrows(DatastoreException.class, () -> projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID, ProjectSettingsType.upload, UploadDestinationListSetting.class));
	}

	@Test
	public void getProjectSettingForNodeWrongInstanceType() {
		EntityHeader projectHeader = new EntityHeader();
		projectHeader.setId(PROJECT_ID);
		when(mockNodeManager.getNodePathAsAdmin(NODE_ID)).thenReturn(Collections.singletonList(projectHeader));
		when(mockProjectSettingDao.get(eq(Collections.singletonList(Long.valueOf(PROJECT_ID))), eq(ProjectSettingsType.upload))).thenReturn(new TestProjectSettingStub());

		// Call under test
		assertThrows(DatastoreException.class, () -> projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID, ProjectSettingsType.upload, UploadDestinationListSetting.class));
	}

	@Test
	public void getUploadDestinationLocations() {
		List<Long> ids = Collections.singletonList(123L);
		List<UploadDestinationLocation> expected = Collections.singletonList(new UploadDestinationLocation());
		when(mockStorageLocationDAO.getUploadDestinationLocations(ids)).thenReturn(expected);

		// Call under test
		List<UploadDestinationLocation> actual = projectSettingsManagerImpl.getUploadDestinationLocations(userInfo, ids);
		assertSame(expected, actual);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_HappyCase() throws Exception {
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);

		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream(USER_NAME.getBytes()));
		when(synapseS3Client.getObject(bucketName, "owner.txt")).thenReturn(s3Object);

		when(mockStorageLocationDAO.create(externalS3StorageLocationSetting)).thenReturn(999L);

		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalS3StorageLocationSetting);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_UnsharedBucket() throws Exception {
		when(synapseS3Client.getRegionForBucket(bucketName)).thenThrow(new CannotDetermineBucketLocationException());

		assertThrows(CannotDetermineBucketLocationException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_InvalidS3BucketName() {
		externalS3StorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalObjectStorageLocationSetting_InvalidS3BucketName() {
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");
		externalObjectStorageLocationSetting.setEndpointUrl("https://myendpoint.com");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting() throws Exception {
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);
		when(synapseGoogleCloudStorageClient.bucketExists(bucketName)).thenReturn(true);
		when(synapseGoogleCloudStorageClient.getObject(bucketName, "owner.txt")).thenReturn(mock(Blob.class));
		when(synapseGoogleCloudStorageClient.getObjectContent(bucketName, "owner.txt")).thenReturn(IOUtils.toInputStream(USER_NAME, StandardCharsets.UTF_8));
		when(mockStorageLocationDAO.create(externalGoogleCloudStorageLocationSetting)).thenReturn(999L);

		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalGoogleCloudStorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalGoogleCloudStorageLocationSetting);
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting_UnsharedBucket() {
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);
		when(synapseGoogleCloudStorageClient.bucketExists(any())).thenThrow(new StorageException(403,
				"someaccount@gserviceaccount.com does not have storage.buckets.get access to somebucket"));
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo,
					externalGoogleCloudStorageLocationSetting);
		});

		assertTrue(e.getMessage().contains("Synapse does not have access to the Google Cloud bucket " + bucketName));
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting_NonExistentBucket() {
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);
		when(synapseGoogleCloudStorageClient.bucketExists("notabucket")).thenReturn(false);
		externalGoogleCloudStorageLocationSetting.setBucket("notabucket");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo,
					externalGoogleCloudStorageLocationSetting);
		});
	}

	@Test
	public void validateProjectSetting() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		projectSettingsManagerImpl.validateProjectSetting(setting, null);

		verify(mockStorageLocationDAO).get(1L);
		verify(mockStorageLocationDAO).get(2L);
	}

	@Test
	public void validateProjectSettingNoProjectId() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(null);
		setting.setSettingsType(ProjectSettingsType.upload);

		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(setting, null));
	}

	@Test
	public void validateProjectSettingNoSettingsType() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(null);

		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(setting, null));
	}

	@Test
	public void validateProjectSettingEmptyLocations() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.<Long> newArrayList());

		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(setting, null));
	}

	@Test
	public void validateProjectSettingNotFoundLocation() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));

		when(mockStorageLocationDAO.get(1L)).thenThrow(new NotFoundException("dummy"));

		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(setting, null));
	}

	@Test
	public void testValidExternalS3() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));


		UserInfo currentUser = new UserInfo(false, 11L);
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setCreatedBy(11L);
		when(mockStorageLocationDAO.get(1L)).thenReturn(externalS3StorageLocationSetting);

		projectSettingsManagerImpl.validateProjectSetting(setting, currentUser);

		verify(mockStorageLocationDAO).get(1L);
	}

	@Test
	public void testExternalS3WrongOwner() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));

		UserInfo currentUser = new UserInfo(false, 11L);
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setCreatedBy(12L);
		when(mockStorageLocationDAO.get(1L)).thenReturn(externalS3StorageLocationSetting);

		when(mockPrincipalAliasDao.getUserName(12L)).thenReturn("some-other-user");

		assertThrows(UnauthorizedException.class, () -> projectSettingsManagerImpl.validateProjectSetting(setting, currentUser));
	}

	@Test
	public void testValidateProjectSettingLocationLimitExceeded() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayListWithCapacity(ProjectSettingsManagerImpl.MAX_LOCATIONS_PER_PROJECT + 1));

		UserInfo currentUser = new UserInfo(false, 11L);
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(setting, currentUser));
	}

	@Test
	public void inspectUsername() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(USER_NAME);
		List<String> expectedAliases = Lists.newArrayList(USER_NAME, "some-email@a.com");

		// Call under test
		projectSettingsManagerImpl.inspectUsername(mockBufferedReader, expectedAliases, bucketName, ProjectSettingsManager.OWNER_MARKER);
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameWithEmailAddress() throws IOException {
		String email = "some-email@a.com";
		when(mockBufferedReader.readLine()).thenReturn(email);
		List<String> expectedAliases = Lists.newArrayList(USER_NAME, email);

		// Call under test
		projectSettingsManagerImpl.inspectUsername(mockBufferedReader, expectedAliases, bucketName, ProjectSettingsManager.OWNER_MARKER);
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameIOException() throws IOException {
		when(mockBufferedReader.readLine()).thenThrow(new IOException());
		List<String> expectedAliases = Lists.newArrayList(USER_NAME, "some-email@a.com");

		// Call under test
		assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, expectedAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameNullUsername() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(null);
		List<String> expectedAliases = Lists.newArrayList(USER_NAME, "some-email@a.com");

		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, expectedAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		assertTrue(e.getMessage().contains("No username found"));
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameBlankUsername() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn("");
		List<String> expectedAliases = Lists.newArrayList(USER_NAME, "some-email@a.com");

		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, expectedAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		assertTrue(e.getMessage().contains("No username found"));
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameUnexpected() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(USER_NAME + "-incorrect");
		List<String> expectedAliases = Lists.newArrayList(USER_NAME, "some-email@a.com");
		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, expectedAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		assertTrue(e.getMessage().contains("The username " + USER_NAME + "-incorrect found under"));
		verify(mockBufferedReader).close();
	}

	private class TestProjectSettingStub implements ProjectSetting {
		@Override
		public String getConcreteType() {
			return null;
		}

		@Override
		public void setConcreteType(String concreteType) {

		}

		@Override
		public String getId() {
			return null;
		}

		@Override
		public void setId(String id) {

		}

		@Override
		public String getProjectId() {
			return null;
		}

		@Override
		public void setProjectId(String projectId) {

		}

		@Override
		public ProjectSettingsType getSettingsType() {
			return null;
		}

		@Override
		public void setSettingsType(ProjectSettingsType settingsType) {

		}

		@Override
		public String getEtag() {
			return null;
		}

		@Override
		public void setEtag(String etag) {

		}

		@Override
		public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
			return null;
		}

		@Override
		public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
			return null;
		}
	};
}
