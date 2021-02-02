package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class ProjectSettingsManagerImplUnitTest {
	private UserInfo userInfo;

	private static final long PARENT_STORAGE_LOCATION_ID = 3;
	private static final String PROJECT_ID = "3523";
	private static final String PROJECT_SETTINGS_ID = "21521";

	private static final String NODE_ID = "3524";
	private static final long STORAGE_LOCATION_ID = 4;

	private static final Long USER_ID = 101L;
	private static final String BUCKET_NAME = "bucket.name";

	@Mock
	private NodeManager mockNodeManager;
	
	@Mock
	private NodeDAO mockNodeDao;

	@Mock
	private AuthorizationManager authorizationManager;

	@Mock
	private StorageLocationDAO mockStorageLocationDAO;

	@Mock
	private ProjectSettingsDAO mockProjectSettingDao;

	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;
	
	@Mock
	private StorageLocationSetting mockStorageLocationSetting;
	
	@Mock
	private StorageLocationProcessor<? extends StorageLocationSetting> mockStorageLocationProcessor;

	@InjectMocks
	@Spy
	private ProjectSettingsManagerImpl projectSettingsManagerImpl;
	
	private UploadDestinationListSetting uploadDestinationListSetting;
	private ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	private ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting;
	private S3StorageLocationSetting synapseStorageLocationSetting;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, USER_ID);

		uploadDestinationListSetting = new UploadDestinationListSetting();
		uploadDestinationListSetting.setProjectId(PROJECT_ID);
		uploadDestinationListSetting.setSettingsType(ProjectSettingsType.upload);
		uploadDestinationListSetting.setId(PROJECT_SETTINGS_ID);
		uploadDestinationListSetting.setEtag("etag");
		uploadDestinationListSetting.setLocations(ImmutableList.of(STORAGE_LOCATION_ID));

		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(BUCKET_NAME);

		externalGoogleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		externalGoogleCloudStorageLocationSetting.setBucket(BUCKET_NAME);

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
		when(mockProjectSettingDao.getInheritedProjectSetting(NODE_ID, ProjectSettingsType.upload)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		// Call under test
		Optional<UploadDestinationListSetting> actual = projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertTrue(actual.isPresent());
		assertSame(uploadDestinationListSetting, actual.get());
	}

	@Test
	public void getProjectSettingForNode_Null() {
		when(mockProjectSettingDao.getInheritedProjectSetting(NODE_ID, ProjectSettingsType.upload)).thenReturn(null);

		// Call under test
		Optional<UploadDestinationListSetting> actual = projectSettingsManagerImpl.getProjectSettingForNode(userInfo, NODE_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertFalse(actual.isPresent());
	}

	@Test
	public void getProjectSettingForNode_WrongType() {
		// Use Mockito to create an instance of ProjectSetting that's not an UploadDestinationListSetting.
		ProjectSetting mockSetting = mock(ProjectSetting.class);
		when(mockProjectSettingDao.getInheritedProjectSetting(NODE_ID, ProjectSettingsType.upload)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(mockSetting);

		// Call under test.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.getProjectSettingForNode(
				userInfo, NODE_ID, ProjectSettingsType.upload, UploadDestinationListSetting.class));
		assertEquals("Settings type for 'upload' is not of type org.sagebionetworks.repo.model.project.UploadDestinationListSetting",
				ex.getMessage());
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
		when(mockProjectSettingDao.create(uploadDestinationListSetting)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Spy getProjectSettingForNode(). This is tested somewhere else, and we want to decouple this test from the
		// getProjectSettingForNode() tests.
		doReturn(Optional.empty()).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
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
		doReturn(Optional.of(parentProjectSetting)).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
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
		when(mockProjectSettingDao.create(uploadDestinationListSetting)).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		synapseStorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		UploadDestinationListSetting parentProjectSetting = new UploadDestinationListSetting();
		parentProjectSetting.setLocations(ImmutableList.of(PARENT_STORAGE_LOCATION_ID));
		doReturn(Optional.of(parentProjectSetting)).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
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

		doReturn(Optional.empty()).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);

		uploadDestinationListSetting.setLocations(ImmutableList.of(STORAGE_LOCATION_ID, PARENT_STORAGE_LOCATION_ID));

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> projectSettingsManagerImpl.createProjectSetting(userInfo,
				uploadDestinationListSetting), "An STS-enabled folder cannot add other upload destinations");
		verify(mockProjectSettingDao, never()).create(any());
	}
	
	@Test
	public void testCreateProjectSettingAutofillType() {
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.project);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
				AuthorizationStatus.authorized());
		
		// STS stuff
		doReturn(Optional.empty()).when(projectSettingsManagerImpl).getProjectSettingForNode(userInfo, PROJECT_ID,
				ProjectSettingsType.upload, ProjectSetting.class);
		
		when(mockProjectSettingDao.create(any())).thenReturn(PROJECT_SETTINGS_ID);
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);

		// Nullify the type
		uploadDestinationListSetting.setSettingsType(null);
		
		// Call under test
		ProjectSetting result = projectSettingsManagerImpl.createProjectSetting(userInfo, uploadDestinationListSetting);
		
		assertSame(uploadDestinationListSetting, result);
		assertEquals(ProjectSettingsType.upload, result.getSettingsType());
		verify(mockNodeManager).getNodeType(userInfo, PROJECT_ID);
		verify(mockProjectSettingDao).create(uploadDestinationListSetting);		
				
	}

	@Test
	public void updateProjectSetting_HappyCase() {
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());
		when(mockNodeManager.getNodeType(userInfo, PROJECT_ID)).thenReturn(EntityType.folder);

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
	public void testUpdateProjectSettingWithNoId() {

		uploadDestinationListSetting.setId(null);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.updateProjectSetting(userInfo, uploadDestinationListSetting);
		});
		
		assertEquals("The id is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateProjectSettingWithNoProjectId() {

		uploadDestinationListSetting.setProjectId(null);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Method under test.
			projectSettingsManagerImpl.updateProjectSetting(userInfo, uploadDestinationListSetting);
		});
		
		assertEquals("The project id is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateProjectSettingAutofillType() {
		
		uploadDestinationListSetting.setSettingsType(null);
		
		// Mock dependencies.
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
				AuthorizationStatus.authorized());

		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(synapseStorageLocationSetting);

		// Method under test.
		projectSettingsManagerImpl.updateProjectSetting(userInfo, uploadDestinationListSetting);
		
		assertEquals(ProjectSettingsType.upload, uploadDestinationListSetting.getSettingsType());
	}

	@Test
	public void deleteProjectSetting_HappyCase() {
		// Mock dependencies.
		when(mockProjectSettingDao.get(PROJECT_SETTINGS_ID)).thenReturn(uploadDestinationListSetting);
		when(authorizationManager.canAccess(userInfo, PROJECT_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
				AuthorizationStatus.authorized());

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
	public void testCreateStorageLocation() {
		
		when(mockStorageLocationProcessor.supports(any())).thenReturn(true);
		
		projectSettingsManagerImpl.setStorageLocationProcessors(Collections.singletonList(mockStorageLocationProcessor));
		
		when(mockStorageLocationDAO.create(any())).thenReturn(STORAGE_LOCATION_ID);
		
		// Call under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, mockStorageLocationSetting);
		
		verify(mockStorageLocationProcessor).beforeCreate(eq(userInfo), any());
		// Make sure that the upload type is set to NONE when null
		verify(mockStorageLocationSetting).setUploadType(UploadType.NONE);
		verify(mockStorageLocationSetting).setCreatedBy(USER_ID);
		verify(mockStorageLocationSetting).setCreatedOn(any());
		verify(mockStorageLocationDAO).create(mockStorageLocationSetting);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}
	
	@Test
	public void testCreateStorageLocationWithUploadType() {
		doNothing().when(projectSettingsManagerImpl).processStorageLocation(any(), any());
		when(mockStorageLocationSetting.getUploadType()).thenReturn(UploadType.S3);
		// Call under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, mockStorageLocationSetting);
		
		// Make sure that the upload type is set to NONE when null
		verify(mockStorageLocationSetting, times(0)).setUploadType(any());
		verify(mockStorageLocationSetting).setCreatedBy(USER_ID);
		verify(mockStorageLocationSetting).setCreatedOn(any());
	}
	
	@Test
	public void testCreateStorageLocationWithNullUser() {
		UserInfo userInfo = null;
		StorageLocationSetting storageLocation = mockStorageLocationSetting;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, storageLocation);
		});
		
		assertEquals("The user is required.", ex.getMessage());
	}
	
	@Test
	public void testCreateStorageLocationWithNullLocation() {
		StorageLocationSetting storageLocation = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, storageLocation);
		});
		
		assertEquals("The storage location is required.", ex.getMessage());
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
	public void testIsStsStorageLocation_NullStorageLocationSetting() {
		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting((StorageLocationSetting) null);
		assertFalse(result);
	}

	@Test
	public void testIsStsStorageLocation_NotStsStorageLocation() {
		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(
				externalGoogleCloudStorageLocationSetting);
		assertFalse(result);
	}

	@Test
	public void testIsStsStorageLocation_StsEnabledFalse() {
		externalS3StorageLocationSetting.setStsEnabled(false);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(externalS3StorageLocationSetting);
		assertFalse(result);
	}

	@Test
	public void testIsStsStorageLocation_StsEnabledNull() {
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(null);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(externalS3StorageLocationSetting);
		assertFalse(result);
	}

	@Test
	public void testIsStsStorageLocation_StsEnabledTrue() {
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(true);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(externalS3StorageLocationSetting);
		assertTrue(result);
	}

	@Test
	public void testIsStsStorageLocation_NullProjectSetting() {
		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting((ProjectSetting) null);
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
	public void testIsStsStorageLocation_NormalCase() {
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(true);
		when(mockStorageLocationDAO.get(STORAGE_LOCATION_ID)).thenReturn(externalS3StorageLocationSetting);

		// Method under test.
		boolean result = projectSettingsManagerImpl.isStsStorageLocationSetting(uploadDestinationListSetting);
		assertTrue(result);
		verify(mockStorageLocationDAO).get(STORAGE_LOCATION_ID);
	}

}
