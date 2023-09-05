package org.sagebionetworks.repo.manager.table;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.SynchronizedProgressCallback;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.FileEventUtils;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshot;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshotDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
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
import org.sagebionetworks.repo.model.table.TableSearchChangeRequest;
import org.sagebionetworks.repo.model.table.TableSearchChangeResponse;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SearchChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.table.model.TableChange;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
	private TableRowTruthDAO tableRowTruthDao;
	@Autowired
	private ConnectionFactory tableConnectionFactory;
	@Autowired
	private StackStatusDao stackStatusDao;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private ColumnModelManager columModelManager;
	@Autowired
	private TableManagerSupport tableManagerSupport;
	@Autowired
	private TableUploadManager tableUploadManager;
	@Autowired
	private TableTransactionDao tableTransactionDao;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private TableTransactionManager transactionManager;
	@Autowired
	private TableSnapshotDao tableSnapshotDao;
	@Autowired
	private StackConfiguration config;
	@Autowired
	private TransactionalMessenger messenger;

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
	public RowReferenceSet appendRows(UserInfo user, String tableId, RowSet delta, TableTransactionContext txContext) throws DatastoreException, NotFoundException, IOException {
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
		appendRowsAsStream(user, tableId, currentSchema, dto.getRows().iterator(), delta.getEtag(), results, txContext);
		return results;
	}
	
	@WriteTransaction
	@Override
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId, PartialRowSet partial, TableTransactionContext txContext) throws DatastoreException, NotFoundException, IOException {
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
		appendRowsAsStream(user, tableId, currentSchema, dto.getRows().iterator(), results.getEtag(), results, txContext);
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
		
		List<ColumnModel> columns = columModelManager.getColumnModelsForTable(user, tableId);
		SparseChangeSet changeSet = new SparseChangeSet(tableId, columns, rowsToDelete.getEtag());
		for(Long rowId: rowsToDelete.getRowIds()){
			SparseRow row = changeSet.addEmptyRow();
			// A delete row has an ID and no values.
			row.setRowId(rowId);
		}
		
		return transactionManager.executeInTransaction(user, tableId, txContext -> {
			RowReferenceSet result;
			try {
				result = appendRowsToTable(user, columns, changeSet, txContext);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return result;
		});
		
	}

	@WriteTransaction
	@Override
	public TableUpdateResponse appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns, Iterator<SparseRowDto> rowStream, String etag, RowReferenceSet results, TableTransactionContext txContext) throws DatastoreException, NotFoundException, IOException {
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
				etag = appendBatchOfRowsToTable(user, columns, delta, results, txContext);
				// Clear the batch
				batch.clear();
				batchSizeBytes = 0;
			}
		}
		// Send the last batch is there are any rows
		if(!batch.isEmpty()){
			// Validate there aren't any illegal file handle replaces
			SparseChangeSet delta = new SparseChangeSet(tableId, columns, batch, etag);
			etag = appendBatchOfRowsToTable(user, columns, delta, results, txContext);
		}
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
	private String appendBatchOfRowsToTable(UserInfo user, List<ColumnModel> columns, SparseChangeSet delta, RowReferenceSet results, TableTransactionContext txContext)
			throws IOException, ReadOnlyException {
		RowReferenceSet rrs = appendRowsToTable(user, columns, delta, txContext);
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
			SparseChangeSet delta, TableTransactionContext txContext) throws IOException {
		// See PLFM-3041
		checkStackWiteStatus();

		final String tableId = delta.getTableId();
		
		validateFileHandles(user, tableId, delta);
				
		// Now set the row version numbers and ID.
		int coutToReserver = TableModelUtils.countEmptyOrInvalidRowIds(delta);
		// Reserver IDs for the missing
		IdRange range = tableRowTruthDao.reserveIdsInRange(tableId, coutToReserver);
		
		// validate the table would be within the size limit.
		if(range.getVersionNumber() > MAXIMUM_VERSIONS_PER_TABLE) {
			throw new IllegalArgumentException(MAXIMUM_TABLE_SIZE_EXCEEDED);
		}
		
		// Are any rows being updated?
		if (coutToReserver < delta.getRowCount()) {
			// Validate that this update does not contain any row level conflicts.
			checkForRowLevelConflict(tableId, delta);
		}
		// Now assign the rowIds and set the version number
		TableModelUtils.assignRowIdsAndVersionNumbers(delta, range);
		
		final Set<Long> fileIdsInSet = delta.getFileHandleIdsInSparseChangeSet();
		
 		final Set<Long> newFileIds = getFileHandleIdsNotAssociatedWithTable(tableId, fileIdsInSet);
 		
 		final Long userId = user.getId();

		List<FileEvent> uploadFileEvents = newFileIds.stream().map(fileHandleId ->
						FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, userId,
								fileHandleId.toString(), tableId, FileHandleAssociateType.TableEntity, config.getStack(), config.getStackInstance()))
				.collect(Collectors.toList());

		uploadFileEvents.forEach(messenger::publishMessageAfterCommit);

		final boolean hasFileRefs = !newFileIds.isEmpty();
		
		tableRowTruthDao.appendRowSetToTable(userId.toString(), tableId, range.getEtag(), range.getVersionNumber(), columns, delta.writeToDto(), txContext.getTransactionId(), hasFileRefs);
		
		// Prepare the results
		RowReferenceSet results = new RowReferenceSet();
		results.setHeaders(TableModelUtils.getSelectColumns(columns));
		results.setTableId(tableId);
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
	 * From the given set of file handle ids computes the subset of ids that are NOT associated with the given table
	 * 
	 * @param tableId The id of the table
	 * @param fileHandleIds A set of file handle ids
	 * @return The subset of file handle ids from the given input set that are not associated with the given table
	 */
	Set<Long> getFileHandleIdsNotAssociatedWithTable(String tableId, Set<Long> fileHandleIds) {
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		
		// First get the set of file handles among the input that are actually associated with the table
		Set<Long> filesAssociatedWithTable = indexDao.getFileHandleIdsAssociatedWithTable(fileHandleIds, idAndVersion);
		
		// Now compute the set difference
		return fileHandleIds.stream().filter( id -> !filesAssociatedWithTable.contains(id)).collect(Collectors.toSet());
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
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(idAndVersion);
		tableManagerSupport.validateTableReadAccess(userInfo, indexDescription);
		if (!TableType.table.equals(indexDescription.getTableType())) {
			throw new UnauthorizedException("Can only be called for TableEntities");
		}
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		String sql = SQLUtils.buildSelectRowIds(tableId, rows, columns);

		final Map<Long, Row> rowMap = new HashMap<Long, Row>(rows.size());
		QueryTranslator query = QueryTranslator.builder(sql, tableManagerSupport, userInfo.getId())
				.indexDescription(indexDescription).build();
		indexDao.queryAsStream(null, query, new RowHandler() {
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
		for (RowReference ref : rows) {
			Row row = rowMap.get(ref.getRowId());
			if (row != null) {
				resultRows.add(row);
			}
		}
		return results;

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
	void validateFileHandles(UserInfo user, String tableId,
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
	public void tableUpdated(final UserInfo userInfo, final List<String> newSchema, final String tableId, Boolean searchEnabled) {
		try {
			IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
			SynchronizedProgressCallback callback = new SynchronizedProgressCallback(EXCLUSIVE_LOCK_TIMEOUT_SECONDS);
			tableManagerSupport.tryRunWithTableExclusiveLock(callback, new LockContext(ContextType.TableUpdate, idAndVersion), idAndVersion,
					(ProgressCallback callbackInner) -> {
						tableUpdatedWithExclusiveLock(callbackInner, userInfo, newSchema, tableId, searchEnabled);
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
	void tableUpdatedWithExclusiveLock(final ProgressCallback callback, final UserInfo userInfo, final List<String> newSchema, final String tableId, Boolean searchEnabled) {
		// Lookup the current schema for this table
		List<String> oldSchema = columModelManager.getColumnIdsForTable(IdAndVersion.parse(tableId));
		// Calculate the schema change (if there is one).
		List<ColumnChange> schemaChange = TableModelUtils.createChangesFromOldSchemaToNew(oldSchema, newSchema);
		TableSchemaChangeRequest changeRequest = new TableSchemaChangeRequest();
		changeRequest.setChanges(schemaChange);
		changeRequest.setEntityId(tableId);
		changeRequest.setOrderedColumnIds(newSchema);
		
		transactionManager.executeInTransaction(userInfo, tableId, txContext -> {
			// Will add a table schema change if needed 
			updateTableSchema(userInfo, changeRequest, txContext);
			
			// Will add a search change id needed
			updateSearchStatus(userInfo, tableId, searchEnabled, txContext);
			
			return null;
		});

	}
		
	/**
	 * Will add a search change to the given table and transaction if the search status changed
	 * 
	 * @param userInfo
	 * @param tableId
	 * @param searchEnabled
	 * @return True if the search status was changed, false otherwise
	 */
	boolean updateSearchStatus(UserInfo userInfo, String tableId, Boolean searchEnabled, TableTransactionContext txContext) {
		// No change needed if the flag was not specified
		if (searchEnabled == null) {
			return false;
		}
		// At this point only the truth knows about the search status since the table has been created/updated already and the table might not have been built yet
		TableRowChange lastSearchChange = tableRowTruthDao.getLastTableRowChange(tableId, TableChangeType.SEARCH);
		
		// No change to the search status 
		if (lastSearchChange == null && !searchEnabled) {
			return false;
		}
		
		// The search status is already up to date
		if (lastSearchChange != null && searchEnabled.equals(lastSearchChange.getIsSearchEnabled())) {
			return false;
		}
		
		tableRowTruthDao.appendSearchChange(userInfo.getId(), tableId, txContext.getTransactionId(), searchEnabled);
		
		return true;
		
	}
	
	/**
	 * Updates the search status of a table through a search change request, makes sure to also update the table entity property
	 * 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @param transactionId
	 * @return
	 */
	TableUpdateResponse updateSearchStatus(UserInfo userInfo, TableSearchChangeRequest change, TableTransactionContext txContext) {
		boolean statusChanged = updateSearchStatus(userInfo, change.getEntityId(), change.getSearchEnabled(), txContext);
		
		if (statusChanged) {
			// Make sure to align the searchEnabled property in the node representing the table
			Node tableNode = nodeManager.getNode(userInfo, change.getEntityId());
			
			tableNode.setIsSearchEnabled(change.getSearchEnabled());
			
			nodeManager.update(userInfo, tableNode, null, false);
		}
		
		return new TableSearchChangeResponse().setSearchEnabled(change.getSearchEnabled());
	}
	
	@Override
	public boolean isTemporaryTableNeededToValidate(TableUpdateRequest change) {
		if (change instanceof TableSchemaChangeRequest) {
			TableSchemaChangeRequest schemaChange = (TableSchemaChangeRequest) change;
			// If one or more of the existing columns will change then a temporary table is needed to validate
			// the change.
			return containsColumnUpdate(schemaChange.getChanges());
		} else if (change instanceof UploadToTableRequest) {
			// might switch to true to support uniqueness constraints.
			return false;
		} else if (change instanceof AppendableRowSetRequest) {
			return false;
		} else if (change instanceof TableSearchChangeRequest) {
			return false;
		} else {
			throw new IllegalArgumentException("Unknown change type: " + change.getClass().getName());
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
	public void validateUpdateRequest(ProgressCallback callback, UserInfo userInfo, TableUpdateRequest change,
			TableIndexManager indexManager) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(change, "change");
		if (change instanceof TableSchemaChangeRequest) {
			validateSchemaUpdateRequest(callback, userInfo, (TableSchemaChangeRequest) change, indexManager);
		} else if (change instanceof UploadToTableRequest) {
			// nothing to validate
		} else if (change instanceof AppendableRowSetRequest) {
			// nothing to validate
		} else if (change instanceof TableSearchChangeRequest) {
			ValidateArgument.required(((TableSearchChangeRequest) change).getSearchEnabled(), "The searchEnabled value");
		} else {
			throw new IllegalArgumentException("Unknown request type: " + change.getClass().getName());
		}

	}

	/**
	 * Validation for a schema change request. Note that this can be invoked only in the context of a background worker since
	 * a schema change might lead to an expensive validation done on a temporary table. 
	 * 
	 * This is relevant only when an existing column is updated (E.g. when the type of the column is modified).
	 * 
	 * This type of validation cannot be invoked (nor is relevant) when the change to the schema is performed through 
	 * an entity update (e.g. if the list of column ids is updated through the entity update, no update to existing columns can
	 * be done in that case).
	 * 
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
			indexManager.alterTempTableSchema(idAndVersion, details);
		}
	}


	@Override
	public TableUpdateResponse updateTable(ProgressCallback callback, UserInfo userInfo, TableUpdateRequest change, TableTransactionContext txContext) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(change, "change");
		if (change instanceof TableSchemaChangeRequest) {
			return updateTableSchema(userInfo, (TableSchemaChangeRequest) change, txContext);
		} else if (change instanceof UploadToTableRequest) {
			return uploadToTable(callback, userInfo, (UploadToTableRequest) change, txContext);
		} else if (change instanceof AppendableRowSetRequest) {
			return appendToTable(userInfo, (AppendableRowSetRequest) change, txContext);
		} else if (change instanceof TableSearchChangeRequest) {
			return updateSearchStatus(userInfo, (TableSearchChangeRequest) change, txContext);
		} else {
			throw new IllegalArgumentException("Unknown request type: " + change.getClass().getName());
		}
	}
	
	/**
	 * Append a rowset to a table from a transaction.
	 * @param userInfo
	 * @param request
	 * @return
	 */
	TableUpdateResponse appendToTable(UserInfo userInfo, AppendableRowSetRequest request, TableTransactionContext txContext) {
		ValidateArgument.required(request.getToAppend(), "AppendableRowSetRequest.toAppend");
		try {
			RowReferenceSet results = null;
			if(request.getToAppend() instanceof PartialRowSet){
				PartialRowSet partialRowSet = (PartialRowSet) request.getToAppend();
				results =  appendPartialRows(userInfo, partialRowSet.getTableId(), partialRowSet, txContext);
			}else if(request.getToAppend() instanceof RowSet){
				RowSet rowSet = (RowSet)request.getToAppend();
				results = appendRows(userInfo, rowSet.getTableId(), rowSet, txContext);
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
	TableUpdateResponse uploadToTable(ProgressCallback callback, UserInfo userInfo, UploadToTableRequest change, TableTransactionContext txContext) {
		// upload the CSV to the table.
		return tableUploadManager.uploadCSV(callback, userInfo, change, (user, tableId, tableSchema, rowStream, updateEtag, progressCallback) -> 
			appendRowsAsStream(user, tableId, tableSchema, rowStream, updateEtag, null, txContext)
		);
	}


	/**
	 * Apply a validated schema change request.
	 * 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	TableSchemaChangeResponse updateTableSchema(UserInfo userInfo, TableSchemaChangeRequest changes, TableTransactionContext txContext) {
		IdAndVersion idAndVersion = IdAndVersion.parse(changes.getEntityId());
		// First determine if this will be an actual change to the schema.
		List<String> newSchemaIds = columModelManager.calculateNewSchemaIdsAndValidate(changes.getEntityId(), changes.getChanges(), changes.getOrderedColumnIds());
		List<String> currentSchemaIds = columModelManager.getColumnIdsForTable(idAndVersion);
		List<ColumnModel> newSchema = null;
		if (!currentSchemaIds.equals(newSchemaIds)) {
			// This will 
			newSchema = applySchemaChangeToTable(userInfo, changes.getEntityId(), newSchemaIds, changes.getChanges(), txContext);
		}else {
			// The schema will not change so return the current schema.
			newSchema = columModelManager.getTableSchema(idAndVersion);
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
	List<ColumnModel> applySchemaChangeToTable(UserInfo userInfo, String tableId, List<String> newSchemaIds, List<ColumnChange> changes, TableTransactionContext txContext) {
		List<ColumnModel> newSchema = columModelManager.bindColumnsToDefaultVersionOfObject(newSchemaIds, tableId);
		tableRowTruthDao.appendSchemaChangeToTable("" + userInfo.getId(), tableId, newSchemaIds, changes, txContext.getTransactionId());
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
		return columModelManager.getColumnIdsForTable(inputIdAndVersion);
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
			case SEARCH:
				tableChange = new SearchChange(wrapped.getIsSearchEnabled());
				break;
			default:
				throw new IllegalStateException("Unknown type: " + wrapped.getChangeType());
			}
			return new ChangeData<>(wrapped.getRowVersion(), clazz.cast(tableChange));
		}

	}
	
	@Override
	public Optional<Long> getTransactionForVersion(String tableId, long version) {
		return tableTransactionDao.getTransactionForVersion(tableId, version);
	}


	@WriteTransaction
	@Override
	public SnapshotResponse createTableSnapshot(UserInfo userInfo, String tableId, SnapshotRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(request, "request");
		
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		
		ObjectType type = tableManagerSupport.getTableObjectType(idAndVersion);
		
		if (ObjectType.ENTITY_VIEW.equals(type)) {
			throw new IllegalArgumentException("EntityView snapshots can only be created via an asynchronous table transaction job.");
		}
		
		// Validate the user has permission to edit the table
		tableManagerSupport.validateTableWriteAccess(userInfo, idAndVersion);
		
		// create a new version
		long snapshotVersion = nodeManager.createSnapshotAndVersion(userInfo, tableId, request);
		
		IdAndVersion snapshotIdAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).setVersion(snapshotVersion).build(); 
		
		// bind the current schema to the version
		columModelManager.bindCurrentColumnsToVersion(snapshotIdAndVersion);
		
		transactionManager.linkVersionToLatestTransaction(snapshotIdAndVersion);
		
		return new SnapshotResponse().setSnapshotVersionNumber(snapshotVersion);
	}
	
	@Override
	public org.sagebionetworks.repo.model.IdRange getTableRowChangeIdRange() {
		return tableRowTruthDao.getTableRowChangeIdRange();
	}

	@Override
	public Iterator<TableRowChange> newTableRowChangeWithFileRefsIterator(org.sagebionetworks.repo.model.IdRange idRange) {
		ValidateArgument.required(idRange, "The idRange");
		ValidateArgument.requirement(idRange.getMinId() <= idRange.getMaxId(), "Invalid idRange, the minId must be lesser or equal than the maxId");
		return new PaginationIterator<TableRowChange>((long limit, long offset) -> tableRowTruthDao.getTableRowChangeWithFileRefsPage(idRange, limit, offset), PAGE_SIZE_LIMIT);
	}


	@Override
	public void storeTableSnapshot(IdAndVersion tableId, ProgressCallback progressCallback) throws Exception {
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.requirement(tableId.getVersion().isPresent(), "The tableId.version is required.");
		
		// We acquire an exclusive lock on the specific table snapshot so that we make sure no other worker is doing the same
		// We use a key specific to this streaming operation + tableId not to interfere with other readers
		String exclusiveLockKey = TableModelUtils.getTableSnapshotStreamingSempahoreKey(tableId);
		
		tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, new LockContext(ContextType.TableSnapshot, tableId),  exclusiveLockKey, (innerCallback) -> {
			
			TableType tableType = tableManagerSupport.getTableType(tableId);
			
			ValidateArgument.requirement(TableType.table.equals(tableType), "Unexpected table type for " + tableId + " (Was " + tableType + ").");
			
			// The snapshot is already saved, nothing to do
			if (tableSnapshotDao.getSnapshot(tableId).isPresent()) {
				return null;
			}
			
			TableState tableState = tableManagerSupport.getTableStatusState(tableId)
				.orElseThrow(() -> new IllegalStateException("The table " + tableId + " status does not exist."));
			
			if (!TableState.AVAILABLE.equals(tableState)) {
				throw new IllegalStateException("The table " +tableId + " is not available.");
			}
			
			String bucket = config.getTableSnapshotBucketName();
			String key = tableId + "/" + UUID.randomUUID().toString() + ".csv.gzip";
			
			tableManagerSupport.streamTableIndexToS3(tableId, bucket, key);
			
			tableSnapshotDao.createSnapshot(new TableSnapshot()
				.withTableId(tableId.getId())
				.withVersion(tableId.getVersion().get())
				.withCreatedOn(new Date())
				.withCreatedBy(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())
				.withBucket(bucket)
				.withKey(key)
			);
			return null;
		});
		
		
	}

}
