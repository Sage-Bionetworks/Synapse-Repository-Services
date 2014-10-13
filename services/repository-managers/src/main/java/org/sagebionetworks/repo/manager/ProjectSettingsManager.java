package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;


public interface ProjectSettingsManager {

	ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	ProjectSetting getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, String type) throws DatastoreException, NotFoundException;

	ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void deleteProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	<T extends ProjectSetting> T getProjectSettingForParent(UserInfo userInfo, String parentId, String type, Class<T> expectedType)
			throws DatastoreException, UnauthorizedException, NotFoundException;
}
