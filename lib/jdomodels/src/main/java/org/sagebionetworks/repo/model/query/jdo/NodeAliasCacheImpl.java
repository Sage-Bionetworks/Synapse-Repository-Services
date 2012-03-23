package org.sagebionetworks.repo.model.query.jdo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A simple cache for Entity alias types.
 * @author John
 *
 */
public class NodeAliasCacheImpl implements NodeAliasCache {
	
	@Autowired
	private NodeDAO nodeDao;
	// da cache
	private Map<String, List<Short>> cache = Collections.synchronizedMap(new HashMap<String, List<Short>>()); 

	@Override
	public List<Short> getAllNodeTypesForAlias(String alias) {
		// Try to get it from the cache
		List<Short> results = cache.get(alias);
		if(results == null){
			results = nodeDao.getAllNodeTypesForAlias(alias);
			cache.put(alias, results);
		}
		// return the results
		return results;
	}

}
