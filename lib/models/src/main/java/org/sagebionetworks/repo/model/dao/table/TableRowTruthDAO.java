package org.sagebionetworks.repo.model.dao.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;

/**
 * This is the "truth" store for all rows of TableEntites.
 * 
 * @author John
 *
 */
public interface TableRowTruthDAO {
	
	/**
	 * Store a change set of rows for a table.
	 * 
	 * @param tableId
	 * @param schema
	 * @param rows
	 * @return The key of this change set.
	 */
	public String storeRowSet(String tableId, List<ColumnModel> schema, RowSet rows);
	
	/**
	 * Fetch a row set using its key.
	 * @param key
	 * @return
	 */
	public RowSet getRowSet(String key);
	
	/**
	 * List the keys of all change sets applied to a table.
	 * 
	 * This can be used to synch the "truth" store with secondary stores.  This is the full history of the table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<String> listRowSetsKeysForTable(String tableId);
	
	/**
	 * This should never be called in a production setting.
	 * 
	 */
	public void truncateAllRowData();

}
