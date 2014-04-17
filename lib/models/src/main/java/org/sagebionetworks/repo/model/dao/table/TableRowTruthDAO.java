package org.sagebionetworks.repo.model.dao.table;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;

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
	 * Get the version number for a given table ID and etag.
	 * 
	 * @param tableIdString
	 * @param etag
	 * @return
	 */
	public long getVersionForEtag(String tableIdString, String etag);
	
	/**
	 * Append a RowSet to a table.
	 * @param tableId
	 * @param models
	 * @param delta
	 * @return
	 * @throws IOException 
	 */
	public RowReferenceSet appendRowSetToTable(String userId, String tableId, List<ColumnModel> models, RowSet delta, boolean isDelete)
			throws IOException;
		
	/**
	 * Fetch a change set for a given table and 
	 * @param key
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	public RowSet getRowSet(String tableId, long rowVersion) throws IOException, NotFoundException;
	
	/**
	 * Use this method to scan over an entire RowSet without loading the set into memory.  For each row found in the 
	 * set, the passed handler will be called with the value of the row.
	 * @param tableId
	 * @param rowVersion
	 * @param handler
	 * @throws IOException
	 * @throws NotFoundException 
	 */
	public TableRowChange scanRowSet(String tableId, long rowVersion, RowHandler handler) throws IOException, NotFoundException;
	
	/**
	 * Get a RowSet for all rows referenced in the requested form.
	 * 
	 * @param ref
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	public RowSet getRowSet(RowReferenceSet ref, List<ColumnModel> result) throws IOException, NotFoundException;
	
	/**
	 * Get all the rows referenced in their unmodified form.
	 * There will be one RowSet for each distinct row version requested.
	 * Note: The headers can vary from one version to another.
	 * @param ref
	 * @return
	 * @throws IOException
	 * @throws NotFoundException 
	 */
	public List<RowSet> getRowSetOriginals(RowReferenceSet ref) throws IOException, NotFoundException;

	/**
	 * Get a rows referenced in its unmodified form.
	 * 
	 * @param tableId
	 * @param ref
	 * @param columns
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public Row getRowOriginal(String tableId, RowReference ref, List<ColumnModel> columns) throws IOException, NotFoundException;
	
	/**
	 * Get all the latest versions of the rows specified by the rowIds
	 * 
	 * @param tableId
	 * @param rowIds
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public RowSetAccessor getLatestVersions(String tableId, Set<Long> rowIds) throws IOException, NotFoundException;

	/**
	 * List the keys of all change sets applied to a table.
	 * 
	 * This can be used to synch the "truth" store with secondary stores. This is the full history of the table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);
	
	/**
	 * List all changes for a table with a version number greater than the given value (exclusive).
	 * @param tableId
	 * @param version
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTableGreaterThanVersion(String tableId, long version);
	
	/**
	 * Get the TableRowChange for a given tableId and row version number.
	 * @param tableId
	 * @param rowVersion
	 * @return
	 * @throws NotFoundException 
	 */
	public TableRowChange getTableRowChange(String tableId, long rowVersion) throws NotFoundException;
	
	/**
	 * This should never be called in a production setting.
	 * 
	 */
	public void truncateAllRowData();
}
