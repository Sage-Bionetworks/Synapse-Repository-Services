package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ReflectionStaticTestUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.collect.Lists;

public class ProjectSettingsImplTest {

	@Mock
	private ProjectSettingsDAO mockProjectSettingsDao;

	@Mock
	private StorageLocationDAO mockStorageLocationDAO;

	@Mock
	private AuthorizationManager mockAuthorizationManager;

	@Mock
	private NodeDAO mockNodeDAO;

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private AmazonS3 mockS3client;

	@Mock
	private UserProfileManager mockUserProfileManager;

	@Mock
	private UserManager mockUserManager;

	private ProjectSettingsManagerImpl projectSettingsManager = new ProjectSettingsManagerImpl();

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		ReflectionStaticTestUtils.mockAutowire(this, projectSettingsManager);
	}

	@Test
	public void testValid() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		projectSettingsManager.validateProjectSetting(setting, null);

		verify(mockStorageLocationDAO).get(1L);
		verify(mockStorageLocationDAO).get(2L);
	}

	@Test
	public void testValidWithEmptyDestination() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));
		setting.setDestinations(Lists.<UploadDestinationSetting> newArrayList());

		projectSettingsManager.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoProjectId() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(null);
		setting.setSettingsType(ProjectSettingsType.upload);

		projectSettingsManager.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoSettingsType() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(null);

		projectSettingsManager.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasNonEmptyDestination() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setDestinations(Lists.<UploadDestinationSetting> newArrayList(new ExternalUploadDestinationSetting()));

		projectSettingsManager.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyLocations() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.<Long> newArrayList());

		projectSettingsManager.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotFoundLocation() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		when(mockStorageLocationDAO.get(2L)).thenThrow(new NotFoundException("dummy"));

		projectSettingsManager.validateProjectSetting(setting, null);
	}

	@Test
	public void testValidExternalS3() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));


		UserInfo currentUser = new UserInfo(false, 11L);
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setCreatedBy(11L);
		when(mockStorageLocationDAO.get(1L)).thenReturn(externalS3StorageLocationSetting);

		projectSettingsManager.validateProjectSetting(setting, currentUser);

		verify(mockStorageLocationDAO).get(1L);
	}

	@Test(expected = UnauthorizedException.class)
	public void testExternalS3WrongOwner() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));

		UserInfo currentUser = new UserInfo(false, 11L);
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setCreatedBy(12L);
		when(mockStorageLocationDAO.get(1L)).thenReturn(externalS3StorageLocationSetting);

		UserProfile profile = new UserProfile();
		when(mockUserProfileManager.getUserProfile("12")).thenReturn(profile);

		projectSettingsManager.validateProjectSetting(setting, currentUser);
	}
}
