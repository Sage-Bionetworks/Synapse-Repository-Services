package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TemporaryCode;

public interface StorageLocationDAO {

	public Long create(StorageLocationSetting setting);
	
	public void delete(Long id);

	public StorageLocationSetting get(Long id) throws DatastoreException, NotFoundException;

	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> locations) throws DatastoreException, NotFoundException;

	/**
	 * @deprecated This should not be used anymore, will return only the last 100 locations
	 */
	@Deprecated
	public List<StorageLocationSetting> getByOwner(Long id) throws DatastoreException, NotFoundException;
	
	/**
	 * Returns all the storage location ids that have at least one duplicate among the user that created it, the id returned is the latest created among the duplicates.
	 * 
	 * @return
	 * @throws DatastoreException
	 */
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	public List<Long> findAllWithDuplicates() throws DatastoreException;
	
	/**
	 * Return all the duplicates of the storage location with the given id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	public Set<Long> findDuplicates(Long id) throws DatastoreException, NotFoundException;
	
	/**
	 * Deletes the storage locations with the given ids
	 * @param ids
	 * @throws DatastoreException
	 */
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	public void deleteBatch(Set<Long> ids) throws DatastoreException;
}
