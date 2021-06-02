package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
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
	RowReferenceSet appendRows(UserInfo user, String tableId, RowSet delta, ProgressCallback progressCallback,
			long transactionId) throws DatastoreException, NotFoundException, IOException;

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
	RowReferenceSet appendPartialRows(UserInfo user, String tableId, PartialRowSet rowsToAppendOrUpdateOrDelete,
			ProgressCallback progressCallback, long transactionId)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Delete a set of rows from a table.
	 * 
	 */
	RowReferenceSet deleteRows(UserInfo user, String tableId, RowSelection rowsToDelete)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Append all rows from the provided iterator into the a table. This method will
	 * batch rows into optimum sized RowSets.
	 * 
	 * Note: This method will only keep one batch of rows in memory at a time so it
	 * should be suitable for appending very large change sets to a table.
	 * 
	 * @param user      The user appending the rows
	 * @param tableId   The ID of the table entity to append the rows too.
	 * @param models    The schema of the rows being appended.
	 * @param rowStream The stream of rows to append to the table.
	 * @param results   This parameter is optional. When provide, it will be
	 *                  populated with a RowReference for each row appended to the
	 *                  table. This parameter should be null for large change sets
	 *                  to minimize memory usage.
	 * @param The       callback will be called for each batch of rows appended to
	 *                  the table. Can be null.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	TableUpdateResponse appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns,
			Iterator<SparseRowDto> rowStream, String etag, RowReferenceSet results, ProgressCallback progressCallback,
			long transactionId) throws DatastoreException, NotFoundException, IOException;

	/**
	 * List the changes that have been applied to a table.
	 * 
	 * @param tableId
	 * @return
	 */
	@Deprecated
	List<TableRowChange> listRowSetsKeysForTable(String tableId);

	/**
	 * Get the a SparseChangeSet for a given TableRowChange.
	 * 
	 * @param change
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	SparseChangeSet getSparseChangeSet(TableRowChange change) throws NotFoundException, IOException;

	/**
	 * Get the schema change for a given version.
	 * 
	 * @param tableId
	 * @param versionNumber
	 * @return
	 * @throws IOException
	 */
	List<ColumnChangeDetails> getSchemaChangeForVersion(String tableId, long versionNumber) throws IOException;

	/**
	 * Get the last table row change
	 * 
	 * @param tableId
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	TableRowChange getLastTableRowChange(String tableId) throws IOException, NotFoundException;

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
	Row getCellValue(UserInfo userInfo, String tableId, RowReference rowRef, ColumnModel model)
			throws IOException, NotFoundException;

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
	RowSet getCellValues(UserInfo userInfo, String tableId, List<RowReference> rows, List<ColumnModel> columns)
			throws IOException, NotFoundException;

	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of FileHandleIds
	 * that are actually associated with the table.
	 * 
	 * @param objectId
	 * @throws TemporarilyUnavailableException if this query cannot be run at this
	 *                                         time.
	 */
	Set<Long> getFileHandleIdsAssociatedWithTable(String tableId, Set<Long> toTest)
			throws TemporarilyUnavailableException;

	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of FileHandleIds
	 * that are actually associated with the table.
	 * 
	 * @param objectId
	 */
	Set<String> getFileHandleIdsAssociatedWithTable(String tableId, List<String> toTest)
			throws TemporarilyUnavailableException;

	/**
	 * Set the schema of the table.
	 * 
	 * @param userInfo
	 * @param columnIds
	 * @param id
	 */
	void setTableSchema(UserInfo userInfo, List<String> columnIds, String id);

	/**
	 * Mark a table as deleted. This occurs when a table is moved to the trash. The
	 * actual data for the table will only be deleted if the table no longer exists.
	 * 
	 * @param deletedId
	 */
	void setTableAsDeleted(String deletedId);

	/**
	 * 
	 * @param deletedId
	 */
	void deleteTable(String deletedId);

	/**
	 * Is a temporary table needed to validate the given table update request.
	 * 
	 * @param change
	 * @return
	 */
	boolean isTemporaryTableNeededToValidate(TableUpdateRequest change);

	/**
	 * Validate a single update request.
	 * 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @param indexManager The index manager is only provided if a temporary table
	 *                     was created for the purpose of validation.
	 */
	void validateUpdateRequest(ProgressCallback callback, UserInfo userInfo, TableUpdateRequest change,
			TableIndexManager indexManager);

	/**
	 * Update the table with the given request.
	 * 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	TableUpdateResponse updateTable(ProgressCallback callback, UserInfo userInfo, TableUpdateRequest change,
			long transactionId);

	/**
	 * Get the schema for the table.
	 * 
	 * @param user
	 * @param idAndVersion
	 * @return
	 */
	List<String> getTableSchema(IdAndVersion idAndVersion);

	/**
	 * Delete all data about a table if it no longer exists. If the passed tableId
	 * is in the trash, then this method will not delete anything.
	 * 
	 * @param tableId
	 */
	void deleteTableIfDoesNotExist(String tableId);

	/**
	 * Create a new Iterator that can be used to iterator over all change metadata
	 * for the given table.
	 * 
	 * @param tableId
	 * @return
	 */
	Iterator<TableChangeMetaData> newTableChangeIterator(String tableId);

	/**
	 * Get a single page of TableChangeMetaData for the given table. The metadata
	 * object can also be used to dynamically load the actual change.
	 * 
	 * @param tableId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<TableChangeMetaData> getTableChangePage(String tableId, long limit, long offset);

	/**
	 * Create a new version of the given table an bind the new version to the
	 * provided transaction id.
	 * 
	 * @param userInfo
	 * @param versionRequest
	 * @param transactionId
	 * @return The version number of the newly created version.
	 */
	long createSnapshotAndBindToTransaction(UserInfo userInfo, String tableId, SnapshotRequest snapshotRequest,
			long transactionId);
	
	/**
	 * Get the transaction Id for a table version.
	 * 
	 * @param tableId
	 * @param version
	 * @return If there is a transaction for the given table and version then the
	 *         transaction ID will be returned. Optional.empty() if such a
	 *         transaction does not exist.
	 * 
	 */
	Optional<Long> getTransactionForVersion(String tableId, long version);

	/**
	 * Create a snapshot of of the given table.
	 * @param userInfo
	 * @param tableId
	 * @param request
	 * @return
	 */
	SnapshotResponse createTableSnapshot(UserInfo userInfo, String tableId, SnapshotRequest request);
}
