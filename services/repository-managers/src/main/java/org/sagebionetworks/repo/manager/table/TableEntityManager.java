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
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

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
	public RowReferenceSet appendRows(UserInfo user, String tableId, List<ColumnModel> columns, RowSet delta, ProgressCallback<Long> progressCallback)
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
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId, List<ColumnModel> columns,
			PartialRowSet rowsToAppendOrUpdateOrDelete, ProgressCallback<Long> progressCallback) throws DatastoreException, NotFoundException, IOException;

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
	 * @param etag
	 *            The last etag read before apply an update. An etag must be
	 *            provide if any rows are being updated.
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
	String appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns, Iterator<Row> rowStream, String etag,
			RowReferenceSet results, ProgressCallback<Long> progressCallback) throws DatastoreException, NotFoundException, IOException;

	/**
	 * List the changes that have been applied to a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);

	/**
	 * Get a specific RowSet.
	 * 
	 * @param tableId
	 * @param rowVersion
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public RowSet getRowSet(String tableId, Long rowVersion, List<ColumnModel> columns)
			throws IOException, NotFoundException;

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
	 * Get the highest possible row id in this table
	 * 
	 * @param tableId
	 * @return the highest possible row id
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public long getMaxRowId(String tableId) throws IOException, NotFoundException;;

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
	public String getCellValue(UserInfo userInfo, String tableId, RowReference rowRef, ColumnModel model) throws IOException,
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
	public RowSet getCellValues(UserInfo userInfo, String tableId, RowReferenceSet rowRefs, List<ColumnModel> columns)
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
	 * Delete the table.
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
	public void validateUpdateRequest(ProgressCallback<Void> callback,
			UserInfo userInfo, TableUpdateRequest change,
			TableIndexManager indexManager);

	/**
	 * Update the table with the given request.
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	public TableUpdateResponse updateTable(ProgressCallback<Void> callback,
			UserInfo userInfo, TableUpdateRequest change);

}
