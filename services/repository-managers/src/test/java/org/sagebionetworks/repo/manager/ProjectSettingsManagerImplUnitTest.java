package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.s3.model.S3Object;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class ProjectSettingsManagerImplUnitTest {
	private UserInfo userInfo;

	@InjectMocks
	@Spy
	private ProjectSettingsManagerImpl projectSettingsManagerImpl;

	private static final long OLD_STORAGE_LOCATION_ID = 2;
	private static final long PARENT_STORAGE_LOCATION_ID = 3;
	private static final String PROJECT_ID = "3523";
	private static final String PROJECT_SETTINGS_ID = "21521";

	private static final String NODE_ID = "3524";
	private static final long STORAGE_LOCATION_ID = 4;

	private static final String USER_NAME = "user-name";
	private static final String USER_EMAIL = "testuser@my.info.net";
	private static final Long USER_ID = 101L;
	private static final String bucketName = "bucket.name";

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private AuthorizationManager authorizationManager;

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

	List<PrincipalAlias> principalAliases;
	private UploadDestinationListSetting uploadDestinationListSetting;
	private ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	private ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting;
	private ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting;
	private ExternalStorageLocationSetting externalStorageLocationSetting;
	private ProxyStorageLocationSettings proxyStorageLocationSettings;
	private S3StorageLocationSetting synapseStorageLocationSetting;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, USER_ID);

		PrincipalAlias username = new PrincipalAlias();
		username.setPrincipalId(USER_ID);
		username.setType(AliasType.USER_NAME);
		username.setAlias(USER_NAME);
		PrincipalAlias email1 = new PrincipalAlias();
		email1.setPrincipalId(USER_ID);
		email1.setType(AliasType.USER_EMAIL);
		email1.setAlias(USER_EMAIL);
		PrincipalAlias email2 = new PrincipalAlias();
		email2.setPrincipalId(USER_ID);
		email2.setType(AliasType.USER_EMAIL);
		email2.setAlias("institutional-email@institution.edu");
		principalAliases = Arrays.asList(username, email1, email2);

		uploadDestinationListSetting = new UploadDestinationListSetting();
		uploadDestinationListSetting.setProjectId(PROJECT_ID);
		uploadDestinationListSetting.setSettingsType(ProjectSettingsType.upload);
		uploadDestinationListSetting.setId(PROJECT_SETTINGS_ID);
		uploadDestinationListSetting.setEtag("etag");
		uploadDestinationListSetting.setLocations(ImmutableList.of(STORAGE_LOCATION_ID));

		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(bucketName);

		externalGoogleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		externalGoogleCloudStorageLocationSetting.setBucket(bucketName);

		externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBucket(bucketName);
		externalObjectStorageLocationSetting.setEndpointUrl("https://myendpoint.com");

		externalStorageLocationSetting = new ExternalStorageLocationSetting();
		externalStorageLocationSetting.setUrl("https://example.com");

		proxyStorageLocationSettings = new ProxyStorageLocationSettings();
		proxyStorageLocationSettings.setProxyUrl("https://example.com");
		proxyStorageLocationSettings.setSecretKey(RandomStringUtils.randomAlphabetic(36));

		synapseStorageLocationSetting = new S3StorageLocationSetting();
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
		when(mockProjectSettingDao.getInheritedProjectSetting(NODE_ID)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		// Call under test
		ProjectSetting actual = projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertSame(uploadDestinationListSetting, actual);
	}

	@Test
	public void getProjectSettingForNode_Null() {
		when(mockProjectSettingDao.getInheritedProjectSetting(NODE_ID)).thenReturn(null);

		// Call under test
		ProjectSetting actual = projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertNull(actual);
	}

	@Test
	public void getProjectSettingForNode_WrongType() {
		// Use Mockito to create an instance of ProjectSetting that's not an UploadDestinationListSetting.
		ProjectSetting mockSetting = mock(ProjectSetting.class);
		when(mockProjectSettingDao.getInheritedProjectSetting(NODE_ID)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(mockSetting);

		// Call under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.getProjectSettingForNode(
				userInfo, NODE_ID, ProjectSettingsType.upload, UploadDestinationListSetting.class),
				"Settings type for 'upload' is not of type UploadDestinationListSetting");
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
	public void createProjectSetting_StsHappyCase() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(true);
		when(mockProjectSettingDao.create(uploadDestinationListSetting)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Spy getProjectSettingForNode(). This is tested somewhere else, and we want to decouple this test from the
		// getProjectSettingForNode() tests.
		doReturn(null).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		// Method under test.
		ProjectSetting result = projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting);
		assertSame(uploadDestinationListSetting, result);
		verify(authorizationManager).canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE);
		verify(mockProjectSettingDao).create(uploadDestinationListSetting);
	}

	@Test
	public void createProjectSetting_NotProjectOrFolder() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.file);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting), "The id is not the id of a project or folder entity");
		verify(mockProjectSettingDao, never()).create(any());
	}

	@Test
	public void createProjectSetting_Unauthorized() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.accessDenied("dummy error message"));

		// Method under test.
		assertThrows(UnauthorizedException.class, () -> projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting), "Cannot create settings for this project");
		verify(mockProjectSettingDao, never()).create(any());
	}

	@Test
	public void createProjectSetting_CannotAddToParentWithSts() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());

		UploadDestinationListSetting parentProjectSetting = new UploadDestinationListSetting();
		parentProjectSetting.setLocations(ImmutableList.of(PARENT_STORAGE_LOCATION_ID));
		doReturn(parentProjectSetting).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		S3StorageLocationSetting parentStorageLocationSetting = new S3StorageLocationSetting();
		parentStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(PARENT_STORAGE_LOCATION_ID)).thenReturn(parentStorageLocationSetting);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting), "Can't override project settings in an STS-enabled folder path");
		verify(mockProjectSettingDao, never()).create(any());
	}

	@Test
	public void createProjectSetting_CanAddToParentWithNonStsStorageLocation() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(true);
		when(mockProjectSettingDao.create(uploadDestinationListSetting)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		UploadDestinationListSetting parentProjectSetting = new UploadDestinationListSetting();
		parentProjectSetting.setLocations(ImmutableList.of(PARENT_STORAGE_LOCATION_ID));
		doReturn(parentProjectSetting).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		S3StorageLocationSetting parentStorageLocationSetting = new S3StorageLocationSetting();
		parentStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(PARENT_STORAGE_LOCATION_ID)).thenReturn(parentStorageLocationSetting);

		// Method under test.
		ProjectSetting result = projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting);
		assertSame(uploadDestinationListSetting, result);
		verify(mockProjectSettingDao).create(uploadDestinationListSetting);
	}

	@Test
	public void createProjectSetting_Invalid() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		doReturn(null).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		uploadDestinationListSetting.setLocations(ImmutableList.of(STORAGE_LOCATION_ID, PARENT_STORAGE_LOCATION_ID));

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting), "An STS-enabled folder cannot add other upload destinations");
		verify(mockProjectSettingDao, never()).create(any());
	}

	@Test
	public void createProjectSetting_CannotAddStsToNonEmptyFolder() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		doReturn(null).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting), "Can't enable STS in a non-empty folder");
		verify(mockProjectSettingDao, never()).create(any());
	}

	@Test
	public void createProjectSetting_CanAddNonStsToNonEmptyFolder() {
		// Mock dependencies.
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);
		when(mockProjectSettingDao.create(uploadDestinationListSetting)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		synapseStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		doReturn(null).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		// Method under test.
		ProjectSetting result = projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting);
		assertSame(uploadDestinationListSetting, result);
		verify(authorizationManager).canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE);
		verify(mockProjectSettingDao).create(uploadDestinationListSetting);
	}

	@Test
	public void updateProjectSetting_HappyCase() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(true);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Method under test.
		projectSettingsManagerImpl.updateProjectSetting(userInfo, uploadDestinationListSetting);
		verify(mockProjectSettingDao).update(uploadDestinationListSetting);
	}

	@Test
	public void updateProjectSetting_Unauthorized() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.accessDenied("dummy error message"));

		// Method under test.
		assertThrows(UnauthorizedException.class, () -> projectSettingsManagerImpl.updateProjectSetting(userInfo,
				uploadDestinationListSetting), "Cannot update settings on this project");
		verify(mockProjectSettingDao, never()).update(any());
	}

	@Test
	public void updateProjectSetting_Invalid() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		uploadDestinationListSetting.setLocations(ImmutableList.of(STORAGE_LOCATION_ID, PARENT_STORAGE_LOCATION_ID));

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.updateProjectSetting(userInfo,
				uploadDestinationListSetting), "An STS-enabled folder cannot add other upload destinations");
		verify(mockProjectSettingDao, never()).update(any());
	}

	@Test
	public void updateProjectSetting_CannotAddStsToNonEmptyFolder() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.updateProjectSetting(userInfo,
				uploadDestinationListSetting), "Can't enable STS in a non-empty folder");
		verify(mockProjectSettingDao, never()).update(any());
	}

	@Test
	public void updateProjectSetting_CanAddNonStsToNonEmptyFolder() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Method under test.
		projectSettingsManagerImpl.updateProjectSetting(userInfo, uploadDestinationListSetting);
		verify(mockProjectSettingDao).update(uploadDestinationListSetting);
	}

	@Test
	public void updateProjectSetting_CannotRemoveStsFromNonEmptyFolder() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		UploadDestinationListSetting oldProjectSetting = new UploadDestinationListSetting();
		oldProjectSetting.setLocations(ImmutableList.of(OLD_STORAGE_LOCATION_ID));
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(oldProjectSetting);

		S3StorageLocationSetting oldStorageLocationSetting = new S3StorageLocationSetting();
		oldStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(OLD_STORAGE_LOCATION_ID)).thenReturn(oldStorageLocationSetting);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.updateProjectSetting(userInfo,
				uploadDestinationListSetting), "Can't disable STS in a non-empty folder");
		verify(mockProjectSettingDao, never()).update(any());
	}

	@Test
	public void updateProjectSetting_CanUpdateNonStsInNonEmptyFolder() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		UploadDestinationListSetting oldProjectSetting = new UploadDestinationListSetting();
		oldProjectSetting.setLocations(ImmutableList.of(OLD_STORAGE_LOCATION_ID));
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(oldProjectSetting);

		S3StorageLocationSetting oldStorageLocationSetting = new S3StorageLocationSetting();
		oldStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(OLD_STORAGE_LOCATION_ID)).thenReturn(oldStorageLocationSetting);

		// Method under test.
		projectSettingsManagerImpl.updateProjectSetting(userInfo, uploadDestinationListSetting);
		verify(mockProjectSettingDao).update(uploadDestinationListSetting);
	}

	@Test
	public void deleteProjectSetting_HappyCase() {
		// Mock dependencies.
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(true);

		// Method under test.
		projectSettingsManagerImpl.deleteProjectSetting(userInfo, PROJECT_SETTINGS_ID);
		verify(authorizationManager).canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE);
		verify(mockProjectSettingDao).delete(PROJECT_SETTINGS_ID);
	}

	@Test
	public void deleteProjectSetting_Unauthorized() {
		// Mock dependencies.
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
				AuthorizationStatus.accessDenied("dummy error message"));

		// Method under test.
		assertThrows(UnauthorizedException.class, () -> projectSettingsManagerImpl.deleteProjectSetting(userInfo,
				PROJECT_SETTINGS_ID), "Cannot delete settings from this project");
		verify(mockProjectSettingDao, never()).delete(any());
	}

	@Test
	public void deleteProjectSetting_CannotDeleteStsFromNonEmptyProject() {
		// Mock dependencies.
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.deleteProjectSetting(userInfo,
				PROJECT_SETTINGS_ID), "Can't disable STS in a non-empty folder");
		verify(mockProjectSettingDao, never()).delete(any());
	}

	@Test
	public void deleteProjectSetting_CanDeleteNonStsFromNonEmptyProject() {
		// Mock dependencies.
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.isEntityEmpty(PROJECT_ID)).thenReturn(false);

		synapseStorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Method under test.
		projectSettingsManagerImpl.deleteProjectSetting(userInfo, PROJECT_SETTINGS_ID);
		verify(mockProjectSettingDao).delete(PROJECT_SETTINGS_ID);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_HappyCase() throws Exception {
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);

		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream(USER_NAME.getBytes()));
		when(synapseS3Client.getObject(bucketName, "owner.txt")).thenReturn(s3Object);

		// Set UploadType to null to verify that we set the default UploadType.
		externalS3StorageLocationSetting.setUploadType(null);

		when(mockStorageLocationDAO.create(externalS3StorageLocationSetting)).thenReturn(999L);

		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalS3StorageLocationSetting);
		assertEquals(UploadType.S3, externalS3StorageLocationSetting.getUploadType());
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
	public void testCreateExternalS3StorageLocationSetting_InvalidS3BaseKey() {
		externalS3StorageLocationSetting.setBaseKey("CantHaveATrailingSlash/");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalStorageLocationSetting_HappyCase() throws IOException {
		when(mockStorageLocationDAO.create(externalStorageLocationSetting)).thenReturn(999L);

		// Set UploadType to null to verify that we set the default UploadType.
		externalStorageLocationSetting.setUploadType(null);

		// Method under test.
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalStorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalStorageLocationSetting);
		assertEquals(UploadType.NONE, externalStorageLocationSetting.getUploadType());
	}

	@Test
	public void testCreateExternalStorageLocationSetting_NullUrl() {
		externalStorageLocationSetting.setUrl(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalStorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalStorageLocationSetting_InvalidUrl() {
		externalStorageLocationSetting.setUrl("invalid url");

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalStorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalObjectStorageLocationSetting_HappyCase() throws IOException {
		when(mockStorageLocationDAO.create(externalObjectStorageLocationSetting)).thenReturn(999L);

		// Set UploadType to null to verify that we set the default UploadType.
		externalObjectStorageLocationSetting.setUploadType(null);

		// Method under test.
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalObjectStorageLocationSetting);
		assertEquals(UploadType.NONE, externalObjectStorageLocationSetting.getUploadType());
	}

	@Test
	public void testCreateExternalObjectStorageLocationSetting_InvalidEndpointUrl() {
		externalObjectStorageLocationSetting.setEndpointUrl("invalid url");

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalObjectStorageLocationSetting_InvalidS3BucketName() {
		externalObjectStorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting() throws Exception {
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		when(synapseGoogleCloudStorageClient.bucketExists(bucketName)).thenReturn(true);
		when(synapseGoogleCloudStorageClient.getObject(bucketName, "owner.txt")).thenReturn(mock(Blob.class));
		when(synapseGoogleCloudStorageClient.getObjectContent(bucketName, "owner.txt")).thenReturn(IOUtils.toInputStream(USER_NAME, StandardCharsets.UTF_8));
		when(mockStorageLocationDAO.create(externalGoogleCloudStorageLocationSetting)).thenReturn(999L);

		// Set UploadType to null to verify that we set the default UploadType.
		externalGoogleCloudStorageLocationSetting.setUploadType(null);

		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalGoogleCloudStorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalGoogleCloudStorageLocationSetting);
		assertEquals(UploadType.GOOGLECLOUDSTORAGE, externalGoogleCloudStorageLocationSetting.getUploadType());
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting_UnsharedBucket() {
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
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
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		when(synapseGoogleCloudStorageClient.bucketExists("notabucket")).thenReturn(false);
		externalGoogleCloudStorageLocationSetting.setBucket("notabucket");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo,
					externalGoogleCloudStorageLocationSetting);
		});
	}

	@Test
	public void testCreateProxyLocationStorageSettings_HappyCase() throws IOException {
		when(mockStorageLocationDAO.create(proxyStorageLocationSettings)).thenReturn(999L);

		// Set UploadType to null to verify that we set the default UploadType.
		proxyStorageLocationSettings.setUploadType(null);

		// Method under test.
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, proxyStorageLocationSettings);

		verify(mockStorageLocationDAO).create(proxyStorageLocationSettings);
		assertEquals(UploadType.NONE, proxyStorageLocationSettings.getUploadType());
	}

	@Test
	public void testCreateProxyLocationStorageSettings_NullProxyUrl() {
		proxyStorageLocationSettings.setProxyUrl(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, proxyStorageLocationSettings);
		});
	}

	@Test
	public void testCreateProxyLocationStorageSettings_InvalidProxyUrl() {
		proxyStorageLocationSettings.setProxyUrl("invalid url");

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, proxyStorageLocationSettings);
		});
	}

	@Test
	public void testCreateProxyLocationStorageSettings_ProxyUrlNotHttps() {
		proxyStorageLocationSettings.setProxyUrl("ftp://example.com");

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, proxyStorageLocationSettings);
		});
	}

	@Test
	public void testCreateProxyLocationStorageSettings_NullSecretKey() {
		proxyStorageLocationSettings.setSecretKey(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, proxyStorageLocationSettings);
		});
	}

	@Test
	public void testCreateProxyLocationStorageSettings_SecretKeyTooShort() {
		proxyStorageLocationSettings.setSecretKey("ab");

		assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, proxyStorageLocationSettings);
		});
	}

	@Test
	public void testCreateSynapseStorageLocationSettings_HappyCase() throws IOException {
		// Set UploadType to null to verify that we set the default UploadType.
		synapseStorageLocationSetting.setUploadType(null);
		synapseStorageLocationSetting.setStsEnabled(false);

		when(mockStorageLocationDAO.create(synapseStorageLocationSetting)).thenReturn(999L);

		// Method under test.
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, synapseStorageLocationSetting);

		verify(mockStorageLocationDAO).create(synapseStorageLocationSetting);
		assertEquals(UploadType.S3, synapseStorageLocationSetting.getUploadType());
		assertNull(synapseStorageLocationSetting.getBaseKey());
	}

	@Test
	public void testCreateSynapseStorageLocationSettings_StsEnabledNull() throws IOException {
		synapseStorageLocationSetting.setStsEnabled(null);

		when(mockStorageLocationDAO.create(synapseStorageLocationSetting)).thenReturn(999L);

		// Method under test.
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, synapseStorageLocationSetting);

		verify(mockStorageLocationDAO).create(synapseStorageLocationSetting);
		assertNull(synapseStorageLocationSetting.getBaseKey());
	}

	@Test
	public void testCreateSynapseStorageLocationSettings_StsEnabledTrue() throws IOException {
		synapseStorageLocationSetting.setStsEnabled(true);

		when(mockStorageLocationDAO.create(synapseStorageLocationSetting)).thenReturn(999L);

		// Method under test.
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, synapseStorageLocationSetting);

		verify(mockStorageLocationDAO).create(synapseStorageLocationSetting);
		assertTrue(synapseStorageLocationSetting.getBaseKey().startsWith(USER_ID + "/"));
	}

	@Test
	public void testCreateSynapseStorageLocationSettings_WithBaseKey() {
		synapseStorageLocationSetting.setBaseKey("dummy base key");

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.createStorageLocationSetting(
				userInfo, synapseStorageLocationSetting), "Cannot specify baseKey when creating an S3StorageLocationSetting");
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
	public void testValidateProjectSetting_StsHappyCase() {
		// Mock dependencies.
		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);

		// Execute - no exception means success.
		projectSettingsManagerImpl.validateProjectSetting(uploadDestinationListSetting, userInfo);
	}

	@Test
	public void testValidateProjectSetting_StsCannotBeEnabledOnProjects() {
		// Mock dependencies.
		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.project);

		// Execute - expected exception.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(
				uploadDestinationListSetting, userInfo), "Can only enable STS on a folder");
	}

	@Test
	public void testValidateProjectSetting_StsCannotBeEnabledWithOtherStorageLocations() {
		// Mock dependencies.
		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);

		// Execute - expected exception.
		uploadDestinationListSetting.setLocations(ImmutableList.of(STORAGE_LOCATION_ID, 10L));
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.validateProjectSetting(
				uploadDestinationListSetting, userInfo), "An STS-enabled folder cannot add other upload destinations");
	}

	@Test
	public void testIsStsStorageLocation_NullProjectSetting() {
		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(null);
		assertFalse(result);
		verifyZeroInteractions(mockStorageLocationDAO);
	}

	@Test
	public void testIsStsStorageLocation_ProjectSettingWrongSubclass() {
		// Create a mock of ProjectSetting, so we can have a ProjectSetting instance that's not an
		// UploadDestinationListSetting.
		ProjectSetting input = mock(ProjectSetting.class);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(input);
		assertFalse(result);
		verifyZeroInteractions(mockStorageLocationDAO);
	}

	@Test
	public void testIsStsStorageLocation_StorageLocationNotFound() {
		// Mock dependencies.
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenThrow(NotFoundException.class);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(uploadDestinationListSetting);
		assertFalse(result);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}

	@Test
	public void testIsStsStorageLocation_NotStsStorageLocation() {
		// Mock dependencies.
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(externalGoogleCloudStorageLocationSetting);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(uploadDestinationListSetting);
		assertFalse(result);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}

	@Test
	public void testIsStsStorageLocation_StsEnabledFalse() {
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(false);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(externalS3StorageLocationSetting);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(uploadDestinationListSetting);
		assertFalse(result);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}

	@Test
	public void testIsStsStorageLocation_StsEnabledNull() {
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(null);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(externalS3StorageLocationSetting);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(uploadDestinationListSetting);
		assertFalse(result);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}

	@Test
	public void testIsStsStorageLocation_StsEnabledTrue() {
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(externalS3StorageLocationSetting);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(uploadDestinationListSetting);
		assertTrue(result);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}

	@Test
	public void inspectUsername() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(USER_NAME);

		// Call under test
		projectSettingsManagerImpl.inspectUsername(mockBufferedReader, principalAliases, bucketName, ProjectSettingsManager.OWNER_MARKER);
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameWithEmailAddress() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(USER_EMAIL);

		// Call under test
		projectSettingsManagerImpl.inspectUsername(mockBufferedReader, principalAliases, bucketName, ProjectSettingsManager.OWNER_MARKER);
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameIOException() throws IOException {
		when(mockBufferedReader.readLine()).thenThrow(new IOException());

		// Call under test
		assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, principalAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameNullUsername() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(null);

		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, principalAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		assertTrue(e.getMessage().contains("No username found"));
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameBlankUsername() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn("");

		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, principalAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		assertTrue(e.getMessage().contains("No username found"));
		verify(mockBufferedReader).close();
	}

	@Test
	public void inspectUsernameUnexpected() throws IOException {
		when(mockBufferedReader.readLine()).thenReturn(USER_NAME + "-incorrect");
		// Call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				projectSettingsManagerImpl.inspectUsername(mockBufferedReader, principalAliases, bucketName, ProjectSettingsManager.OWNER_MARKER));
		assertTrue(e.getMessage().contains("The username " + USER_NAME + "-incorrect found under"));
		verify(mockBufferedReader).close();
	}
}
