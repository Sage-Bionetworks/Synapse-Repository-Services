package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ProjectSettingsService {

	ProjectSetting getProjectSetting(Long userId, String id) throws DatastoreException, NotFoundException;

	ProjectSetting getProjectSettingByProjectAndType(Long userId, String projectId, ProjectSettingsType type) throws DatastoreException,
			NotFoundException;

	ProjectSetting createProjectSetting(Long userId, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void updateProjectSetting(Long userId, ProjectSetting projectSetting) throws DatastoreException, NotFoundException;

	void deleteProjectSetting(Long userId, String id) throws DatastoreException, NotFoundException;

	StorageLocationSetting createStorageLocationSetting(Long userId, StorageLocationSetting storageLocationSetting)
			throws DatastoreException, NotFoundException, IOException;

	@Deprecated
	List<StorageLocationSetting> getMyStorageLocations(Long userId) throws DatastoreException, NotFoundException;

	StorageLocationSetting getMyStorageLocation(Long userId, Long storageLocationId) throws DatastoreException, NotFoundException;
}
