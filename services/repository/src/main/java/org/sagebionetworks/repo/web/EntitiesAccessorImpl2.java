package org.sagebionetworks.repo.web;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs a query and returns the results as a list of entities.
 * 
 * @author jmhill
 *
 */
public class EntitiesAccessorImpl2 implements EntitiesAccessor2 {
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	NodeQueryDao nodeQueryDao;
	
	@Override
	public <T extends Base> List<T> getInRange(String userId, int offset, int limit, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {
		// Create a paginated query
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.getNodeTypeForClass(clazz));
		query.setLimit(limit);
		query.setOffset(offset);
		return executeQuery(userId, clazz, query);
	}

	@Override
	public <T extends Base> List<T> getInRangeSortedBy(String userId, int offset, int limit,	String sortBy, Boolean ascending, Class<? extends T> clazz)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// Create a paginated query
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.getNodeTypeForClass(clazz));
		query.setLimit(limit);
		query.setOffset(offset);
		query.setAscending(ascending);
		query.setSort(sortBy);
		return executeQuery(userId, clazz, query);
	}

	/**
	 * This runs the actual query.
	 * @param <T>
	 * @param userId
	 * @param clazz
	 * @param query
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public <T extends Base> List<T> executeQuery(String userId, Class<? extends T> clazz, BasicQuery query) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		NodeQueryResults queryResults = nodeQueryDao.executeQuery(query);
		List<String> ids = queryResults.getResultIds();
		// Convert the list of ids to entities.
		List<T> results = new ArrayList<T>();
		for(String id: ids){
			T entity = entityManager.getEntity(userId, id, clazz);
			results.add(entity);
		}
		return results;
	}

	@Override
	public void overrideAuthDaoForTest(AuthorizationDAO mockAuth) {
		entityManager.overrideAuthDaoForTest(mockAuth);
	}

}
