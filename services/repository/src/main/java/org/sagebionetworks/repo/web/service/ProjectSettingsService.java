package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ProjectSettingsService {

	ProjectSetting getProjectSetting(Long userId, String id) throws DatastoreException, NotFoundException;

	ProjectSetting getProjectSettingByProjectAndType(Long userId, String projectId, String type) throws DatastoreException, NotFoundException;

	ProjectSetting createProjectSetting(Long userId, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void updateProjectSetting(Long userId, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void deleteProjectSetting(Long userId, String id) throws DatastoreException, NotFoundException;
}
