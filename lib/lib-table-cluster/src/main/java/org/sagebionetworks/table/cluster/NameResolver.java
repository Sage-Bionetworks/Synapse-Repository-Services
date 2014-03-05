package org.sagebionetworks.table.cluster;

/**
 * Abstraction for resolving names to their corresponding IDs.
 * 
 * @author John
 *
 */
public interface NameResolver {
	
	/**
	 * Resolve a column name to a Column ID.
	 * @param name
	 * @return
	 */
	public Long resolveColumnNameToColumnId(String name);

}
