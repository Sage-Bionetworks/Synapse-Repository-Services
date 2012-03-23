package org.sagebionetworks.repo.model.query.jdo;

import java.util.List;

/**
 * Cache for the alias of each node type.
 * @author John
 *
 */
public interface NodeAliasCache {
	
	public List<Short> getAllNodeTypesForAlias(String alias);

}
