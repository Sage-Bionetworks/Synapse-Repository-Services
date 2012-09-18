package org.sagebionetworks.repo.model;

/**
 * 
 * Interface for DAOs of objects which can be migrated
 * 
 * @author brucehoff
 *
 */
public interface MigratableDAO {
	
	/**
	 * 
	 * @return the number of items in the entire system of the type managed by the DAO
	 * @throws DatastoreException
	 */
	long getCount() throws DatastoreException;
	
	/**
	 * 
	 * @param offset  zero based offset
	 * @param limit page size
	 * @param includeDependencies says whether to include dependencies for each object or omit (for efficiency)
	 * @return paginated list of objects in the system (optionally with their dependencies)
	 * @throws DatastoreException
	 */
	QueryResults<MigratableObjectData> getMigrationObjectData(long offset, long limit, boolean includeDependencies) throws DatastoreException;

	QueryResults<MigratableObjectCount> getMigratableObjectCounts(long offset, long limit, boolean includeDependencies) throws DatastoreException;
	
}
