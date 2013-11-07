package org.sagebionetworks.repo.model.dao.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.RowSet;

/**
 * Abstraction for secondary Tables DAO layers.  Secondary Table DAOs are used for random access
 * and queries of row data but are not the truth store for the row data.
 * from a table.
 * 
 * @author John
 *
 */
public interface SecondaryTableRowDAO {

	/**
	 * Store a RowSet for the given table and key.
	 * 
	 * @param tableId
	 * @param key
	 * @param rows
	 * @return
	 */
	public boolean storeRowSet(String tableId, String key, RowSet rows);
	
	/**
	 * List the keys of the RowSets stored in this DAO.  This is used to synch a secondary DAO with the truth store.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<String> listRowSets(String tableId);
}
