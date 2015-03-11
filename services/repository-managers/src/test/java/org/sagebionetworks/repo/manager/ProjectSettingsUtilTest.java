package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.project.ExternalS3UploadDestinationLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;

public class ProjectSettingsUtilTest {

	private UploadDestinationLocationDAO mockDao = mock(UploadDestinationLocationDAO.class);
	private UserProfileManager mockUserProfileManager = mock(UserProfileManager.class);

	@Test
	public void testValid() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);

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

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoProjectId() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(null);
		setting.setSettingsType(ProjectSettingsType.upload);

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoSettingsType() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(null);

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasNonEmptyDestination() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setDestinations(Lists.<UploadDestinationSetting> newArrayList(new ExternalUploadDestinationSetting()));

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyLocations() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.<Long> newArrayList());

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotFoundLocation() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		when(mockDao.get(2L)).thenThrow(new NotFoundException("dummy"));

		ProjectSettingsUtil.validateProjectSetting(setting, null, null, mockDao);
	}

	@Test
	public void testValidExternalS3() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));


		UserInfo currentUser = new UserInfo(false, 11L);
		ExternalS3UploadDestinationLocationSetting externalS3UploadDestinationLocationSetting = new ExternalS3UploadDestinationLocationSetting();
		externalS3UploadDestinationLocationSetting.setCreatedBy(11L);
		when(mockDao.get(1L)).thenReturn(externalS3UploadDestinationLocationSetting);

		ProjectSettingsUtil.validateProjectSetting(setting, currentUser, mockUserProfileManager, mockDao);

		verify(mockDao).get(1L);
	}

	@Test(expected = UnauthorizedException.class)
	public void testExternalS3WrongOwner() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L));

		UserInfo currentUser = new UserInfo(false, 11L);
		ExternalS3UploadDestinationLocationSetting externalS3UploadDestinationLocationSetting = new ExternalS3UploadDestinationLocationSetting();
		externalS3UploadDestinationLocationSetting.setCreatedBy(12L);
		when(mockDao.get(1L)).thenReturn(externalS3UploadDestinationLocationSetting);

		UserProfile profile = new UserProfile();
		when(mockUserProfileManager.getUserProfile(currentUser, "12")).thenReturn(profile);

		ProjectSettingsUtil.validateProjectSetting(setting, currentUser, mockUserProfileManager, mockDao);

		verify(mockDao).get(1L);
		verify(mockUserProfileManager).getUserProfile(currentUser, "12");
	}
}
