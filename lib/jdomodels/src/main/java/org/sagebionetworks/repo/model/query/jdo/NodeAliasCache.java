package org.sagebionetworks.repo.model.query.jdo;

import java.util.List;

/**
 * Cache for the alias of each node type.
 * @author John
 *
 */
public interface NodeAliasCache {
	
	public List<Short> getAllNodeTypesForAlias(String alias);

	/*
	 * Some node types have more than one alias.  This method returns the preferred alias.
	 */
	public String getPreferredAlias(String alias);
	
}
