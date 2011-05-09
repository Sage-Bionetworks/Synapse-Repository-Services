package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Simple implementation of the QueryManager.
 * 
 * @author jmhill
 *
 */
public class QueryManagerImpl implements QueryManager {
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	NodeQueryDao nodeQueryDao;
	


	@Override
	public <T extends Base> QueryResults executeQuery(String userId, BasicQuery query, Class<? extends T> clazz) throws DatastoreException {
		if(query == null) throw new IllegalArgumentException("Query cannot be null");
		if(query.getFrom() == null) throw new IllegalArgumentException("Query.getFrom() cannot be null");
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(query);
		List<String> ids = nodeResults.getResultIds();
		// Convert the list of ids to entities.
		List<Map<String, Object>> allRows = new ArrayList<Map<String, Object>>();
		for(String id: ids){
			EntityWithAnnotations<T> entityWithAnnos;
			try {
				entityWithAnnos = entityManager.getEntityWithAnnotations(userId, id, clazz);
				Map<String, Object> row = EntityToMapUtil.createMapFromEntity(entityWithAnnos);
				// Add this row
				allRows.add(row);
			} catch (NotFoundException e) {
				// This should never occur
				throw new DatastoreException("Node query returned node id: "+id+" but we failed to load this node: "+e.getMessage(), e);
			} catch (UnauthorizedException e) {
				// This should never occur
				throw new DatastoreException("Node query returned node id: "+id+" but the user was not authorized to see this node: "+e.getMessage(), e);
			}
		}
		// done
		return new QueryResults(allRows, nodeResults.getTotalNumberOfResults());
	}
	

}
