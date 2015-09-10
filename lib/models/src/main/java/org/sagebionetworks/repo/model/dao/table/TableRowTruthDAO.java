package org.sagebionetworks.repo.model.dao.table;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModelMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ProgressCallback;

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
	 * Append a RowSet to a table.
	 * 
	 * @param tableId
	 * @param models
	 * @param delta
	 * @return
	 * @throws IOException
	 */
	public RowReferenceSet appendRowSetToTable(String userId, String tableId, ColumnModelMapper models, RawRowSet delta)
			throws IOException;
		
	/**
	 * Fetch a change set for a given table and
	 * 
	 * @param rowsToGet
	 * @param key
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public RowSet getRowSet(String tableId, long rowVersion, Set<Long> rowsToGet, ColumnModelMapper schema) throws IOException,
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
	public RowSet getRowSet(RowReferenceSet ref, ColumnModelMapper columnMapper) throws IOException, NotFoundException;
	
	/**
	 * Get all the rows referenced in their unmodified form.
	 * There will be one RowSet for each distinct row version requested.
	 * Note: The headers can vary from one version to another.
	 * @param ref
	 * @return
	 * @throws IOException
	 * @throws NotFoundException 
	 */
	public List<RawRowSet> getRowSetOriginals(RowReferenceSet ref, ColumnModelMapper columnMapper) throws IOException, NotFoundException;

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
	public Row getRowOriginal(String tableId, RowReference ref, ColumnModelMapper columnMapper) throws IOException, NotFoundException;
	
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
	public RowSetAccessor getLatestVersionsWithRowData(String tableId, Set<Long> rowIds, long minVersion, ColumnMapper columnMapper)
			throws IOException, NotFoundException;

	/**
	 * Get all the latest versions of the rows specified by the rowIds
	 * 
	 * @param tableId
	 * @param rowIdsInOut the set of row ids to find
	 * @param minVersion only check with versions equal or greater than the minVersion
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public Map<Long, Long> getLatestVersions(String tableId, Set<Long> rowIds, long minVersion) throws IOException, NotFoundException;

	/**
	 * Get all the latest versions for this table
	 * 
	 * @param tableId
	 * @param minVersion only return rows that have a version equal or greater than this
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws TableUnavilableException
	 */
	public Map<Long, Long> getLatestVersions(String tableId, long minVersion, long rowIdOffset, long limit) throws IOException,
			NotFoundException, TableUnavilableException;

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
	 * Count all changes for a table with a version number greater than the given value (exclusive).
	 * 
	 * @param tableId
	 * @param version
	 * @return
	 */
	public int countRowSetsForTableGreaterThanVersion(String tableId, long version);

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
	 * Update the lastest version cache if supported
	 * 
	 * @throws IOException
	 */
	public void updateLatestVersionCache(String tableId, ProgressCallback<Long> progressCallback) throws IOException;

	/**
	 * Remove the latest version cache and row cache for the table
	 * 
	 * @param tableId
	 */
	public void removeCaches(Long tableId) throws IOException;
	
	/**
	 * Check for a row level conflicts in the passed change sets, by scanning
	 * each row of each change set and looking for the intersection with the
	 * passed row Ids.
	 * 
	 * @param tableId
	 * @param delta
	 * @param coutToReserver
	 * @throws IOException 
	 * @throws ConflictingUpdateException
	 *             when a conflict is found
	 */
	public void checkForRowLevelConflict(String tableId, RawRowSet delta, long minVersion) throws IOException;

	/**
	 * Scan over a given changeset
	 * @param handler
	 * @param dto
	 * @throws IOException
	 */
	public void scanChange(RowHandler handler, TableRowChange dto) throws IOException;
	
	
}
