package org.sagebionetworks.repo.model;

/**
 * 
 * Interface for DAOs of objects which can be migrated
 * 
 * @author brucehoff
 *
 */
public interface MigratableDAO {
	
	long getCount() throws DatastoreException;
	
	QueryResults<ObjectData> getMigrationObjectData(long offset, long limit, boolean includeDependencies) throws DatastoreException;
	
//	void serialize(String file);
//	
//	void deserialize(String file);

}
