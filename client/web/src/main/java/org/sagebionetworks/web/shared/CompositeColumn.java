package org.sagebionetworks.web.shared;

import java.util.List;

/**
 * Defines a column that depends on other columns
 * 
 * @author jmhill
 *
 */
public interface CompositeColumn {
	
	/**
	 * What are the base column ids that this column depends on.
	 * @return
	 */
	public List<String> getBaseDependencyIds();

}
