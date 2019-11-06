package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
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
	public ProjectSetting getProjectSettingByProjectAndType(Long userId, String projectId, ProjectSettingsType type)
			throws DatastoreException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		Optional<ProjectSetting> setting = projectSettingsManager.getProjectSettingByProjectAndType(userInfo, projectId, type);
		return setting.orElse(null);
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

	@Override
	public StorageLocationSetting createStorageLocationSetting(Long userId, StorageLocationSetting storageLocationSetting)
			throws DatastoreException, NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return projectSettingsManager.createStorageLocationSetting(userInfo, storageLocationSetting);
	}

	@Override
	public List<StorageLocationSetting> getMyStorageLocations(Long userId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return projectSettingsManager.getMyStorageLocationSettings(userInfo);
	}

	@Override
	public StorageLocationSetting getMyStorageLocation(Long userId, Long storageLocationId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return projectSettingsManager.getMyStorageLocationSetting(userInfo, storageLocationId);
	}
}
