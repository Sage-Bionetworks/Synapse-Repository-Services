package org.sagebionetworks.repo.manager.backup.migration;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.QueryResults;

/**
 * 
 * Determines dependencies of objects on other objects.  
 * This allows the client to ensure that dependencies are in place before migrating an object.
 * 
 * @author brucehoff
 *
 */
public interface DependencyManager {
	
	/**
	 * get all objects in the system and (optionally) their dependencies, paginated
	 * 
	 * @param offset, 0 based pagination param
	 * @param limit, pagination param
	 * @return
	 * @throws DatastoreException 
	 */
	QueryResults<MigratableObjectData> getAllObjects(long offset, long limit, boolean includeDependencies) throws DatastoreException;
	QueryResults<MigratableObjectCount> getAllObjectsCounts(long offset, long limit, boolean includeDependencies) throws DatastoreException;

}
