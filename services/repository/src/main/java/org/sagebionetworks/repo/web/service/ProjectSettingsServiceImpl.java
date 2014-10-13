package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectSettingsServiceImpl implements ProjectSettingsService {

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private UserManager userManager;

	@Override
	public ProjectSetting getProjectSetting(Long userId, String id) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return projectSettingsManager.getProjectSetting(userInfo, id);
	}

	@Override
	public ProjectSetting getProjectSettingByProjectAndType(Long userId, String projectId, String type) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return projectSettingsManager.getProjectSettingByProjectAndType(userInfo, projectId, type);
	}

	@Override
	public ProjectSetting createProjectSetting(Long userId, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return projectSettingsManager.createProjectSetting(userInfo, projectSetting);
	}

	@Override
	public void updateProjectSetting(Long userId, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		projectSettingsManager.updateProjectSetting(userInfo, projectSetting);
	}

	@Override
	public void deleteProjectSetting(Long userId, String id) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		projectSettingsManager.deleteProjectSetting(userInfo, id);
	}
}
