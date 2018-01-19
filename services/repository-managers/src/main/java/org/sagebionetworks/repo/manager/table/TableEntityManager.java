package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.model.SparseChangeSet;

/**
 * Abstraction for Table Row management.
 * 
 * @author jmhill
 * 
 */
public interface TableEntityManager {

	/**
	 * Append a set of rows to a table.
	 * 
	 * @param user
	 * @param delta
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws IOException
	 */
	public RowReferenceSet appendRows(UserInfo user, String tableId, RowSet delta, ProgressCallback progressCallback)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Append or update a set of partial rows to a table.
	 * 
	 * @param user
	 * @param tableId
	 * @param models
	 * @param rowsToAppendOrUpdate
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId,
			PartialRowSet rowsToAppendOrUpdateOrDelete, ProgressCallback progressCallback) throws DatastoreException, NotFoundException, IOException;

	/**
	 * Delete a set of rows from a table.
	 * 
	 */
	public RowReferenceSet deleteRows(UserInfo user, String tableId, RowSelection rowsToDelete)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Delete all rows from a table.
	 * 
	 * @param models
	 */
	public void deleteAllRows(String id);

	/**
	 * Append all rows from the provided iterator into the a table. This method
	 * will batch rows into optimum sized RowSets.
	 * 
	 * Note: This method will only keep one batch of rows in memory at a time so
	 * it should be suitable for appending very large change sets to a table.
	 * 
	 * @param user The user appending the rows
	 * @param tableId The ID of the table entity to append the rows too.
	 * @param models The schema of the rows being appended.
	 * @param rowStream The stream of rows to append to the table.
	 * @param results
	 *            This parameter is optional. When provide, it will be populated
	 *            with a RowReference for each row appended to the table. This
	 *            parameter should be null for large change sets to minimize
	 *            memory usage.
	 * @param The callback will be called for each batch of rows appended to the table.  Can be null.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	TableUpdateResponse appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns, Iterator<SparseRowDto> rowStream, String etag,
			RowReferenceSet results, ProgressCallback progressCallback) throws DatastoreException, NotFoundException, IOException;

	/**
	 * List the changes that have been applied to a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);
	
	/**
	 * Get the a SparseChangeSet for a given TableRowChange.
	 * 
	 * @param change
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	public SparseChangeSet getSparseChangeSet(TableRowChange change) throws NotFoundException, IOException;
	
	/**
	 * Get the schema change for a given version.
	 * 
	 * @param tableId
	 * @param versionNumber
	 * @return
	 * @throws IOException
	 */
	public List<ColumnChangeDetails> getSchemaChangeForVersion(String tableId, long versionNumber) throws IOException;

	/**
	 * Get the last table row change
	 * 
	 * @param tableId
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public TableRowChange getLastTableRowChange(String tableId) throws IOException, NotFoundException;

	/**
	 * Get the values for a specific row reference and column
	 * 
	 * @param userInfo
	 * @param refSet
	 * @param model
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public Row getCellValue(UserInfo userInfo, String tableId, RowReference rowRef, ColumnModel model) throws IOException,
			NotFoundException;

	/**
	 * Get the values for a specific row reference set and columns
	 * 
	 * @param userInfo
	 * @param refSet
	 * @param model
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public RowSet getCellValues(UserInfo userInfo, String tableId, List<RowReference> rows, List<ColumnModel> columns)
			throws IOException, NotFoundException;
	
	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of
	 * FileHandleIds that are actually associated with the table.
	 * @param objectId
	 * @throws TemporarilyUnavailableException if this query cannot be run at this time.
	 */
	public Set<Long> getFileHandleIdsAssociatedWithTable(String tableId, Set<Long> toTest) throws TemporarilyUnavailableException;
	
	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of
	 * FileHandleIds that are actually associated with the table.
	 * @param objectId
	 */
	public Set<String> getFileHandleIdsAssociatedWithTable(String tableId, List<String> toTest) throws TemporarilyUnavailableException;

	/**
	 * Set the schema of the table.
	 * @param userInfo
	 * @param columnIds
	 * @param id
	 */
	public void setTableSchema(UserInfo userInfo, List<String> columnIds,
			String id);

	/**
	 * Mark a table as deleted.  This occurs when a table is moved to the trash.
	 * The actual data for the table will only be deleted if the table no longer exists.
	 * @param deletedId
	 */
	public void setTableAsDeleted(String deletedId);
	
	/**
	 * 
	 * @param deletedId
	 */
	public void deleteTable(String deletedId);

	/**
	 * Is a temporary table needed to validate the given table update request.
	 * @param change
	 * @return
	 */
	public boolean isTemporaryTableNeededToValidate(TableUpdateRequest change);

	/**
	 * Validate a single update request.
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @param indexManager The index manager is only provided if a temporary table was created 
	 * for the purpose of validation.
	 */
	public void validateUpdateRequest(ProgressCallback callback,
			UserInfo userInfo, TableUpdateRequest change,
			TableIndexManager indexManager);

	/**
	 * Update the table with the given request.
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	public TableUpdateResponse updateTable(ProgressCallback callback,
			UserInfo userInfo, TableUpdateRequest change);

	/**
	 * Get the schema for the table.
	 * @param user
	 * @param id
	 * @return
	 */
	public List<String> getTableSchema(String id);

	/**
	 * Delete all data about a table if it no longer exists.
	 * If the passed tableId is in the trash, then this method will not
	 * delete anything.
	 * 
	 * @param tableId
	 */
	public void deleteTableIfDoesNotExist(String tableId);


}
