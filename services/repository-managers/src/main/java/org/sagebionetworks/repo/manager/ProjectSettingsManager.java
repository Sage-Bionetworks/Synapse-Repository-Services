package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;


public interface ProjectSettingsManager {

	ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	Optional<ProjectSetting> getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, ProjectSettingsType type)
			throws DatastoreException, NotFoundException;

	ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void deleteProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	<T extends ProjectSetting> Optional<T> getProjectSettingForNode(UserInfo userInfo, String parentId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Helper method to check if a StorageLocationSetting is a an STS-enabled storage location. That is, the storage
	 * location is an StsStorageLocation with StsEnabled=true.
	 */
	boolean isStsStorageLocationSetting(StorageLocationSetting storageLocationSetting);

	/**
	 * Helper method to check if a ProjectSetting is a an STS-enabled storage location. That is, the storage location
	 * referenced in the project setting is an StsStorageLocation with StsEnabled=true.
	 */
	boolean isStsStorageLocationSetting(ProjectSetting projectSetting);

	<T extends StorageLocationSetting> T createStorageLocationSetting(UserInfo userInfo, T StorageLocationSetting) throws DatastoreException,
			NotFoundException, IOException;
	
	@Deprecated
	List<StorageLocationSetting> getMyStorageLocationSettings(UserInfo userInfo) throws DatastoreException, NotFoundException;

	StorageLocationSetting getMyStorageLocationSetting(UserInfo userInfo, Long storageLocationId) throws DatastoreException,
			NotFoundException;

	StorageLocationSetting getStorageLocationSetting(Long storageLocationId) throws DatastoreException, NotFoundException;

	List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, List<Long> storageLocationIds) throws DatastoreException,
			NotFoundException;
	
}
