package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

public interface StorageLocationDAO {

	public Long create(StorageLocationSetting setting);

	public StorageLocationSetting get(Long id) throws DatastoreException, NotFoundException;

	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> locations) throws DatastoreException, NotFoundException;

	public List<StorageLocationSetting> getAllStorageLocationSettings();

	public List<StorageLocationSetting> getByOwner(Long id) throws DatastoreException, NotFoundException;
}
