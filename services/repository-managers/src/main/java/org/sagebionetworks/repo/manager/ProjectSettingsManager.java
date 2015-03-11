package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;


public interface ProjectSettingsManager {

	ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	ProjectSetting getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, ProjectSettingsType type)
			throws DatastoreException, NotFoundException;

	ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void deleteProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	<T extends ProjectSetting> T getProjectSettingForParent(UserInfo userInfo, String parentId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException;

	<T extends UploadDestinationLocationSetting> T createUploadDestinationLocationSetting(UserInfo userInfo,
			T uploadDestinationLocationSetting) throws DatastoreException, NotFoundException, IOException;

	<T extends UploadDestinationLocationSetting> T updateUploadDestinationLocationSetting(UserInfo userInfo,
			T uploadDestinationLocationSetting) throws DatastoreException, NotFoundException;

	List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, List<Long> locations);
}
