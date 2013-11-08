package org.sagebionetworks.repo.model.dao.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;

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
	public RowReferenceSet appendRowSetToTable(String userId, String tableId, List<ColumnModel> models, RowSet delta) throws IOException;
		
	/**
	 * Fetch a change set for a given table and 
	 * @param key
	 * @return
	 * @throws IOException 
	 */
	public RowSet getRowSet(String tableId, long rowVersion) throws IOException;
	
	/**
	 * Use this method to scan over an entire RowSet without loading the set into memory.  For each row found in the 
	 * set, the passed handler will be called with the value of the row.
	 * @param tableId
	 * @param rowVersion
	 * @param handler
	 * @throws IOException
	 */
	public void scanRowSet(String tableId, long rowVersion, RowHandler handler) throws IOException;
	
	/**
	 * Get a RowSet for all rows referenced.
	 * 
	 * @param ref
	 * @return
	 * @throws IOException 
	 */
	public RowSet getRowSet(RowReferenceSet ref) throws IOException;
	
	/**
	 * Get all the rows referenced in their unmodified form.
	 * There will be one RowSet for each distinct row version requested.
	 * Note: The headers can vary from one version to another.
	 * @param ref
	 * @return
	 * @throws IOException
	 */
	public List<RowSet> getRowSetOriginals(RowReferenceSet ref) throws IOException;
	
	/**
	 * List the keys of all change sets applied to a table.
	 * 
	 * This can be used to synch the "truth" store with secondary stores.  This is the full history of the table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);
	
	/**
	 * Get the TableRowChange for a given tableId and row version number.
	 * @param tableId
	 * @param rowVersion
	 * @return
	 */
	public TableRowChange getTableRowChange(String tableId, long rowVersion);
	
	/**
	 * This should never be called in a production setting.
	 * 
	 */
	public void truncateAllRowData();

}
