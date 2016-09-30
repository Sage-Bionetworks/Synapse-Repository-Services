package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TableEntityManagerImpl implements TableEntityManager {
	
	private static final int EXCLUSIVE_LOCK_TIMEOUT_MS = 5*1000;

	private static final String PARTIAL_ROW_KEY_NOT_A_VALID = "PartialRow.value.key: '%s' is not a valid column ID for row ID: %s";

	static private Log log = LogFactory.getLog(TableEntityManagerImpl.class);

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


	@WriteTransactionReadCommitted
	@Override
	public RowReferenceSet appendRows(UserInfo user, String tableId, List<ColumnModel> columns, RowSet delta, ProgressCallback<Long> progressCallback)
			throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(columns, "columns");
		ValidateArgument.required(delta, "RowSet");
		// Validate the request is under the max bytes per requested
		validateRequestSize(columns, delta.getRows().size());
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		appendRowsAsStream(user, tableId, columns, delta.getRows().iterator(), delta.getEtag(), results, progressCallback);
		return results;
	}
	
	@WriteTransactionReadCommitted
	@Override
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId, List<ColumnModel> columns,
			PartialRowSet rowsToAppendOrUpdateOrDelete, ProgressCallback<Long> progressCallback)
			throws DatastoreException, NotFoundException, IOException {
		Validate.required(user, "User");
		Validate.required(tableId, "TableId");
		Validate.required(columns, "columns");
		Validate.required(rowsToAppendOrUpdateOrDelete, "RowsToAppendOrUpdate");
		// Validate the request is under the max bytes per requested
		validateRequestSize(columns, rowsToAppendOrUpdateOrDelete.getRows().size());
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		RowSet fullRowsToAppendOrUpdateOrDelete = mergeWithLastVersion(tableId, rowsToAppendOrUpdateOrDelete, columns);
		appendRowsAsStream(user, tableId, columns, fullRowsToAppendOrUpdateOrDelete.getRows().iterator(),
				fullRowsToAppendOrUpdateOrDelete.getEtag(), results, progressCallback);
		return results;
	}

	/**
	 * This method merges the partial row with the most current version of the row as it exists on S3. For updates, this
	 * will find the most current version, and any column not present as a key in the map will be replaced with the most
	 * recent value. For inserts, this will replace null cell values with their defaults.
	 */
	private RowSet mergeWithLastVersion(String tableId, PartialRowSet rowsToAppendOrUpdateOrDelete, List<ColumnModel> columns)
			throws IOException,
			NotFoundException {
		RowSet result = new RowSet();
		TableRowChange lastTableRowChange = tableRowTruthDao.getLastTableRowChange(tableId, TableChangeType.ROW);
		result.setEtag(lastTableRowChange == null ? null : lastTableRowChange.getEtag());
		result.setHeaders(TableModelUtils.getSelectColumns(columns));
		result.setTableId(tableId);
		List<Row> rows = Lists.newArrayListWithCapacity(rowsToAppendOrUpdateOrDelete.getRows().size());
		Set<Long> columnIdSet = Transform.toSet(result.getHeaders(), TableModelUtils.SELECT_COLUMN_TO_ID);
		Map<Long, Pair<PartialRow, Row>> rowsToUpdate = Maps.newHashMap();
		for (PartialRow partialRow : rowsToAppendOrUpdateOrDelete.getRows()) {
			validatePartialRow(partialRow, columnIdSet);
			Row row;
			if (partialRow.getRowId() != null) {
				row = new Row();
				row.setRowId(partialRow.getRowId());
				rowsToUpdate.put(partialRow.getRowId(), Pair.create(partialRow, row));
			} else {
				row = resolveInsertValues(partialRow, columns);
			}
			rows.add(row);
		}
		resolveUpdateValues(tableId, rowsToUpdate, columns);
		result.setRows(rows);
		return result;
	}
	
	/**
	 * Validate the PartialRow matches the headers.
	 * 
	 * @param row
	 * @param headers
	 */
	public static void validatePartialRow(PartialRow row, Set<Long> columnIds){
		if(row == null){
			throw new IllegalArgumentException("PartialRow cannot be null");
		}
		if(columnIds == null){
			throw new IllegalArgumentException("Set<Long> columnIds cannot be null");
		}
		if(row != null){
			if(row.getValues() != null){
				for(String key: row.getValues().keySet()){
					try {
						Long columnId = Long.parseLong(key);
						if(!columnIds.contains(columnId)){
							throw new IllegalArgumentException(String.format(PARTIAL_ROW_KEY_NOT_A_VALID, key, row.getRowId()));
						}
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(String.format(PARTIAL_ROW_KEY_NOT_A_VALID, key, row.getRowId()));
					}
				}
			}
		}
	}

	private static Row resolveInsertValues(PartialRow partialRow, List<ColumnModel> columns) {
		List<String> values = Lists.newArrayListWithCapacity(columns.size());
		for (ColumnModel model : columns) {
			String value = null;
			if (model != null) {
				value = partialRow.getValues().get(model.getId());
				if (value == null) {
					value = model.getDefaultValue();
				}
			}
			values.add(value);
		}
		Row row = new Row();
		row.setValues(values);
		return row;
	}

	private void resolveUpdateValues(String tableId, Map<Long, Pair<PartialRow, Row>> rowsToUpdate, List<ColumnModel> columns)
			throws IOException, NotFoundException {
		RowSetAccessor currentRowData = tableRowTruthDao.getLatestVersionsWithRowData(tableId, rowsToUpdate.keySet(), 0L, columns);
		for (Map.Entry<Long, Pair<PartialRow, Row>> rowToUpdate : rowsToUpdate.entrySet()) {
			Long rowId = rowToUpdate.getKey();
			PartialRow partialRow = rowToUpdate.getValue().getFirst();
			Row row = rowToUpdate.getValue().getSecond();
			RowAccessor currentRow = currentRowData.getRow(rowId);
			row.setVersionNumber(currentRow.getVersionNumber());
			if(partialRow.getValues() == null){
				row.setValues(null);
			}else{
				List<String> values = Lists.newArrayListWithCapacity(columns.size());
				for (ColumnModel model : columns) {
					String value = null;
					if (model!= null) {
						if (partialRow.getValues().containsKey(model.getId())) {
							value = partialRow.getValues().get(model.getId());
						} else {
							value = currentRow.getCellById(model.getId());
						}
						if (value == null) {
							value = model.getDefaultValue();
						}
					}
					values.add(value);
				}
				row.setValues(values);
			}
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public RowReferenceSet deleteRows(UserInfo user, String tableId, RowSelection rowsToDelete) throws DatastoreException, NotFoundException,
			IOException {
		Validate.required(user, "user");
		Validate.required(tableId, "tableId");
		Validate.required(rowsToDelete, "rowsToDelete");

		// Validate the user has permission to edit the table
		tableManagerSupport.validateTableWriteAccess(user, tableId);

		// create a rowset of all deletes
		List<Row> rows = Transform.toList(rowsToDelete.getRowIds(), new Function<Long, Row>() {
			@Override
			public Row apply(Long input) {
				Row row = new Row();
				row.setRowId(input);
				row.setVersionNumber(null);
				row.setValues(null);
				return row;
			}
		});
		List<ColumnModel> columns = tableManagerSupport.getColumnModelsForTable(tableId);
		RawRowSet rowSetToDelete = new RawRowSet(TableModelUtils.getIds(columns), rowsToDelete.getEtag(), tableId, rows);
		RowReferenceSet result = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, columns, rowSetToDelete);
		// The table has change so we must reset the state.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(tableId);
		return result;
	}

	@WriteTransactionReadCommitted
	@Override
	public void deleteAllRows(String tableId) {
		Validate.required(tableId, "tableId");
		tableRowTruthDao.deleteAllRowDataForTable(tableId);
	}

	@WriteTransactionReadCommitted
	@Override
	public String appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns, Iterator<Row> rowStream, String etag,
			RowReferenceSet results, ProgressCallback<Long> progressCallback) throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(columns, "columns");
		validateFeatureEnabled();

		// Validate the user has permission to edit the table
		tableManagerSupport.validateTableWriteAccess(user, tableId);

		// To prevent race conditions on concurrency checking we apply all changes to a single table
		// serially by locking on the table's Id.
		tableManagerSupport.lockOnTableId(tableId);
		
		List<String> ids = TableModelUtils.getIds(columns);
		List<Row> batch = new LinkedList<Row>();
		int batchSizeBytes = 0;
		int count = 0;
		RawRowSet delta = new RawRowSet(ids, etag, tableId, batch);
		while(rowStream.hasNext()){
			Row row = rowStream.next();
			batch.add(row);
			// batch using the actual size of the row.
			batchSizeBytes += TableModelUtils.calculateActualRowSize(row);
			if(batchSizeBytes >= maxBytesPerChangeSet){
				// Validate there aren't any illegal file handle replaces
				validateFileHandles(user, tableId, columns, delta.getRows());
				// Send this batch and keep the etag.
				etag = appendBatchOfRowsToTable(user, columns, delta, results, progressCallback);
				// Clear the batch
				count += batch.size();
				batch.clear();
				batchSizeBytes = 0;
				if(log.isTraceEnabled()){
					log.trace("Appended: "+count+" rows to table: "+tableId);
				}
			}
		}
		// Send the last batch is there are any rows
		if(!batch.isEmpty()){
			// Validate there aren't any illegal file handle replaces
			validateFileHandles(user, tableId, columns, delta.getRows());
			etag = appendBatchOfRowsToTable(user, columns, delta, results, progressCallback);
		}
		// The table has change so we must reset the state.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(tableId);
		return etag;
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
	private String appendBatchOfRowsToTable(UserInfo user, List<ColumnModel> columns, RawRowSet delta, RowReferenceSet results,
			ProgressCallback<Long> progressCallback)
			throws IOException, ReadOnlyException {
		// See PLFM-3041
		checkStackWiteStatus();
		RowReferenceSet rrs = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), delta.getTableId(), columns, delta);
		if(progressCallback != null){
			progressCallback.progressMade(new Long(rrs.getRows().size()));
		}
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
	
	@Override
	public List<TableRowChange> listRowSetsKeysForTable(String tableId) {
		return tableRowTruthDao.listRowSetsKeysForTable(tableId);
	}

	@Override
	public RowSet getRowSet(String tableId, Long rowVersion, List<ColumnModel> columns) throws IOException,
			NotFoundException {
		return tableRowTruthDao.getRowSet(tableId, rowVersion, columns);
	}

	@Override
	public TableRowChange getLastTableRowChange(String tableId) throws IOException, NotFoundException {
		return tableRowTruthDao.getLastTableRowChange(tableId);
	}

	@Override
	public long getMaxRowId(String tableId) throws IOException, NotFoundException {
		return tableRowTruthDao.getMaxRowId(tableId);
	}

	@Override
	public String getCellValue(UserInfo userInfo, String tableId, RowReference rowRef, ColumnModel column) throws IOException,
			NotFoundException {
		tableManagerSupport.validateTableReadAccess(userInfo, tableId);
		Row row = tableRowTruthDao.getRowOriginal(tableId, rowRef, Lists.newArrayList(column));
		return row.getValues().get(0);
	}

	@Override
	public RowSet getCellValues(UserInfo userInfo, String tableId, RowReferenceSet rowRefs, List<ColumnModel> columns)
			throws IOException, NotFoundException {
		tableManagerSupport.validateTableReadAccess(userInfo, tableId);
		return tableRowTruthDao.getRowSet(rowRefs, columns);
	}


	
	private void validateRequestSize(List<ColumnModel> columns, int rowCount) {
		// Validate the request is under the max bytes per requested
		if (!TableModelUtils.isRequestWithinMaxBytePerRequest(columns, rowCount, this.maxBytesPerRequest)) {
			throw new IllegalArgumentException("Request exceed the maximum number of bytes per request.  Maximum : "+this.maxBytesPerRequest+" bytes");
		}
	}

	public void setMaxBytesPerRequest(int maxBytesPerRequest) {
		this.maxBytesPerRequest = maxBytesPerRequest;
	}

	/**
	 * Can the user download all FileHandles in the passed set of rows.
	 * @param user
	 * @param tableId
	 * @param columnMapper
	 * @param rows
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public void validateFileHandles(UserInfo user, String tableId, List<ColumnModel> columns, List<Row> rows)
			throws IOException,
			NotFoundException {
		
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(columns));
		validateFileHandles(user, tableId, rowSet);
	}

	/**
	 * 
	 * @param user
	 * @param tableId
	 * @param rowSet
	 */
	public void validateFileHandles(UserInfo user, String tableId,
			RowSet rowSet) {
		if(user.isAdmin()){
			return;
		}
		// Extract the files handles from the change set.
		Set<Long> filesHandleIds = TableModelUtils.getFileHandleIdsInRowSet(rowSet);
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
				TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
				// Get the sub-set of files associated with the table.
				Set<Long> filesAssociatedWithTable = indexDao.getFileHandleIdsAssociatedWithTable(new HashSet<Long>(remainingFilesToCheck), tableId);
				// remove all files associated with the table
				remainingFilesToCheck.removeAll(filesAssociatedWithTable);
				// Any files remaining in the set are not created by the user and are not associated with the table.
				if(!remainingFilesToCheck.isEmpty()){
					throw new UnauthorizedException("Cannot access files: "+remainingFilesToCheck.toString());
				}
			}
		}
	}
	
	/**
	 * Throws an exception if the table feature is disabled.
	 */
	public void validateFeatureEnabled(){
		if(!StackConfiguration.singleton().getTableEnabled()){
			throw new IllegalStateException("This method cannot be called when the table feature is disabled.");
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
		try {
			return tableManagerSupport.tryRunWithTableNonexclusiveLock(null, tableId, READ_LOCK_TIMEOUT_SEC, new ProgressingCallable<Set<Long>, Void>(){

				@Override
				public Set<Long> call(ProgressCallback<Void> callback) throws Exception {
					// What is the current version of the talbe's truth?
					TableRowChange lastChange = tableRowTruthDao.getLastTableRowChange(tableId);
					callback.progressMade(null);
					if(lastChange == null){
						// There are no changes applied to this table so return an empty set.
						return Sets.newHashSet();
					}
					long truthVersion = lastChange.getRowVersion();
					// Next connect to the table
					TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
					if(indexDao == null){
						throw new TemporarilyUnavailableException("Cannot connect to table index at this time.");
					}
					long indexVersion = indexDao.getMaxCurrentCompleteVersionForTable(tableId);
					if(indexVersion < truthVersion){
						throw new TemporarilyUnavailableException("Waiting for the table index to be built.");
					}
					callback.progressMade(null);
					// the index dao 
					return indexDao.getFileHandleIdsAssociatedWithTable(toTest, tableId);
				}});
		} catch (LockUnavilableException e) {
			throw new TemporarilyUnavailableException(e);
		} catch (TemporarilyUnavailableException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void setTableSchema(final UserInfo userInfo, final List<String> columnIds,
			final String id) {
		try {
			tableManagerSupport.tryRunWithTableExclusiveLock(null, id, EXCLUSIVE_LOCK_TIMEOUT_MS, new ProgressingCallable<Void, Void>() {

				@Override
				public Void call(ProgressCallback<Void> callback) throws Exception {
					columModelManager.bindColumnToObject(userInfo, columnIds, id);
					tableManagerSupport.setTableToProcessingAndTriggerUpdate(id);
					return null;
				}
			});
		}catch (LockUnavilableException e) {
			throw new TemporarilyUnavailableException("Cannot update an unavailable table");
		}catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void deleteTable(String deletedId) {
		columModelManager.unbindAllColumnsAndOwnerFromObject(deletedId);
		deleteAllRows(deletedId);
		tableManagerSupport.setTableDeleted(deletedId, ObjectType.TABLE);
	}


	@Override
	public boolean isTemporaryTableNeededToValidate(TableUpdateRequest change) {
		if(change instanceof TableSchemaChangeRequest){
			TableSchemaChangeRequest schemaChange = (TableSchemaChangeRequest) change;
			return containsColumnUpdate(schemaChange.getChanges());
		}else{
			throw new IllegalArgumentException("Unknown change type: "+change.getClass().getName());
		}
	}
	
	/**
	 * Is a Temporary table needed to validate the passed set of changes.
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
	public void validateUpdateRequest(ProgressCallback<Void> callback,
			UserInfo userInfo, TableUpdateRequest change,
			TableIndexManager indexManager) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(change, "change");
		if(change instanceof TableSchemaChangeRequest){
			validateSchemaUpdateRequest(callback, userInfo, (TableSchemaChangeRequest)change, indexManager);
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
	public void validateSchemaUpdateRequest(ProgressCallback<Void> callback,
			UserInfo userInfo, TableSchemaChangeRequest changes,
			TableIndexManager indexManager) {
		// first determine what the new Schema will be
		columModelManager.calculateNewSchemaIdsAndValidate(changes.getEntityId(), changes.getChanges());
		// If the change includes an update then the schema change must be checked against the temp table.
		boolean includesUpdate = containsColumnUpdate(changes.getChanges());
		if(includesUpdate){
			if(indexManager == null){
				throw new IllegalStateException("A temporary table is needed to validate but was not provided.");
			}
			List<ColumnChangeDetails> details = columModelManager.getColumnChangeDetails(changes.getChanges());
			// attempt to apply the schema change to the temp copy of the table.
			indexManager.alterTempTableSchmea(callback, changes.getEntityId(), details);
		}
	}


	@Override
	public TableUpdateResponse updateTable(ProgressCallback<Void> callback,
			UserInfo userInfo, TableUpdateRequest change) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(change, "change");
		if(change instanceof TableSchemaChangeRequest){
			return updateTableSchema(callback, userInfo, (TableSchemaChangeRequest)change);
		}else{
			throw new IllegalArgumentException("Unknown request type: "+change.getClass().getName());
		}
	}
	
	/**
	 * Apply a validated schema change request.
	 * 
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	public TableSchemaChangeResponse updateTableSchema(ProgressCallback<Void> callback,
			UserInfo userInfo, TableSchemaChangeRequest changes) {

		// first determine what the new Schema will be
		List<String> newSchemaIds = columModelManager.calculateNewSchemaIdsAndValidate(changes.getEntityId(), changes.getChanges());
		columModelManager.bindColumnToObject(userInfo, newSchemaIds, changes.getEntityId());
		boolean keepOrder = true;
		List<ColumnModel> newSchema = columModelManager.getColumnModel(userInfo, newSchemaIds, keepOrder);
		// If the change includes an update then a change needs to be pushed to the changes
		if(containsColumnUpdate(changes.getChanges())){
			List<String> newSchemaIdsLong = TableModelUtils.getIds(newSchema);
			try {
				this.tableRowTruthDao.appendSchemaChangeToTable(""+userInfo.getId(), changes.getEntityId(), newSchemaIdsLong, changes.getChanges());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// trigger an update.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(changes.getEntityId());
		TableSchemaChangeResponse response = new TableSchemaChangeResponse();
		response.setSchema(newSchema);
		return response;
	}


	@Override
	public List<ColumnChangeDetails> getSchemaChangeForVersion(String tableId,
			long versionNumber) throws IOException {
		List<ColumnChange> changes = tableRowTruthDao.getSchemaChangeForVersion(tableId, versionNumber);
		return columModelManager.getColumnChangeDetails(changes);
	}


	@Override
	public List<String> getTableSchema(UserInfo user, String id) {
		return columModelManager.getColumnIdForTable(id);
	}

}
