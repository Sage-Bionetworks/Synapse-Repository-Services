package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;


public interface ProjectSettingsManager {

	public static final String OWNER_MARKER = "owner.txt";

	ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	ProjectSetting getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, ProjectSettingsType type)
			throws DatastoreException, NotFoundException;

	ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void deleteProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	<T extends ProjectSetting> T getProjectSettingForNode(UserInfo userInfo, String parentId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException;

	<T extends ProjectSetting> List<T> getNodeSettingsByType(ProjectSettingsType settingsType, Class<T> expectedType);

	<T extends StorageLocationSetting> T createStorageLocationSetting(UserInfo userInfo, T StorageLocationSetting) throws DatastoreException,
			NotFoundException, IOException;

	List<StorageLocationSetting> getMyStorageLocationSettings(UserInfo userInfo) throws DatastoreException, NotFoundException;

	StorageLocationSetting getMyStorageLocationSetting(UserInfo userInfo, Long storageLocationId) throws DatastoreException,
			NotFoundException;

	StorageLocationSetting getStorageLocationSetting(Long storageLocationId) throws DatastoreException, NotFoundException;

	List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, List<Long> locations) throws DatastoreException,
			NotFoundException;
}
