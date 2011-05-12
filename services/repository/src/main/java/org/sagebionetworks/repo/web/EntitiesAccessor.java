package org.sagebionetworks.repo.web;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.BasicQuery;

/**
 * Strategy pattern interface for querying, sorting, filtering, and sorting
 * entities of a particular type
 * 
 * @author deflaux
 * @param <T>
 *            the particular type of entity we are querying
 * 
 */
public interface EntitiesAccessor {

	/**
	 * @param offset
	 * @param limit
	 * @return the list of zero or more entities found
	 * @throws DatastoreException
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	public <T extends Base> PaginatedResults<T> getInRange(String userId, int offset, int limit, Class<? extends T>  clazz) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * @param offset
	 * @param limit
	 * @param sortBy
	 * @param ascending
	 * @return the list of zero or more entities found
	 * @throws DatastoreException
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 */
	public <T extends Base> PaginatedResults<T> getInRangeSortedBy(String userI, int offset, int limit, String sortBy,
			Boolean ascending, Class<? extends T>  clazz) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Execute a query for the user.
	 * @param <T>
	 * @param userId
	 * @param clazz
	 * @param query
	 * @return The list of entities that match the query.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public <T extends Base> PaginatedResults<T> executeQuery(String userId, Class<? extends T> clazz, BasicQuery query) throws DatastoreException, NotFoundException,	UnauthorizedException;


	/**
	 * Get all of the children of a given type
	 * @param <T>
	 * @param userId
	 * @param parentId
	 * @param clazz
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public <T extends BaseChild> List<T> getChildrenOfType(String userId, String parentId, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException;
	/**
	 * Used to override this dao for a test.
	 * @param mockAuth
	 */
	public void overrideAuthDaoForTest(AuthorizationManager mockAuth);

}
