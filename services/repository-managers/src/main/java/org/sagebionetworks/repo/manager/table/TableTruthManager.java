package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * Abstraction for metadata about the truth of a table.
 * 
 */
public interface TableTruthManager {

	/**
	 * The MD5 hex of a table's schema.
	 * 
	 * @param tableId
	 * @return
	 */
	String getSchemaMD5Hex(String tableId);

	/**
	 * Get the version of the given table. This is can be different for each
	 * table type. The value is used to indicate the current state of a table's
	 * truth.
	 * 
	 * @param tableId
	 * @return
	 */
	long getTableVersion(String tableId);

	/**
	 * Is the given table available.
	 * 
	 * @param tableId
	 * @return
	 */
	boolean isTableAvailable(String tableId);

	/**
	 * Lookup the object type for this table.
	 * 
	 * @param tableId
	 * @return
	 */
	ObjectType getTableType(String tableId);

}
