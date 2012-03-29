package org.sagebionetworks.repo.model.query.jdo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A simple cache for Entity alias types.
 * 
 * @author John
 * 
 */
public class NodeAliasCacheImpl implements NodeAliasCache {
	
	@Autowired
	private NodeDAO nodeDao;
	// da cache
	private Map<String, List<Short>> cache = Collections
			.synchronizedMap(new HashMap<String, List<Short>>());

	@Override
	public List<Short> getAllNodeTypesForAlias(String alias) {
		// Try to get it from the cache
		List<Short> results = cache.get(alias);
		if (results == null) {
			results = nodeDao.getAllNodeTypesForAlias(alias);
			cache.put(alias, results);
		}
		// return the results
		return results;
	}

	@Override
	public String getPreferredAlias(String alias) {
		// TODO it does not appear that Register.json shows which is the
		// preferred one, so we're just hard-coding it here. Be sure to upgrade
		// this at a later date if there is a better way to get the preferred
		// alias
		if(alias.equals(EntityType.dataset.name())) {
			return "study"; // this is not defined as a constant or an enum in any place that I could find
		}
		else if (alias.equals(EntityType.layer.name())) {
			return "data"; // this is not defined as a constant or an enum in any place that I could find
		}
		return alias;
	}

}
