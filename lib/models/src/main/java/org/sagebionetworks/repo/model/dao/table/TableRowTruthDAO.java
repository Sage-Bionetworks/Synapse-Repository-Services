package org.sagebionetworks.repo.model.dao.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableChange;

/**
 * This is the "truth" store for all rows of TableEntites.
 * 
 * @author John
 *
 */
public interface TableRowTruthDAO {
	
	/**
	 * Reserver a range of IDs for a given table and count.
	 * 
	 * @param tableId
	 * @param coutToReserver
	 * @return
	 */
	public IdRange reserveIdsInRange(String tableId, long coutToReserver);
	
	/**
	 * Append a RowSet to a table.
	 * @param tableId
	 * @param models
	 * @param delta
	 * @return
	 * @throws IOException 
	 */
	public RowReferenceSet appendRowSetToTable(String tableId, List<ColumnModel> models, RowSet delta) throws IOException;
	
	/**
	 * Store a change set of rows for a table.
	 * 
	 * @param tableId
	 * @param schema
	 * @param rows
	 * @return The key of this change set.
	 */
	public TableChange storeRowSet(TableChange change, RowSet rows);
	
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
	public List<TableChange> listRowSetsKeysForTable(String tableId);
	
	/**
	 * This should never be called in a production setting.
	 * 
	 */
	public void truncateAllRowData();

}
