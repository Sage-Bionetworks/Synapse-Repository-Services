package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

public interface StorageLocationDAO {
	
	Long DEFAULT_STORAGE_LOCATION_ID = 1L;

	public Long create(StorageLocationSetting setting);
	
	public void delete(Long id);

	/**
	 * 
	 * @param id
	 * @return The storage location with the given id. If the id is null will return the default synapse storage location
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public StorageLocationSetting get(Long id) throws DatastoreException, NotFoundException;

	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> storageLocationIds) throws DatastoreException, NotFoundException;

	/**
	 * @deprecated This should not be used anymore, will return only the last 100 locations
	 */
	@Deprecated
	public List<StorageLocationSetting> getByOwner(Long id) throws DatastoreException, NotFoundException;
	
}
