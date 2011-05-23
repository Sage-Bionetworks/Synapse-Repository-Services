package org.sagebionetworks.repo.web;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs a query and returns the results as a list of entities.
 * 
 * @author jmhill
 *
 */
public class EntitiesAccessorImpl implements EntitiesAccessor {
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	NodeQueryDao nodeQueryDao;
	
	@Override
	public <T extends Base> PaginatedResults<T> getInRange(UserInfo userInfo, int offset, int limit, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {
		// Create a paginated query
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.getNodeTypeForClass(clazz));
		query.setLimit(limit);
		query.setOffset(offset-1);
		return executeQuery(userInfo, clazz, query);
	}

	@Override
	public <T extends Base> PaginatedResults<T> getInRangeSortedBy(UserInfo userInfo, int offset, int limit,String sortBy, Boolean ascending, Class<? extends T> clazz)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// Create a paginated query
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.getNodeTypeForClass(clazz));
		query.setLimit(limit);
		query.setOffset(offset-1);
		query.setAscending(ascending);
		query.setSort(sortBy);
		return executeQuery(userInfo, clazz, query);
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
	public <T extends Base> PaginatedResults<T> executeQuery(UserInfo userInfo, Class<? extends T> clazz, BasicQuery query) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		NodeQueryResults queryResults = nodeQueryDao.executeQuery(query);
		List<String> ids = queryResults.getResultIds();
		// Convert the list of ids to entities.
		List<T> list = new ArrayList<T>();
		for(String id: ids){
			T entity = entityManager.getEntity(userInfo, id, clazz);
			list.add(entity);
		}
		PaginatedResults<T> result = new PaginatedResults<T>();
		result.setResults(list);
		result.setTotalNumberOfResults(queryResults.getTotalNumberOfResults());
		return result;
	}

	@Override
	public void overrideAuthDaoForTest(AuthorizationManager mockAuth) {
		entityManager.overrideAuthDaoForTest(mockAuth);
	}

	@Override
	public <T extends BaseChild> List<T> getChildrenOfType(UserInfo userInfo, String parentId, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {
		// Convert this into a special query
		BasicQuery query = new BasicQuery();
		// We want all children
		query.setLimit(Long.MAX_VALUE);
		query.setOffset(0);
		query.setFrom(ObjectType.getNodeTypeForClass(clazz));
		query.addExpression(new Expression(new CompoundId(null, "parentId"), Compartor.EQUALS, Long.parseLong(parentId)));
		PaginatedResults<T> results = executeQuery(userInfo, clazz, query);
		return results.getResults();
	}

}
