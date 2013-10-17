package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.RowSet;

/**
 * Abstraction for storing Table Row Data.
 * 
 * @author John
 *
 */
public interface TableRowStorageDAO {
	
	/**
	 * Store a row set.
	 * @param schema
	 * @param rows
	 * @return The key the stored object.
	 */
	public String storeRowSet(RowSet rows);
	
	/**
	 * Get a RowSet given its key.
	 * @param key
	 * @return
	 */
	public RowSet getRowSet(String key);

}
