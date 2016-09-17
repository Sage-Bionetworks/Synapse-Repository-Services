package org.sagebionetworks.repo.model.dao.table;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableChangeType;
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
	 * Get the highest row ID in this table
	 * 
	 * @return
	 */
	public long getMaxRowId(String tableId);

	/**
	 * Get the highest current version for this table
	 * 
	 * @return
	 */
	public TableRowChange getLastTableRowChange(String tableId);
	
	/**
	 * Get the latest TableRowChange of a given type.
	 * 
	 * @param tableId
	 * @param changeType
	 * @return
	 */
	public TableRowChange getLastTableRowChange(String tableId, TableChangeType changeType);

	/**
	 * Append a RowSet to a table.
	 * 
	 * @param tableId
	 * @param models
	 * @param delta
	 * @return
	 * @throws IOException
	 */
	public RowReferenceSet appendRowSetToTable(String userId, String tableId, List<ColumnModel> columns, RawRowSet delta)
			throws IOException;
	
	/**
	 * Append a schema change to the table's changes.
	 * 
	 * @param userId
	 * @param tableId
	 * @param current
	 * @param changes
	 * @throws IOException 
	 */
	public long appendSchemaChangeToTable(String userId, String tableId, List<String> current, List<ColumnChange> changes) throws IOException;
	
	/**
	 * Get the schema change for a given version.
	 * @param tableId
	 * @param versionNumber
	 * @return
	 * @throws IOException 
	 */
	public List<ColumnChange> getSchemaChangeForVersion(String tableId, long versionNumber) throws IOException;
		
	/**
	 * Fetch a change set for a given table and
	 * 
	 * @param rowsToGet
	 * @param key
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public RowSet getRowSet(String tableId, long rowVersion, List<ColumnModel> columns) throws IOException,
			NotFoundException;
	
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
	public RowSet getRowSet(RowReferenceSet ref, List<ColumnModel> columns) throws IOException, NotFoundException;
	
	/**
	 * Get all the rows referenced in their unmodified form.
	 * There will be one RowSet for each distinct row version requested.
	 * Note: The headers can vary from one version to another.
	 * @param ref
	 * @return
	 * @throws IOException
	 * @throws NotFoundException 
	 */
	public List<RawRowSet> getRowSetOriginals(RowReferenceSet ref, List<ColumnModel> columns) throws IOException, NotFoundException;

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
	 * @param rowIdsInOut the set of row ids to find
	 * @param minVersion only check with versions equal or greater than the minVersion
	 * @param columnMapper
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public RowSetAccessor getLatestVersionsWithRowData(String tableId, Set<Long> rowIds, long minVersion, List<ColumnModel> columns)
			throws IOException, NotFoundException;

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
	 * 
	 * @param tableId
	 * @param version
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTableGreaterThanVersion(String tableId, long version);

	/**
	 * Get the TableRowChange for a given tableId and row version number.
	 * 
	 * @param tableId
	 * @param rowVersion
	 * @return
	 * @throws NotFoundException
	 */
	public TableRowChange getTableRowChange(String tableId, long rowVersion) throws NotFoundException;

	/**
	 * This should only be called after the table entity has been deleted
	 * 
	 * @param tableId
	 */
	public void deleteAllRowDataForTable(String tableId);
	
	/**
	 * This should never be called in a production setting.
	 * 
	 */
	public void truncateAllRowData();
	
	/**
	 * Check for a row level conflicts in the passed change sets, by scanning
	 * each row of each change set and looking for the intersection with the
	 * passed row Ids.
	 * 
	 * @param tableId
	 * @param delta
	 * @throws IOException 
	 * @throws ConflictingUpdateException
	 *             when a conflict is found
	 */
	public void checkForRowLevelConflict(String tableId, RawRowSet delta) throws IOException;

	/**
	 * Scan over a given changeset
	 * @param handler
	 * @param dto
	 * @throws IOException
	 */
	public void scanChange(RowHandler handler, TableRowChange dto) throws IOException;
	
	
}
