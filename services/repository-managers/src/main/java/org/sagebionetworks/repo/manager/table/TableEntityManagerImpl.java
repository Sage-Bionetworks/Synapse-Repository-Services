package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.SynchronizedProgressCallback;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionBuilder;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.table.model.TableChange;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableEntityManagerImpl implements TableEntityManager {
	
	private static final long PAGE_SIZE_LIMIT = 1000L;

	public static final String MAXIMUM_TABLE_SIZE_EXCEEDED = "Maximum table size exceeded.";

	/**
	 * See PLFM-4774
	 */
	public static final long MAXIMUM_VERSIONS_PER_TABLE = 30*1000;
	
	/**
	 * See: PLFM-5456
	 */
	private static final int EXCLUSIVE_LOCK_TIMEOUT_SECONDS = 5;
	
	public static final int READ_LOCK_TIMEOUT_SEC = 60;
	
	@Autowired
	TableRowTruthDAO tableRowTruthDao;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	ColumnModelManager columModelManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TransactionTemplate readCommitedTransactionTemplate;
	@Autowired
	TableUploadManager tableUploadManager;
	@Autowired
	TableTransactionDao tableTransactionDao;
	@Autowired
	NodeManager nodeManager;
	
	/**
	 * Injected via spring
	 */
	int maxBytesPerRequest;
	
	/**
	 * Injected by spring
	 */
	int maxBytesPerChangeSet;

	/**
	 * Injected via spring
	 * @param maxBytesPerChangeSet
	 */
	public void setMaxBytesPerChangeSet(int maxBytesPerChangeSet) {
		this.maxBytesPerChangeSet = maxBytesPerChangeSet;
	}


	@WriteTransaction
	@Override
	public RowReferenceSet appendRows(UserInfo user, String tableId, RowSet delta, ProgressCallback progressCallback, long transactionId)
			throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(delta, "RowSet");
		List<ColumnModel> currentSchema = columModelManager.getColumnModelsForTable(user, tableId);
		// Validate the request is under the max bytes per requested
		TableModelUtils.validateRequestSize(currentSchema, delta, maxBytesPerRequest);
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		SparseChangeSet sparseChangeSet = TableModelUtils.createSparseChangeSet(delta, currentSchema);
		SparseChangeSetDto dto = sparseChangeSet.writeToDto();
		appendRowsAsStream(user, tableId, currentSchema, dto.getRows().iterator(), delta.getEtag(), results, progressCallback, transactionId);
		return results;
	}
	
	@WriteTransaction
	@Override
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId,
			PartialRowSet partial, ProgressCallback progressCallback, long transactionId)
			throws DatastoreException, NotFoundException, IOException {
		Validate.required(user, "User");
		Validate.required(tableId, "TableId");
		Validate.required(partial, "RowsToAppendOrUpdate");
		List<ColumnModel> currentSchema = columModelManager.getColumnModelsForTable(user, tableId);
		// Validate the request is under the max bytes per requested
		TableModelUtils.validateRequestSize(partial, maxBytesPerRequest);
		TableModelUtils.validatePartialRowSet(partial, currentSchema);
		
		/*
		 * Partial change sets do not require row level conflict checking.
		 * By using the version number and etag from the last row change applied
		 * to the table, row level conflict check is bypassed.
		 */
		TableRowChange lastRowChange = tableRowTruthDao.getLastTableRowChange(tableId, TableChangeType.ROW);
		SparseChangeSetDto dto = TableModelUtils.createSparseChangeSetFromPartialRowSet(lastRowChange, partial);
		RowReferenceSet results = new RowReferenceSet();
		appendRowsAsStream(user, tableId, currentSchema, dto.getRows().iterator(), results.getEtag(), results, progressCallback, transactionId);
		return results;
	}
	

	@WriteTransaction
	@Override
	public RowReferenceSet deleteRows(UserInfo user, String tableId, RowSelection rowsToDelete) throws DatastoreException, NotFoundException,
			IOException {
		Validate.required(user, "user");
		Validate.required(tableId, "tableId");
		Validate.required(rowsToDelete, "rowsToDelete");
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);

		// Validate the user has permission to edit the table
		tableManagerSupport.validateTableWriteAccess(user, idAndVersion);
		
		List<ColumnModel> columns = tableManagerSupport.getColumnModelsForTable(idAndVersion);
		SparseChangeSet changeSet = new SparseChangeSet(tableId, columns, rowsToDelete.getEtag());
		for(Long rowId: rowsToDelete.getRowIds()){
			SparseRow row = changeSet.addEmptyRow();
			// A delete row has an ID and no values.
			row.setRowId(rowId);
		}
		long transactionId = tableTransactionDao.startTransaction(tableId, user.getId());
		RowReferenceSet result = appendRowsToTable(user, columns, changeSet, transactionId);
		// The table has change so we must reset the state.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
		return result;
	}

	@WriteTransaction
	@Override
	public TableUpdateResponse appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns, Iterator<SparseRowDto> rowStream, String etag,
			RowReferenceSet results, ProgressCallback progressCallback, long transactionId) throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(columns, "columns");
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// Validate the user has permission to edit the table
		tableManagerSupport.validateTableWriteAccess(user, idAndVersion);

		// Touch an lock on the table.
		tableManagerSupport.touchTable(user, tableId);
		
		/*
		 * RowId and RowVersion can be ignored when appending data to an empty
		 * table. See PLFM-3155.
		 */
		boolean ignoreRowIdAndVersion = !tableRowTruthDao.hasAtLeastOneChangeOfType(tableId, TableChangeType.ROW);
		
		List<SparseRowDto> batch = new LinkedList<SparseRowDto>();
		int batchSizeBytes = 0;
		long rowCount = 0;
		while(rowStream.hasNext()){
			SparseRowDto row = rowStream.next();
			if(ignoreRowIdAndVersion){
				row.setRowId(null);
				row.setVersionNumber(null);
			}
			batch.add(row);
			rowCount++;
			// batch using the actual size of the row.
			batchSizeBytes += TableModelUtils.calculateActualRowSize(row);
			if(batchSizeBytes >= maxBytesPerChangeSet){
				// Send this batch and keep the etag.
				SparseChangeSet delta = new SparseChangeSet(tableId, columns, batch, etag);
				etag = appendBatchOfRowsToTable(user, columns, delta, results, progressCallback, transactionId);
				// Clear the batch
				batch.clear();
				batchSizeBytes = 0;
			}
		}
		// Send the last batch is there are any rows
		if(!batch.isEmpty()){
			// Validate there aren't any illegal file handle replaces
			SparseChangeSet delta = new SparseChangeSet(tableId, columns, batch, etag);
			etag = appendBatchOfRowsToTable(user, columns, delta, results, progressCallback, transactionId);
		}
		// The table has change so we must reset the state.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
		// Done
		UploadToTableResult result = new UploadToTableResult();
		result.setRowsProcessed(rowCount);
		result.setEtag(etag);
		return result;
	}

	/**
	 * Check that stack's status is set to READ_WRITE.
	 * @throws ReadOnlyException Thrown if the stack status is anything other than READ_WRITE.
	 */
	private void checkStackWiteStatus() throws ReadOnlyException {
		StatusEnum status = stackStatusDao.getCurrentStatus();
		if(!StatusEnum.READ_WRITE.equals(status)){
			throw new ReadOnlyException("Write operations are not allowed while the stack status is set to: "+status.name());
		}
		
	}

	/**
	 * Append a batch of rows to a table.
	 * 
	 * @param tableId
	 * @param columnMapper
	 * @param ETAG
	 * @param results
	 * @param headers
	 * @param batch
	 * @return
	 * @throws IOException
	 * @throws ReadOnlyException If the stack status is anything other than READ_WRITE
	 */
	private String appendBatchOfRowsToTable(UserInfo user, List<ColumnModel> columns, SparseChangeSet delta, RowReferenceSet results,
			ProgressCallback progressCallback, long transactionId)
			throws IOException, ReadOnlyException {
		RowReferenceSet rrs = appendRowsToTable(user, columns, delta, transactionId);
		if(results != null){
			results.setEtag(rrs.getEtag());
			results.setHeaders(TableModelUtils.getSelectColumns(columns));
			results.setTableId(delta.getTableId());
			if(results.getRows() == null){
				results.setRows(new LinkedList<RowReference>());
			}
			if(rrs.getRows()!= null){
				results.getRows().addAll(rrs.getRows());
			}
		}
		return rrs.getEtag();
	}


	/**
	 * Append the given rows to the 
	 * @param user
	 * @param columns
	 * @param delta
	 * @return
	 * @throws IOException
	 */
	RowReferenceSet appendRowsToTable(UserInfo user, List<ColumnModel> columns,
			SparseChangeSet delta, long transactionId) throws IOException {
		// See PLFM-3041
		checkStackWiteStatus();
		validateFileHandles(user, delta.getTableId(), delta);
				
		// Now set the row version numbers and ID.
		int coutToReserver = TableModelUtils.countEmptyOrInvalidRowIds(delta);
		// Reserver IDs for the missing
		IdRange range = tableRowTruthDao.reserveIdsInRange(delta.getTableId(), coutToReserver);
		
		// validate the table would be within the size limit.
		if(range.getVersionNumber() > MAXIMUM_VERSIONS_PER_TABLE) {
			throw new IllegalArgumentException(MAXIMUM_TABLE_SIZE_EXCEEDED);
		}
		
		// Are any rows being updated?
		if (coutToReserver < delta.getRowCount()) {
			// Validate that this update does not contain any row level conflicts.
			checkForRowLevelConflict(delta.getTableId(), delta);
		}
		// Now assign the rowIds and set the version number
		TableModelUtils.assignRowIdsAndVersionNumbers(delta, range);
		
		tableRowTruthDao.appendRowSetToTable(user.getId().toString(), delta.getTableId(), range.getEtag(), range.getVersionNumber(), columns, delta.writeToDto(), transactionId);
		
		// Prepare the results
		RowReferenceSet results = new RowReferenceSet();
		results.setHeaders(TableModelUtils.getSelectColumns(columns));
		results.setTableId(delta.getTableId());
		results.setEtag(range.getEtag());
		List<RowReference> refs = new LinkedList<RowReference>();
		// Build up the row references
		for (SparseRow row : delta.rowIterator()) {
			RowReference ref = new RowReference();
			ref.setRowId(row.getRowId());
			ref.setVersionNumber(row.getVersionNumber());
			refs.add(ref);
		}
		results.setRows(refs);
		return results;
	}
	
	/**
	 * Check for row level conflicts with the given change set.
	 * 
	 * @param tableIdString
	 * @param delta
	 * @throws IOException
	 */
	public void checkForRowLevelConflict(String tableIdString, SparseChangeSet delta) throws IOException {
		// Map each valid row to its version number
		Map<Long, Long> rowIdToRowVersionNumberFromUpdate = TableModelUtils.getDistictValidRowIds(delta.rowIterator());
		long versionOfDelta = -1;
		for(Long versionNumber: rowIdToRowVersionNumberFromUpdate.values()){
			if(versionNumber == null){
				throw new IllegalArgumentException("Row version number cannot be null");
			}
			versionOfDelta = Math.max(versionOfDelta, versionNumber);
		}
		// If we were given an etag we can use it to determine the version used to create the delta.
		if(delta.getEtag() != null){
			long versionOfEtag = tableRowTruthDao.getVersionForEtag(tableIdString, delta.getEtag());
			versionOfDelta = Math.max(versionOfDelta, versionOfEtag);
		}
		final Set<Long> deltaRowIds = rowIdToRowVersionNumberFromUpdate.keySet();
		if(!deltaRowIds.isEmpty()){
			// Need to check all changes that have been applied since the version of the delta?
			List<TableRowChange> rowChanges = tableRowTruthDao.listRowSetsKeysForTableGreaterThanVersion(tableIdString, versionOfDelta);
			// scan all changes greater than this row.
			for (final TableRowChange rowChange : rowChanges) {
				if(TableChangeType.ROW.equals(rowChange.getChangeType())){
					SparseChangeSetDto change = tableRowTruthDao.getRowSet(rowChange);
					for(SparseRowDto row: change.getRows()){
						if (deltaRowIds.contains(row.getRowId())) {
							throw new ConflictingUpdateException("Row id: " + row.getRowId()
									+ " has been changed since last read.  Please get the latest value for this row and then attempt to update it again.");
						}			
					}
				}
			}
		}
	}

	@Deprecated
	@Override
	public List<TableRowChange> listRowSetsKeysForTable(String tableId) {
		return tableRowTruthDao.listRowSetsKeysForTable(tableId);
	}

	@Override
	public TableRowChange getLastTableRowChange(String tableId) throws IOException, NotFoundException {
		return tableRowTruthDao.getLastTableRowChange(tableId);
	}

	@Override
	public Row getCellValue(UserInfo userInfo, String tableId, RowReference rowRef, ColumnModel column) throws IOException,
			NotFoundException {
		RowSet set = getCellValues(userInfo, tableId, Lists.newArrayList(rowRef), Lists.newArrayList(column));
		if(set.getRows() == null || set.getRows().isEmpty()){
			throw new NotFoundException("Row ID: "+rowRef.getRowId());
		}
		return set.getRows().get(0);
	}

	@Override
	public RowSet getCellValues(UserInfo userInfo, String tableId, List<RowReference> rows, List<ColumnModel> columns)
			throws IOException, NotFoundException {
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);		
		tableManagerSupport.validateTableReadAccess(userInfo, idAndVersion);
		EntityType type = tableManagerSupport.getTableEntityType(idAndVersion);
		if(!EntityType.table.equals(type)){
			throw new UnauthorizedException("Can only be called for TableEntities");
		}
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		String sql = SQLUtils.buildSelectRowIds(tableId, rows, columns);
		final Map<Long, Row> rowMap = new HashMap<Long, Row>(rows.size());
		try {
			SqlQuery query = new SqlQueryBuilder(sql, columns).build();
			indexDao.queryAsStream(null, query, new  RowHandler() {
				@Override
				public void nextRow(Row row) {
					rowMap.put(row.getRowId(), row);
				}
			});
			RowSet results = new RowSet();
			results.setTableId(tableId);
			results.setHeaders(query.getSelectColumns());
			List<Row> resultRows = new LinkedList<Row>();
			results.setRows(resultRows);
			for(RowReference ref:rows){
				Row row = rowMap.get(ref.getRowId());
				if(row != null){
					resultRows.add(row);
				}
			}
			return results;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public void setMaxBytesPerRequest(int maxBytesPerRequest) {
		this.maxBytesPerRequest = maxBytesPerRequest;
	}


	/**
	 * Validate the caller has access to the fileHandles referenced in the given changeset.
	 * @param user
	 * @param tableId
	 * @param rowSet
	 */
	public void validateFileHandles(UserInfo user, String tableId,
			SparseChangeSet rowSet) {
		if(user.isAdmin()){
			return;
		}
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// Extract the files handles from the change set.
		Set<Long> filesHandleIds = rowSet.getFileHandleIdsInSparseChangeSet();
		if(!filesHandleIds.isEmpty()){
			// convert the longs to strings.
			List<String> fileHandesToCheck = new LinkedList<String>();
			CollectionUtils.convertLongToString(filesHandleIds, fileHandesToCheck);
			// Which files were created by the user?
			Set<String> filesCreatedByUser = fileHandleDao.getFileHandleIdsCreatedByUser(user.getId(), fileHandesToCheck);
			// build up the set of files not created by the user.
			Set<Long> remainingFilesToCheck = new HashSet<Long>();
			for(String fileString: fileHandesToCheck){
				if(!filesCreatedByUser.contains(fileString)){
					remainingFilesToCheck.add(Long.parseLong(fileString));
				}
			}
			// are there any more files to check?
			if(!remainingFilesToCheck.isEmpty()){
				// The remaining files were not created by the user so they must already be associated with the table.
				TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
				// Get the sub-set of files associated with the table.
				Set<Long> filesAssociatedWithTable = indexDao.getFileHandleIdsAssociatedWithTable(new HashSet<Long>(remainingFilesToCheck), idAndVersion);
				// remove all files associated with the table
				remainingFilesToCheck.removeAll(filesAssociatedWithTable);
				// Any files remaining in the set are not created by the user and are not associated with the table.
				if(!remainingFilesToCheck.isEmpty()){
					throw new UnauthorizedException("Cannot access files: "+remainingFilesToCheck.toString());
				}
			}
		}
	}
	
	@Override
	public Set<String> getFileHandleIdsAssociatedWithTable(String tableId,
			List<String> toTest) {
		Set<Long> longSet = new HashSet<Long>(toTest.size());
		CollectionUtils.convertStringToLong(toTest, longSet);
		Set<Long> results = getFileHandleIdsAssociatedWithTable(tableId, longSet);
		Set<String> resultString = new HashSet<String>(results.size());
		CollectionUtils.convertLongToString(results, resultString);
		return resultString;
	}

	@Override
	public Set<Long> getFileHandleIdsAssociatedWithTable(final String tableId,
			final Set<Long> toTest) {
		// What is the current version of the talbe's truth?
		TableRowChange lastChange = tableRowTruthDao.getLastTableRowChange(tableId);
		if(lastChange == null){
			// There are no changes applied to this table so return an empty set.
			return Sets.newHashSet();
		}
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// Next connect to the table
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		// the index dao 
		return indexDao.getFileHandleIdsAssociatedWithTable(toTest, idAndVersion);
	}

	@WriteTransaction
	@Override
	public void setTableSchema(final UserInfo userInfo, final List<String> newSchema, final String tableId) {
		try {
			IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
			SynchronizedProgressCallback callback = new SynchronizedProgressCallback();
			tableManagerSupport.tryRunWithTableExclusiveLock(callback, idAndVersion, EXCLUSIVE_LOCK_TIMEOUT_SECONDS,
					(ProgressCallback callbackInner) -> {
						setTableSchemaWithExclusiveLock(callbackInner, userInfo, newSchema, tableId);
						return null;
					});
		} catch (LockUnavilableException e) {
			throw new TemporarilyUnavailableException("Cannot update an unavailable table");
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Note: This method should only be called while holding an exclusive lock on the table.
	 * @param userInfo
	 * @param newSchema
	 * @param tableId
	 */
	void setTableSchemaWithExclusiveLock(final ProgressCallback callback, final UserInfo userInfo, final List<String> newSchema,
			final String tableId) {
		// Lookup the current schema for this table
		List<String> oldSchema = columModelManager.getColumnIdForTable(IdAndVersion.parse(tableId));
		// Calculate the schema change (if there is one).
		List<ColumnChange> schemaChange = TableModelUtils.createChangesFromOldSchemaToNew(oldSchema, newSchema);
		TableSchemaChangeRequest changeRequest = new TableSchemaChangeRequest();
		changeRequest.setChanges(schemaChange);
		changeRequest.setEntityId(tableId);
		changeRequest.setOrderedColumnIds(newSchema);
		// Start a transaction to change the table to the new schema.
		long transactionId = tableTransactionDao.startTransaction(tableId, userInfo.getId());
		updateTableSchema(callback, userInfo, changeRequest, transactionId);
	}

	@Override
	public boolean isTemporaryTableNeededToValidate(TableUpdateRequest change) {
		if(change instanceof TableSchemaChangeRequest){
			TableSchemaChangeRequest schemaChange = (TableSchemaChangeRequest) change;
			// If one or more of the existing columns will change then a temporary table is needed to validate the change.
			return containsColumnUpdate(schemaChange.getChanges());
		}else if(change instanceof UploadToTableRequest){
			// might switch to true to support uniqueness constraints.
			return false;
		}else if(change instanceof AppendableRowSetRequest){
			return false;
		}else{
			throw new IllegalArgumentException("Unknown change type: "+change.getClass().getName());
		}
	}
	
	/**
	 * Does the given change include an update of an existing column?
	 * 
	 * @param changes
	 * @return
	 */
	public static boolean containsColumnUpdate(
			List<ColumnChange> changes) {
		if(changes == null){
			return false;
		}
		if(changes.isEmpty()){
			return false;
		}
		for(ColumnChange change: changes){
			if(change.getNewColumnId() != null && change.getOldColumnId() != null){
				if(!change.getNewColumnId().equals(change.getOldColumnId())){
					// a column change requires a temporary table to validate.
					return true;
				}
			}
		}
		return false;
	}


	@Override
	public void validateUpdateRequest(ProgressCallback callback,
			UserInfo userInfo, TableUpdateRequest change,
			TableIndexManager indexManager) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(change, "change");
		if(change instanceof TableSchemaChangeRequest){
			validateSchemaUpdateRequest(callback, userInfo, (TableSchemaChangeRequest)change, indexManager);
		}else if(change instanceof UploadToTableRequest){
			// nothing to validate
		}else if(change instanceof AppendableRowSetRequest){
			// nothing to validate
		}else{
			throw new IllegalArgumentException("Unknown request type: "+change.getClass().getName());
		}
		
	}
	
	/**
	 * Validation for a schema change request.
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @param indexManager
	 */
	public void validateSchemaUpdateRequest(ProgressCallback callback,
			UserInfo userInfo, TableSchemaChangeRequest changes,
			TableIndexManager indexManager) {
		// first determine what the new Schema will be
		columModelManager.calculateNewSchemaIdsAndValidate(changes.getEntityId(), changes.getChanges(), changes.getOrderedColumnIds());
		// If the change includes an update then the schema change must be checked against the temp table.
		boolean includesUpdate = containsColumnUpdate(changes.getChanges());
		if(includesUpdate){
			if(indexManager == null){
				throw new IllegalStateException("A temporary table is needed to validate but was not provided.");
			}
			List<ColumnChangeDetails> details = columModelManager.getColumnChangeDetails(changes.getChanges());
			IdAndVersion idAndVersion = IdAndVersion.parse(changes.getEntityId());
			// attempt to apply the schema change to the temp copy of the table.
			indexManager.alterTempTableSchmea(idAndVersion, details);
		}
	}


	@Override
	public TableUpdateResponse updateTable(ProgressCallback callback,
			UserInfo userInfo, TableUpdateRequest change, long transactionId) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(change, "change");
		if(change instanceof TableSchemaChangeRequest){
			return updateTableSchema(callback, userInfo, (TableSchemaChangeRequest)change, transactionId);
		}else if(change instanceof UploadToTableRequest){
			return uploadToTable(callback, userInfo, (UploadToTableRequest)change, transactionId);
		}else if(change instanceof AppendableRowSetRequest){
			return appendToTable(callback, userInfo, (AppendableRowSetRequest)change, transactionId);
		}else{
			throw new IllegalArgumentException("Unknown request type: "+change.getClass().getName());
		}
	}
	
	/**
	 * Append a rowset to a table from a transaction.
	 * @param callback
	 * @param userInfo
	 * @param request
	 * @return
	 */
	TableUpdateResponse appendToTable(ProgressCallback callback,
			UserInfo userInfo, AppendableRowSetRequest request, long transactionId) {
		ValidateArgument.required(request.getToAppend(), "AppendableRowSetRequest.toAppend");
		try {
			RowReferenceSet results = null;
			if(request.getToAppend() instanceof PartialRowSet){
				PartialRowSet partialRowSet = (PartialRowSet) request.getToAppend();
				results =  appendPartialRows(userInfo, partialRowSet.getTableId(), partialRowSet, callback, transactionId);
			}else if(request.getToAppend() instanceof RowSet){
				RowSet rowSet = (RowSet)request.getToAppend();
				results = appendRows(userInfo, rowSet.getTableId(), rowSet, callback, transactionId);
			}else{
				throw new IllegalArgumentException("Unknown RowSet type: "+request.getToAppend().getClass().getName());
			}
			RowReferenceSetResults  rrsr = new RowReferenceSetResults();
			rrsr.setRowReferenceSet(results);
			return rrsr;
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Upload the given file request to 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 * @throws IOException 
	 */
	TableUpdateResponse uploadToTable(ProgressCallback callback,
			UserInfo userInfo, UploadToTableRequest change, long transactionId) {
		// Touch an lock on the table.
		tableManagerSupport.touchTable(userInfo, change.getTableId());
		// upload the CSV to the table.
		return tableUploadManager.uploadCSV(callback, userInfo, change, new UploadRowProcessor() {
			@Override
			public TableUpdateResponse processRows(UserInfo user, String tableId, List<ColumnModel> tableSchema,
					Iterator<SparseRowDto> rowStream, String updateEtag, ProgressCallback progressCallback)
					throws DatastoreException, NotFoundException, IOException {
				return appendRowsAsStream(user, tableId, tableSchema, rowStream, updateEtag, null, progressCallback, transactionId);
			}});
	}


	/**
	 * Apply a validated schema change request.
	 * 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	TableSchemaChangeResponse updateTableSchema(ProgressCallback callback,
			UserInfo userInfo, TableSchemaChangeRequest changes, long transactionId) {
		IdAndVersion idAndVersion = IdAndVersion.parse(changes.getEntityId());
		// First determine if this will be an actual change to the schema.
		List<String> newSchemaIds = columModelManager.calculateNewSchemaIdsAndValidate(changes.getEntityId(), changes.getChanges(), changes.getOrderedColumnIds());
		List<String> currentSchemaIds = columModelManager.getColumnIdForTable(idAndVersion);
		List<ColumnModel> newSchema = null;
		if (!currentSchemaIds.equals(newSchemaIds)) {
			// This will 
			newSchema = applySchemaChangeToTable(userInfo, changes.getEntityId(), newSchemaIds, changes.getChanges(), transactionId);
		}else {
			// The schema will not change so return the current schema.
			newSchema = columModelManager.getColumnModelsForObject(idAndVersion);
		}
		
		TableSchemaChangeResponse response = new TableSchemaChangeResponse();
		response.setSchema(newSchema);
		return response;
	}
	
	/**
	 * Apply the given schema change to the table.
	 * @param userInfo
	 * @param tableId
	 * @param newSchemaIds
	 * @param changes
	 * @param transactionId
	 * @return
	 */
	List<ColumnModel> applySchemaChangeToTable(UserInfo userInfo, String tableId, List<String> newSchemaIds,
			List<ColumnChange> changes, long transactionId) {
		// This is a change.
		tableManagerSupport.touchTable(userInfo, tableId);
		List<ColumnModel> newSchema = columModelManager.bindColumnsToDefaultVersionOfObject(newSchemaIds, tableId);
		tableRowTruthDao.appendSchemaChangeToTable("" + userInfo.getId(), tableId, newSchemaIds, changes,
				transactionId);
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// trigger an update.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
		return newSchema;
	}

	@Override
	public List<ColumnChangeDetails> getSchemaChangeForVersion(String tableId,
			long versionNumber) throws IOException {
		List<ColumnChange> changes = tableRowTruthDao.getSchemaChangeForVersion(tableId, versionNumber);
		return columModelManager.getColumnChangeDetails(changes);
	}


	@Override
	public List<String> getTableSchema(final IdAndVersion inputIdAndVersion) {
		IdAndVersionBuilder lookupBuilder = IdAndVersion.newBuilder();
		lookupBuilder.setId(inputIdAndVersion.getId());
		if(inputIdAndVersion.getVersion().isPresent()) {
			/*
			 * The current version of any table is always 'in progress' and does not have a
			 * schema bound to it. This means the schema for the current version always
			 * matches the latest schema for the table. Therefore, when a caller explicitly
			 * requests the schema of the current version, the latest schema is returned.
			 */
			long currentVersion = nodeManager.getCurrentRevisionNumber(inputIdAndVersion.getId().toString());
			long inputVersion = inputIdAndVersion.getVersion().get();
			if(inputVersion != currentVersion) {
				// Only use the input version number when it is not the current version.
				lookupBuilder.setVersion(inputVersion);
			}
			
		}
		// lookup the schema for the appropriate version.
		return columModelManager.getColumnIdForTable(lookupBuilder.build());
	}

	
	@Override
	public SparseChangeSet getSparseChangeSet(TableRowChange change) throws NotFoundException, IOException {
		ValidateArgument.required(change, "TableRowChange");
		ValidateArgument.required(change.getKeyNew(), "TableRowChange.keyNew");
		SparseChangeSetDto dto = tableRowTruthDao.getRowSet(change);
		List<ColumnModel> schema = columModelManager.getAndValidateColumnModels(dto.getColumnIds());
		return new SparseChangeSet(dto, schema);
	}


	@WriteTransaction
	@Override
	public void deleteTableIfDoesNotExist(String tableId) {
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		if(!tableManagerSupport.doesTableExist(idAndVersion)) {
			// The table no longer exists so delete it.
			this.deleteTable(tableId);
		}
	}

	@WriteTransaction
	@Override
	public void setTableAsDeleted(String deletedId) {
		IdAndVersion idAndVersion = IdAndVersion.parse(deletedId);
		tableManagerSupport.setTableDeleted(idAndVersion, ObjectType.TABLE);
	}

	@WriteTransaction
	@Override
	public void deleteTable(String deletedId) {
		columModelManager.unbindAllColumnsAndOwnerFromObject(deletedId);
		tableRowTruthDao.deleteAllRowDataForTable(deletedId);
		tableTransactionDao.deleteTable(deletedId);
	}


	@Override
	public Iterator<TableChangeMetaData> newTableChangeIterator(final String tableId) {
		// convert from a paginated result to an iterator.
		return new PaginationIterator<TableChangeMetaData>((long limit, long offset) -> {
			return getTableChangePage(tableId, limit, offset);
		}, PAGE_SIZE_LIMIT);
	}
	
	@Override
	public List<TableChangeMetaData> getTableChangePage(String tableId, long limit, long offset){
		List<TableRowChange> innerChangePage = tableRowTruthDao.getTableChangePage(tableId, limit, offset);
		// Wrap the metadata to allow the full change be dynamically loaded.
		List<TableChangeMetaData> results = new ArrayList<>(innerChangePage.size());
		for(TableRowChange toWrap: innerChangePage) {
			TableChangeWrapper wrapper = new TableChangeWrapper(toWrap);
			results.add(wrapper);
		}
		return results;
	}
	
	/**
	 * Wrapper of table change metadata that supports dynamically loading the full
	 * change on demand.
	 *
	 */
	private class TableChangeWrapper implements TableChangeMetaData {

		private TableRowChange wrapped;

		TableChangeWrapper(TableRowChange toWrap) {
			this.wrapped = toWrap;
		}

		@Override
		public Long getChangeNumber() {
			return wrapped.getRowVersion();
		}

		@Override
		public TableChangeType getChangeType() {
			return wrapped.getChangeType();
		}
		
		@Override
		public String getETag() {
			return wrapped.getEtag();
		}

		@Override
		public <T extends TableChange> ChangeData<T> loadChangeData(Class<T> clazz)
				throws NotFoundException, IOException {
			TableChange tableChange = null;
			switch (wrapped.getChangeType()) {
			case ROW:
				tableChange = getSparseChangeSet(wrapped);
				break;
			case COLUMN:
				List<ColumnChangeDetails> details = getSchemaChangeForVersion(wrapped.getTableId(),
						wrapped.getRowVersion());
				tableChange = new SchemaChange(details);
				break;
			default:
				throw new IllegalStateException("Unknown type: " + wrapped.getChangeType());
			}
			return new ChangeData<>(wrapped.getRowVersion(), clazz.cast(tableChange));
		}

	}

	@WriteTransaction
	@Override
	public long createSnapshotAndBindToTransaction(UserInfo userInfo, String tableId, SnapshotRequest snapshotRequest,
			long transactionId) {
		// create a new version
		long snapshotVersion = nodeManager.createSnapshotAndVersion(userInfo, tableId, snapshotRequest);
		linkVersionToTransaction(tableId, snapshotVersion, transactionId);
		return snapshotVersion;
	}
	
	/**
	 * Link a table version to a transaction.
	 * 
	 * @param tableIdString
	 * @param version
	 * @param transactionId
	 */
	void linkVersionToTransaction(String tableIdString, long version, long transactionId) {
		ValidateArgument.required(tableIdString, "tableId");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		// Lock the parent row and check the table is associated with the transaction.
		long transactionTableId = tableTransactionDao.getTableIdWithLock(transactionId);
		if(transactionTableId != tableId) {
			throw new IllegalArgumentException("Transaction: "+transactionId+" is not associated with table: "+tableIdString);
		}
		tableTransactionDao.linkTransactionToVersion(transactionId, version);
		// bump the parent etag so the change can migrate.
		tableTransactionDao.updateTransactionEtag(transactionId);
		// bind the current schema to the version
		columModelManager.bindDefaultColumnsToObjectVersion(IdAndVersion.newBuilder().setId(tableId).setVersion(version).build());
	}


	@Override
	public Optional<Long> getTransactionForVersion(String tableId, long version) {
		return tableTransactionDao.getTransactionForVersion(tableId, version);
	}


	@WriteTransaction
	@Override
	public SnapshotResponse createTableSnapshot(UserInfo userInfo, String tableIdString, SnapshotRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(tableIdString, "TableId");
		ValidateArgument.required(request, "request");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		IdAndVersion idAndVersion = IdAndVersion.newBuilder().setId(tableId).build();
		// Validate the user has permission to edit the table
		tableManagerSupport.validateTableWriteAccess(userInfo, idAndVersion);
		// Table must have at least one transaction, such as setting the table's schema.
		Optional<Long> lastTransactionNumber = tableRowTruthDao.getLastTransactionId(tableIdString);
		if(!lastTransactionNumber.isPresent()) {
			throw new IllegalArgumentException("This table: "+tableId+" does not have a schema so a snapshot cannot be created.");
		}
		long snapshotVersion = createSnapshotAndBindToTransaction(userInfo, tableIdString, request, lastTransactionNumber.get());
		SnapshotResponse response = new SnapshotResponse();
		response.setSnapshotVersionNumber(snapshotVersion);
		return response;
	}

}
