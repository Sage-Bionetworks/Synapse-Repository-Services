package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class ProjectSettingsImplTest {

	private StorageLocationDAO mockDao = mock(StorageLocationDAO.class);
	private UserProfileManager mockUserProfileManager = mock(UserProfileManager.class);

	private ProjectSettingsManagerImpl projectSettingsManager = new ProjectSettingsManagerImpl();

	@Before
	public void setup() {
		ReflectionTestUtils.setField(projectSettingsManager, "storageLocationDAO", mockDao);
		ReflectionTestUtils.setField(projectSettingsManager, "userProfileManager", mockUserProfileManager);
	}

	@Test
	public void testValid() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		projectSettingsManager.validateProjectSetting(setting, null);

		verify(mockDao).get(1L);
		verify(mockDao).get(2L);
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

		when(mockDao.get(2L)).thenThrow(new NotFoundException("dummy"));

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
		when(mockDao.get(1L)).thenReturn(externalS3StorageLocationSetting);

		projectSettingsManager.validateProjectSetting(setting, currentUser);

		verify(mockDao).get(1L);
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
		when(mockDao.get(1L)).thenReturn(externalS3StorageLocationSetting);

		UserProfile profile = new UserProfile();
		when(mockUserProfileManager.getUserProfile(currentUser, "12")).thenReturn(profile);

		projectSettingsManager.validateProjectSetting(setting, currentUser);

		verify(mockDao).get(1L);
		verify(mockUserProfileManager).getUserProfile(currentUser, "12");
	}
}
