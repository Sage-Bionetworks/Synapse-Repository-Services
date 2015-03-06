package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3UploadDestinationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;

public class ProjectSettingsUtilTest {

	UploadDestinationLocationDAO mockDao = mock(UploadDestinationLocationDAO.class);

	@Test
	public void testValid() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L, 2L));

		ProjectSettingsUtil.validateProjectSetting(setting, mockDao);

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

		ProjectSettingsUtil.validateProjectSetting(setting, mockDao);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoProjectId() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(null);
		setting.setSettingsType(ProjectSettingsType.upload);

		ProjectSettingsUtil.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoSettingsType() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(null);

		ProjectSettingsUtil.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasNonEmptyDestination() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setDestinations(Lists.<UploadDestinationSetting> newArrayList(new ExternalUploadDestinationSetting()));

		ProjectSettingsUtil.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyLocations() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.<Long> newArrayList());

		ProjectSettingsUtil.validateProjectSetting(setting, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotFoundLocation() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("projectId");
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(1L,2L));

		when(mockDao.get(2L)).thenThrow(new NotFoundException("dummy"));
		
		ProjectSettingsUtil.validateProjectSetting(setting, mockDao);
	}
}
