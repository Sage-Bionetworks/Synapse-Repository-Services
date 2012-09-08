package org.sagebionetworks.repo.model;

/**
 * Accesses database for storage locations.
 *
 * @author ewu
 */
public interface StorageLocationDAO {

	/**
	 * Replaces the storage locations for a given node.
	 */
	void replaceLocationData(StorageLocations locations) throws DatastoreException;

	// TODO : Long getUsage(String userId, String locationType) 
	//            throws NotFoundException, DatastoreException, InvalidModelException;
}
