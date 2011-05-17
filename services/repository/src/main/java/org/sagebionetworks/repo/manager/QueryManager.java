package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Executes a paginated query with the results as list of maps. Only objects
 * that user is allowed to see will be returned.
 * 
 * @author jmhill
 * 
 */
public interface QueryManager {

	/**
	 * Executes a paginated query with the results as list of maps. Only objects
	 * that user is allowed to see will be returned.
	 * 
	 * @param userId
	 * @param query
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	public <T extends Base> QueryResults executeQuery(UserInfo userInfo, BasicQuery query, Class<? extends T> clazz) throws DatastoreException;

}
