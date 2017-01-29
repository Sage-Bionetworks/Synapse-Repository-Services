package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.MysqlFunction;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SqlDirective;
import org.sagebionetworks.table.query.model.visitors.GetTableNameVisitor;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.Closer;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;

public class TableRowManagerImpl implements TableRowManager {
	
	private static final String PARTIAL_ROW_KEY_NOT_A_VALID = "PartialRow.value.key: '%s' is not a valid column ID for row ID: %s";

	static private Log log = LogFactory.getLog(TableRowManagerImpl.class);
	
	public static final long BUNDLE_MASK_QUERY_RESULTS = 0x1;
	public static final long BUNDLE_MASK_QUERY_COUNT = 0x2;
	public static final long BUNDLE_MASK_QUERY_SELECT_COLUMNS = 0x4;
	public static final long BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE = 0x8;
	public static final long BUNDLE_MASK_QUERY_COLUMN_MODELS = 0x10;

	public static final int READ_LOCK_TIMEOUT_SEC = 60;
	
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	TableRowTruthDAO tableRowTruthDao;
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	ColumnModelDAO columnModelDAO;
	@Autowired
	WriteReadSemaphoreRunner writeReadSemaphoreRunner;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	StackStatusDao stackStatusDao;
	
	
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
	public RowReferenceSet appendRows(UserInfo user, String tableId, ColumnMapper columnMapper, RowSet delta, ProgressCallback<Long> progressCallback)
			throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(columnMapper, "columnMapper");
		ValidateArgument.required(delta, "RowSet");
		// Validate the request is under the max bytes per requested
		validateRequestSize(columnMapper, delta.getRows().size());
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		appendRowsAsStream(user, tableId, columnMapper, delta.getRows().iterator(), delta.getEtag(), results, progressCallback);
		return results;
	}
	
	@Override
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId, ColumnMapper columnMapper,
			PartialRowSet rowsToAppendOrUpdateOrDelete, ProgressCallback<Long> progressCallback)
			throws DatastoreException, NotFoundException, IOException {
		Validate.required(user, "User");
		Validate.required(tableId, "TableId");
		Validate.required(columnMapper, "columnMapper");
		Validate.required(rowsToAppendOrUpdateOrDelete, "RowsToAppendOrUpdate");
		// Validate the request is under the max bytes per requested
		validateRequestSize(columnMapper, rowsToAppendOrUpdateOrDelete.getRows().size());
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		RowSet fullRowsToAppendOrUpdateOrDelete = mergeWithLastVersion(tableId, rowsToAppendOrUpdateOrDelete, columnMapper);
		appendRowsAsStream(user, tableId, columnMapper, fullRowsToAppendOrUpdateOrDelete.getRows().iterator(),
				fullRowsToAppendOrUpdateOrDelete.getEtag(), results, progressCallback);
		return results;
	}

	/**
	 * This method merges the partial row with the most current version of the row as it exists on S3. For updates, this
	 * will find the most current version, and any column not present as a key in the map will be replaced with the most
	 * recent value. For inserts, this will replace null cell values with their defaults.
	 */
	private RowSet mergeWithLastVersion(String tableId, PartialRowSet rowsToAppendOrUpdateOrDelete, ColumnMapper columnMapper)
			throws IOException,
			NotFoundException {
		RowSet result = new RowSet();
		TableRowChange lastTableRowChange = tableRowTruthDao.getLastTableRowChange(tableId);
		result.setEtag(lastTableRowChange == null ? null : lastTableRowChange.getEtag());
		result.setHeaders(columnMapper.getSelectColumns());
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
				if (partialRow.getValues() == null) {
					// no values specified, this is a row deletion
				} else {
					rowsToUpdate.put(partialRow.getRowId(), Pair.create(partialRow, row));
				}
			} else {
				row = resolveInsertValues(partialRow, columnMapper);
			}
			rows.add(row);
		}
		resolveUpdateValues(tableId, rowsToUpdate, columnMapper);
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

	private static Row resolveInsertValues(PartialRow partialRow, ColumnMapper columnMapper) {
		List<String> values = Lists.newArrayListWithCapacity(columnMapper.selectColumnCount());
		for (SelectColumnAndModel model : columnMapper.getSelectColumnAndModels()) {
			String value = null;
			if (model.getColumnModel() != null) {
				value = partialRow.getValues().get(model.getColumnModel().getId());
				if (value == null) {
					value = model.getColumnModel().getDefaultValue();
				}
			}
			values.add(value);
		}
		Row row = new Row();
		row.setValues(values);
		return row;
	}

	private void resolveUpdateValues(String tableId, Map<Long, Pair<PartialRow, Row>> rowsToUpdate, ColumnMapper columnMapper)
			throws IOException, NotFoundException {
		RowSetAccessor currentRowData = tableRowTruthDao.getLatestVersionsWithRowData(tableId, rowsToUpdate.keySet(), 0L, columnMapper);
		for (Map.Entry<Long, Pair<PartialRow, Row>> rowToUpdate : rowsToUpdate.entrySet()) {
			Long rowId = rowToUpdate.getKey();
			PartialRow partialRow = rowToUpdate.getValue().getFirst();
			Row row = rowToUpdate.getValue().getSecond();
			RowAccessor currentRow = currentRowData.getRow(rowId);

			List<String> values = Lists.newArrayListWithCapacity(columnMapper.selectColumnCount());
			for (SelectColumnAndModel model : columnMapper.getSelectColumnAndModels()) {
				String value = null;
				if (model.getColumnModel() != null) {
					if (partialRow.getValues().containsKey(model.getColumnModel().getId())) {
						value = partialRow.getValues().get(model.getColumnModel().getId());
					} else {
						value = currentRow.getCellById(Long.parseLong(model.getColumnModel().getId()));
					}
					if (value == null) {
						value = model.getColumnModel().getDefaultValue();
					}
				}
				values.add(value);
			}
			row.setValues(values);
		}
	}

	@WriteTransaction
	@Override
	public RowReferenceSet deleteRows(UserInfo user, String tableId, RowSelection rowsToDelete) throws DatastoreException, NotFoundException,
			IOException {
		Validate.required(user, "user");
		Validate.required(tableId, "tableId");
		Validate.required(rowsToDelete, "rowsToDelete");

		// Validate the user has permission to edit the table
		validateTableWriteAccess(user, tableId);

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
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(getColumnModelsForTable(tableId), false);
		RawRowSet rowSetToDelete = new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), rowsToDelete.getEtag(), tableId, rows);
		RowReferenceSet result = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, mapper, rowSetToDelete);
		// The table has change so we must reset the state.
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		return result;
	}

	@WriteTransaction
	@Override
	public void deleteAllRows(String tableId) {
		Validate.required(tableId, "tableId");
		tableRowTruthDao.deleteAllRowDataForTable(tableId);
	}

	@WriteTransaction
	@Override
	public String appendRowsAsStream(UserInfo user, String tableId, ColumnMapper columnMapper, Iterator<Row> rowStream, String etag,
			RowReferenceSet results, ProgressCallback<Long> progressCallback) throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(tableId, "TableId");
		ValidateArgument.required(columnMapper, "columnMapper");
		validateFeatureEnabled();

		// Validate the user has permission to edit the table
		validateTableWriteAccess(user, tableId);

		// To prevent race conditions on concurrency checking we apply all changes to a single table
		// serially by locking on the table's Id.
		columnModelDAO.lockOnOwner(tableId);
		
		List<Long> ids = Transform.toList(columnMapper.getColumnModels(), TableModelUtils.COLUMN_MODEL_TO_ID);
		// Calculate the size per row
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(columnMapper.getColumnModels());
		List<Row> batch = new LinkedList<Row>();
		int batchSizeBytes = 0;
		int count = 0;
		RawRowSet delta = new RawRowSet(ids, etag, tableId, batch);
		while(rowStream.hasNext()){
			batch.add(rowStream.next());
			batchSizeBytes += maxBytesPerRow;
			if(batchSizeBytes >= maxBytesPerChangeSet){
				// Validate there aren't any illegal file handle replaces
				validateFileHandles(user, tableId, columnMapper, delta.getRows());
				// Send this batch and keep the etag.
				etag = appendBatchOfRowsToTable(user, columnMapper, delta, results, progressCallback);
				// Clear the batch
				count += batch.size();
				batch.clear();
				batchSizeBytes = 0;
				log.info("Appended: "+count+" rows to table: "+tableId);
			}
		}
		// Send the last batch is there are any rows
		if(!batch.isEmpty()){
			// Validate there aren't any illegal file handle replaces
			validateFileHandles(user, tableId, columnMapper, delta.getRows());
			etag = appendBatchOfRowsToTable(user, columnMapper, delta, results, progressCallback);
		}
		// The table has change so we must reset the state.
		tableStatusDAO.resetTableStatusToProcessing(tableId);
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
	 * @param etag
	 * @param results
	 * @param headers
	 * @param batch
	 * @return
	 * @throws IOException
	 * @throws ReadOnlyException If the stack status is anything other than READ_WRITE
	 */
	private String appendBatchOfRowsToTable(UserInfo user, ColumnMapper columnMapper, RawRowSet delta, RowReferenceSet results,
			ProgressCallback<Long> progressCallback)
			throws IOException, ReadOnlyException {
		// See PLFM-3041
		checkStackWiteStatus();
		RowReferenceSet rrs = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), delta.getTableId(), columnMapper, delta);
		if(progressCallback != null){
			progressCallback.progressMade(new Long(rrs.getRows().size()));
		}
		if(results != null){
			results.setEtag(rrs.getEtag());
			results.setHeaders(columnMapper.getSelectColumns());
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
	public List<ColumnModel> getColumnModelsForTable(String tableId) throws DatastoreException, NotFoundException {
		return columnModelDAO.getColumnModelsForObject(tableId);
	}

	@Override
	public List<ColumnModel> getColumnsForHeaders(List<String> headers) throws DatastoreException, NotFoundException {
		// Not all of the headers are columns so filter out those that are not.
		List<String> columnIds = new LinkedList<String>();
		for(String header: headers){
			// Is this a columns ID
			try {
				Long id = Long.parseLong(header);
				// This header is a columnId so include it
				columnIds.add(id.toString());
			} catch (NumberFormatException e) {
				// expected, this just means a header was not a columnModel id so we skip it.
			}
		}
		// If the columnIds is null, then none of the headers were actual column model ids.
		if(columnIds.isEmpty()){
			return new ArrayList<ColumnModel>(0);
		}
		return columnModelDAO.getColumnModel(columnIds, true);
	}
	
	@Override
	public List<TableRowChange> listRowSetsKeysForTable(String tableId) {
		return tableRowTruthDao.listRowSetsKeysForTable(tableId);
	}

	@Override
	public RowSet getRowSet(String tableId, Long rowVersion, ColumnMapper columnMapper) throws IOException,
			NotFoundException {
		return tableRowTruthDao.getRowSet(tableId, rowVersion, columnMapper);
	}

	@Override
	public Map<Long, Long> getCurrentRowVersions(String tableId, Long minVersion, long rowIdOffset, long limit) throws IOException,
			NotFoundException, TableUnavilableException {
		return tableRowTruthDao.getLatestVersions(tableId, minVersion, rowIdOffset, limit);
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
		validateTableReadAccess(userInfo, tableId);
		Row row = tableRowTruthDao.getRowOriginal(tableId, rowRef, TableModelUtils.createSingleColumnColumnMapper(column, false));
		return row.getValues().get(0);
	}

	@Override
	public RowSet getCellValues(UserInfo userInfo, String tableId, RowReferenceSet rowRefs, ColumnMapper resultSchema)
			throws IOException, NotFoundException {
		validateTableReadAccess(userInfo, tableId);
		return tableRowTruthDao.getRowSet(rowRefs, resultSchema);
	}

	@Override
	public <R,T> R tryRunWithTableExclusiveLock(ProgressCallback<T> callback,
			String tableId, int timeoutSec, ProgressingCallable<R, T> callable)
			throws LockUnavilableException, InterruptedException, Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithWriteLock(callback, key, timeoutSec, callable);
	}

	@Override
	public <R,T> R tryRunWithTableNonexclusiveLock(ProgressCallback<T> callback, String tableId, int lockTimeoutSec, ProgressingCallable<R, T> callable)
			throws Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithReadLock(callback, key, lockTimeoutSec, callable);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableRowManager#getTableStatusOrCreateIfNotExists(java.lang.String)
	 */
	@Override
	public TableStatus getTableStatusOrCreateIfNotExists(String tableId) throws NotFoundException, IOException {
		try {
			TableStatus status = tableStatusDAO.getTableStatus(tableId);
			if(!TableState.AVAILABLE.equals(status.getState())){
				return status;
			}
			// We need to validate the table is really AVAILABLE
			TableRowChange lastChange = getLastTableRowChange(tableId);
			if(lastChange == null){
				if(status.getLastTableChangeEtag() == null){
					// there are not changes and status etag is null then the table is empty and AVAILABLE.
					return status;
				}
			}else{
				// We have at least one change on the table, does the status etag match the change etag?
				if(lastChange.getEtag().equals(status.getLastTableChangeEtag())){
					// the table is up-to-date
					return status;
				}
			}
			// the table status and last change do not match. Set the table to processing an trigger an update.
			return setTableToProcessingAndTriggerUpdate(tableId);
			
		} catch (NotFoundException e) {
			// make sure the table exists
			if (!nodeDao.doesNodeExist(KeyFactory.stringToKey(tableId))) {
				throw new NotFoundException("Table " + tableId + " not found");
			}
			return setTableToProcessingAndTriggerUpdate(tableId);
		}
	}

	/**
	 * Set the table's status to be PROCESSING, fire a table update and return the table's status.
	 * @param tableId
	 * @return
	 */
	private TableStatus setTableToProcessingAndTriggerUpdate(String tableId) {
		// we get here, if the index for this table is not (yet?) being build. We need to kick off the
		// building of the index and report the table as unavailable
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		// status should exist now
		return tableStatusDAO.getTableStatus(tableId);
	}

	@Override
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException,
			NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken, tableChangeEtag);
	}

	@Override
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken, errorMessage, errorDetails);
	}

	@Override
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken, progressMessage, currentProgress, totalProgress);
	}

	@Override
	public Pair<QueryResult, Long> query(ProgressCallback<Void> progressCallback, UserInfo user, String query, List<SortItem> sortList, Long offset, Long limit, boolean runQuery,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException,
			TableFailedException {
		return query(progressCallback, user, createQuery(query, sortList), offset, limit, runQuery, runCount, isConsistent);
	}

	@Override
	public Pair<QueryResult, Long> query(ProgressCallback<Void> progressCallback, UserInfo user, SqlQuery query, Long offset, Long limit, boolean runQuery,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException,
			TableFailedException {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "SqlQuery");
		validateFeatureEnabled();
		// Validate the user has read access on this object
		validateTableReadAccess(user, query.getTableId());
		// Does this table exist?
		if(query.getTableSchema() == null || query.getTableSchema().isEmpty()){
			// there are no columns for this table so the table does not actually exist.
			// for this case the caller expects an empty result set.  See PLFM-2636
			QueryResult result = new QueryResult();
			RowSet emptyRowSet = new RowSet();
			emptyRowSet.setTableId(query.getTableId());
			result.setQueryResults(emptyRowSet);
			return Pair.create(runQuery ? result : null, runCount ? 0L : null);
		}

		SqlQuery paginatedQuery = null;
		Long maxRowsPerPage = null;
		boolean oneRowWasAdded = false;
		if (runQuery || (runCount && query.isAggregatedResult())) {
			maxRowsPerPage = getMaxRowsPerPage(query.getSelectColumnModels());
			if (maxRowsPerPage == null || maxRowsPerPage == 0) {
				maxRowsPerPage = 100L;
			}
			
			// limits are a bit complicated. We want to get the minimum of maxRowsPerPage and limit in query, but
			// if we use maxRowsPerPage, we want to add 1 row, which we later remove and use as a marker to see if there
			// are more results
			
			long limitFromRequest = (limit != null) ? limit : Long.MAX_VALUE;
			long offsetFromRequest = (offset != null) ? offset : 0L;
			
			long limitFromQuery = Long.MAX_VALUE;
			long offsetFromQuery = 0L;
			
			Pagination pagination = query.getModel().getTableExpression().getPagination();
			if (pagination != null) {
				if (pagination.getLimit() != null) {
					limitFromQuery = pagination.getLimit();
				}
				if (pagination.getOffset() != null) {
					offsetFromQuery = pagination.getOffset();
				}
			}
			
			long paginatedOffset = offsetFromQuery + offsetFromRequest;
			// adjust the limit from the query based on the additional offset (assume Long.MAX_VALUE - offset is still
			// always large enough)
			limitFromQuery = Math.max(0, limitFromQuery - offsetFromRequest);
			
			long paginatedLimit = Math.min(limitFromRequest, limitFromQuery);
			if (paginatedLimit > maxRowsPerPage) {
				paginatedLimit = maxRowsPerPage + 1;
				oneRowWasAdded = true;
			}
			
			paginatedQuery = createPaginatedQuery(query, paginatedOffset, paginatedLimit);
		}

		SqlQuery countQuery = null;
		if (runCount) {
			// there are two ways of getting counts. One is to replace the select clause with count(*) and another is to
			// use SQL_CALC_FOUND_ROWS & FOUND_ROWS()
			// the count(*) is usually more efficient, but it is almost impossible to get that right if the query to
			// count is an aggregate itself.
			// So, we use count(*) if there is no aggregate and SQL_CALC_FOUND_ROWS if there is
			if (query.isAggregatedResult()) {
				// add the SQL_CALC_FOUND_ROWS to the query
				SelectList paginatedSelectList = paginatedQuery.getModel().getSelectList();
				if (BooleanUtils.isTrue(paginatedQuery.getModel().getSelectList().getAsterisk())) {
					// bug in mysql, SQL_CALC_FOUND_ROWS does not work when the select is '*'. Expand to the known
					// columns
					paginatedSelectList = new SelectList(Lists.transform(paginatedQuery.getSelectColumnModels().getSelectColumns(),
							new Function<SelectColumn, DerivedColumn>() {
								@Override
								public DerivedColumn apply(SelectColumn input) {
									return SQLTranslatorUtils.createDerivedColumn(input.getName());
								}
							}));
				}
				QuerySpecification paginatedModel = new QuerySpecification(SqlDirective.SQL_CALC_FOUND_ROWS, paginatedQuery.getModel()
						.getSetQuantifier(), paginatedSelectList, paginatedQuery.getModel().getTableExpression());
				paginatedQuery = new SqlQuery(paginatedModel, paginatedQuery.getTableSchema(), paginatedQuery.getTableId());
				// and make the count query "SELECT FOUND_ROWS()"
				SelectList selectList = new SelectList(Lists.newArrayList(SQLTranslatorUtils.createDerivedColumn(MysqlFunction.FOUND_ROWS)));
				countQuery = new SqlQuery(new QuerySpecification(null, null, selectList, null), paginatedQuery.getTableSchema(),
						paginatedQuery.getTableId());
			} else {
				QuerySpecification model = SqlElementUntils.convertToCountQuery(query.getModel());
				// Lookup the column models for this table
				List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(query.getTableId());
				countQuery = new SqlQuery(model, columnModels, query.getTableId());
			}
		}

		RowSet rowSet = null;
		Long count = null;
		// If this is a consistent read then we need a read lock
		if (isConsistent) {
			// A consistent query is only run if the table index is available and up-to-date
			// with the table state. A read-lock on the index will be held while the query is run.
			Pair<RowSet, Long> result = runConsistentQuery(progressCallback, paginatedQuery, countQuery);
			rowSet = result.getFirst();
			count = result.getSecond();
		} else {
			// This path queries the table index regardless of the state of the index and without a
			// read-lock.
			if (paginatedQuery != null) {
				rowSet = query(progressCallback, paginatedQuery);
			}
			if (countQuery != null) {
				RowSet countResult = query(progressCallback, countQuery);
				List<Row> rows = countResult.getRows();
				if (!rows.isEmpty()) {
					List<String> values = rows.get(0).getValues();
					if (!values.isEmpty()) {
						count = Long.parseLong(values.get(0));
					}
				}
			}
		}

		// post processing for query result
		QueryResult queryResult = null;
		if (runQuery && rowSet != null) {
			QueryNextPageToken nextPageToken = null;
			if (oneRowWasAdded) {
				if (rowSet.getRows().size() > maxRowsPerPage) {
					// we need to limit the rowSet to maxRowsPerPage and set the next page token
					rowSet.setRows(Lists.newArrayList(rowSet.getRows().subList(0, maxRowsPerPage.intValue())));
					nextPageToken = createNextPageToken(query, (offset == null ? 0 : offset) + maxRowsPerPage, limit, isConsistent);
				}
			}

			queryResult = new QueryResult();
			queryResult.setQueryResults(rowSet);
			queryResult.setNextPageToken(nextPageToken);
		}

		// post processing for count. When a limit and/or offset is specified in a query, count(*) just ignores those,
		// since it assumes the limit & offset apply to the one row count(*) returns. In actuality, we want to apply
		// that limit & offset to the count itself. We do that here manually.
		if (runCount && count != null) {
			Pagination pagination = query.getModel().getTableExpression().getPagination();
			if (pagination != null) {
				if (pagination.getOffset() != null) {
					long offsetForCount = pagination.getOffset();
					count = Math.max(0, count - offsetForCount);
				}
				if (pagination.getLimit() != null) {
					long limitForCount = pagination.getLimit();
					count = Math.min(limitForCount, count);
				}
			}
		}

		return Pair.create(queryResult, count);
	}
	
	@Override
	public QueryResult queryNextPage(ProgressCallback<Void> progressCallback, UserInfo user, QueryNextPageToken nextPageToken) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException {
		Query query = createQueryFromNextPageToken(nextPageToken);
		Pair<QueryResult, Long> queryResult = query(progressCallback, user, query.getSql(), null, query.getOffset(), query.getLimit(), true,
				false, query.getIsConsistent());
		return queryResult.getFirst();
	}

	@Override
	public QueryResultBundle queryBundle(ProgressCallback<Void> progressCallback, UserInfo user, QueryBundleRequest queryBundle) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException {
		ValidateArgument.required(queryBundle.getQuery(), "query");
		ValidateArgument.required(queryBundle.getQuery().getSql(), "query.sql");

		QueryResultBundle bundle = new QueryResultBundle();
		// The SQL query is need for the actual query, select columns, and max rows per page.
		SqlQuery sqlQuery = createQuery(queryBundle.getQuery().getSql(), queryBundle.getQuery().getSort());

		// query
		long partMask = -1L; // default all
		if (queryBundle.getPartMask() != null) {
			partMask = queryBundle.getPartMask();
		}
		boolean runQuery = ((partMask & BUNDLE_MASK_QUERY_RESULTS) != 0);
		boolean runCount = ((partMask & BUNDLE_MASK_QUERY_COUNT) != 0);
		if (runQuery || runCount) {
			Pair<QueryResult, Long> queryResult = query(progressCallback, user, sqlQuery, queryBundle.getQuery().getOffset(),
					queryBundle.getQuery().getLimit(), runQuery, runCount, BooleanUtils.isNotFalse(queryBundle.getQuery().getIsConsistent()));
			bundle.setQueryResult(queryResult.getFirst());
			bundle.setQueryCount(queryResult.getSecond());
		}
		// select columns must be fetched for for the select columns or max rows per page.
		if ((partMask & BUNDLE_MASK_QUERY_SELECT_COLUMNS) > 0) {
			bundle.setSelectColumns(sqlQuery.getSelectColumnModels().getSelectColumns());
		}
		// all schema columns
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setColumnModels(sqlQuery.getTableSchema());
		}
		// Max rows per column
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setMaxRowsPerPage(getMaxRowsPerPage(sqlQuery.getSelectColumnModels()));
		}
		return bundle;
	}

	public static final Charset UTF8 = Charset.forName("UTF-8");
	
	/**
	 * Create a QueryNextPageToken from a sql query.
	 * @param sql
	 * @param nextOffset
	 * @param limit
	 * @param isConsistent
	 * @return
	 */
	public static QueryNextPageToken createNextPageToken(SqlQuery sql, Long nextOffset, Long limit, boolean isConsistent) {
		return createNextPageToken(sql.getModel().toString(), nextOffset, limit, isConsistent);
	}

	/**
	 * Create a QueryNextPageToken from a sql string.
	 * @param sql
	 * @param nextOffset
	 * @param limit
	 * @param isConsistent
	 * @return
	 */
	public static QueryNextPageToken createNextPageToken(String sql, Long nextOffset, Long limit, boolean isConsistent) {
		Query query = new Query();
		query.setSql(sql);
		query.setOffset(nextOffset);
		query.setLimit(limit);
		query.setIsConsistent(isConsistent);

		StringWriter writer = new StringWriter(sql.length() + 50);
		XStream xstream = new XStream();
		xstream.alias("Query", Query.class);
		xstream.toXML(query, writer);
		Closer.closeQuietly(writer);
		QueryNextPageToken nextPageToken = new QueryNextPageToken();
		nextPageToken.setToken(writer.toString());
		return nextPageToken;
	}

	/**
	 * Extract a query from a next page token.
	 * @param nextPageToken
	 * @return
	 */
	public static Query createQueryFromNextPageToken(QueryNextPageToken nextPageToken) {
		if (nextPageToken == null || StringUtils.isEmpty(nextPageToken.getToken())) {
			throw new IllegalArgumentException("Next page token cannot be empty");
		}
		try {
			XStream xstream = new XStream();
			xstream.alias("Query", Query.class);
			Query query = (Query) xstream.fromXML(nextPageToken.getToken(), new Query());
			return query;
		} catch (Throwable t) {
			throw new IllegalArgumentException("Not a valid next page token", t);
		}
	}

	private SqlQuery createQuery(String sql, List<SortItem> sortList) {
		// First parse the SQL
		QuerySpecification model = parserQuery(sql);
		if (sortList != null && !sortList.isEmpty()) {
			// change the query to use the sort list
			model = SqlElementUntils.convertToSortedQuery(model, sortList);
		}

		String tableId = model.doVisit(new GetTableNameVisitor()).getTableName();
		if (tableId == null) {
			throw new IllegalArgumentException("Could not parse the table name in the sql expression: " + sql);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		return new SqlQuery(model, columnModels, tableId);
	}

	private SqlQuery createPaginatedQuery(SqlQuery query, Long offset, Long limit) {
		QuerySpecification model;
		try {
			model = SqlElementUntils.convertToPaginatedQuery(query.getModel(), offset, limit);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(query.getTableId());
		return new SqlQuery(model, columnModels, query.getTableId());
	}

	/**
	 * Run a consistent query. All resulting rows will be loading into memory at one time with this method.
	 * 
	 * @param tableId
	 * @param query
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	private Pair<RowSet, Long> runConsistentQuery(ProgressCallback<Void> progressCallback, final SqlQuery query, final SqlQuery countQuery) throws TableUnavilableException,
			NotFoundException, TableFailedException {
		List<QueryHandler> queryHandlers = Lists.newArrayList();
		final Object[] output = { null, null };
		if (query != null) {
			final RowSet results = new RowSet();
			output[0] = results;
			final List<Row> rows = new LinkedList<Row>();
			results.setRows(rows);
			results.setTableId(query.getTableId());
			// Stream the results but keep them in memory.
			queryHandlers.add(new QueryHandler(query, new RowAndHeaderHandler() {

				@Override
				public void writeHeader() {
					results.setHeaders(query.getSelectColumnModels().getSelectColumns());
				}

				@Override
				public void nextRow(Row row) {
					rows.add(row);
				}

				@Override
				public void setEtag(String etag) {
					results.setEtag(etag);
				}
			}));
		}

		if (countQuery != null) {
			queryHandlers.add(new QueryHandler(countQuery, new RowAndHeaderHandler() {

				@Override
				public void writeHeader() {
					// no-op
				}

				@Override
				public void nextRow(Row row) {
					if (!row.getValues().isEmpty()) {
						output[1] = Long.parseLong(row.getValues().get(0));
					}
				}

				@Override
				public void setEtag(String etag) {
				}
			}));
		}

		runConsistentQueryAsStream(progressCallback, queryHandlers);

		return Pair.create((RowSet) output[0], (Long) output[1]);
	}
	
	/**
	 * Run a consistent query and stream the results. Use this method to avoid loading all rows into memory at one time.
	 * 
	 * @param query
	 * @param handler
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	private void runConsistentQueryAsStream(ProgressCallback<Void> progressCallback, final List<QueryHandler> queryHandlers) throws TableUnavilableException, NotFoundException,
			TableFailedException {
		if (queryHandlers.isEmpty()) {
			return;
		}

		final String tableId = queryHandlers.get(0).getQuery().getTableId();
		try {
			// Run with a read lock.
			tryRunWithTableNonexclusiveLock(progressCallback, tableId, READ_LOCK_TIMEOUT_SEC, new ProgressingCallable<String, Void>() {
				@Override
				public String call(final ProgressCallback<Void> callback) throws Exception {
					// We can only run this query if the table is available.
					final TableStatus status = getTableStatusOrCreateIfNotExists(tableId);
					switch(status.getState()){
					case AVAILABLE:
						break;
					case PROCESSING:
						// When the table is not available, we communicate the current status of the
						// table in this exception.
						throw new TableUnavilableException(status);
					default:
					case PROCESSING_FAILED:
						// When the table is in a failed state, we communicate the current status of the
						// table in this exception.
						throw new TableFailedException(status);
					}
					// We can only run this
					final TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
					indexDao.executeInReadTransaction(new TransactionCallback<Void>() {
						@Override
						public Void doInTransaction(TransactionStatus transactionStatus) {
							for (QueryHandler queryHandler : queryHandlers) {
								if (!queryHandler.getQuery().getTableId().equals(tableId)) {
									throw new IllegalArgumentException("All queries should be on the same table, but " + tableId + " and "
											+ queryHandler.getQuery().getTableId());
								}
								indexDao.queryAsStream(callback, queryHandler.getQuery(), queryHandler.getHandler());
								queryHandler.getHandler().setEtag(status.getLastTableChangeEtag());
							}
							return null;
						}
					});
					return null;
				}
			});
		} catch (LockUnavilableException e) {
			TableUnavilableException e1 = createTableUnavilableException(tableId);
			throw e1;
		} catch(TableUnavilableException e){
			throw e;
		} catch (TableFailedException e) {
			throw e;
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			// All other exception are converted to generic datastore.
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * 
	 * @param sql
	 * @param writer
	 * @return The resulting RowSet will not contain any
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	@Override
	public DownloadFromTableResult runConsistentQueryAsStream(ProgressCallback<Void> progressCallback, UserInfo user, String sql, List<SortItem> sortList,
			final CSVWriterStream writer, boolean includeRowIdAndVersion, final boolean writeHeader) throws TableUnavilableException,
			NotFoundException, TableFailedException {
		// Convert to a query.
		final SqlQuery query = createQuery(sql, sortList);

		// Validate the user has read access on this object
		validateTableReadAccess(user, query.getTableId());

		if(includeRowIdAndVersion && query.isAggregatedResult()){
			// PLFM-2993: in the case of an aggregated result, we cannot return row id and row versions. Just don't
			// return them if it is an aggregated query
			includeRowIdAndVersion = false;
		}
		final DownloadFromTableResult repsonse = new DownloadFromTableResult();
		final boolean includeRowIdAndVersionFinal = includeRowIdAndVersion;
		repsonse.setTableId(query.getTableId());
		repsonse.setHeaders(query.getSelectColumnModels().getSelectColumns());

		runConsistentQueryAsStream(progressCallback, Collections.singletonList(new QueryHandler(query, new RowAndHeaderHandler() {
			@Override
			public void writeHeader() {
				if (writeHeader) {
					String[] csvHeaders = TableModelUtils.createColumnNameHeader(query.getSelectColumnModels().getSelectColumns(),
							includeRowIdAndVersionFinal);
					writer.writeNext(csvHeaders);
				}
			}

			@Override
			public void nextRow(Row row) {
				String[] array = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersionFinal);
				writer.writeNext(array);
			}

			@Override
			public void setEtag(String etag) {
				repsonse.setEtag(etag);
			}
		})));
		return repsonse;
	}

	TableUnavilableException createTableUnavilableException(String tableId){
		// When this occurs we need to lookup the status of the table and pass that to the caller
		try {
			TableStatus status = tableStatusDAO.getTableStatus(tableId);
			return new TableUnavilableException(status);
		} catch (NotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	/**
	 * Run the actual query.
	 * @param query
	 * @return
	 */
	private RowSet query(ProgressCallback<Void> callback, SqlQuery query) {
		// Get a connection
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(query.getTableId());
		return indexDao.query(callback, query);
	}
	
	/**
	 * Parser a query and convert ParseExceptions to IllegalArgumentExceptions
	 * 
	 * @param sql
	 * @return
	 */
	private QuerySpecification parserQuery(String sql){
		try {
			return TableQueryParser.parserQuery(sql);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private void validateRequestSize(ColumnMapper columnMapper, int rowCount) {
		// Validate the request is under the max bytes per requested
		if (!TableModelUtils.isRequestWithinMaxBytePerRequest(columnMapper, rowCount, this.maxBytesPerRequest)) {
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
	public void validateFileHandles(UserInfo user, String tableId, ColumnMapper columnMapper, List<Row> rows)
			throws IOException,
			NotFoundException {
		
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(columnMapper.getSelectColumns());
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
		// Extract the files handles from the change set.
		Set<Long> filesHandleIds = TableModelUtils.getFileHandleIdsInRowSet(rowSet);
		if(!filesHandleIds.isEmpty()){
			// convert the longs to strings.
			List<String> fileHandesStrings = new LinkedList<String>();
			TableModelUtils.convertLongToString(filesHandleIds, fileHandesStrings);
			// The user must have the download permission for every file handle in the change set.
			List<FileHandleAuthorizationStatus> resutls = authorizationManager.canDownloadFile(user, fileHandesStrings, tableId, FileHandleAssociateType.TableEntity);
			for(FileHandleAuthorizationStatus status: resutls){
				if(!status.getStatus().getAuthorized()){
					throw new UnauthorizedException("Cannot access file: "+status.getFileHandleId());
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
	public Long getMaxRowsPerPage(ColumnMapper columnMapper) {
		// Calculate the size
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSizeForColumnModels(columnMapper);
		if (maxRowSizeBytes < 1)
			return null;
		return (long) (this.maxBytesPerRequest / maxRowSizeBytes);
	}

	@Override
	public Long getMaxRowsPerPage(List<ColumnModel> models) {
		// Calculate the size
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(models);
		if (maxRowSizeBytes < 1)
			return null;
		return (long) (this.maxBytesPerRequest / maxRowSizeBytes);
	}

	private void validateTableReadAccess(UserInfo userInfo, String tableId) throws UnauthorizedException, DatastoreException,
			NotFoundException {
		// They must have read permission to access table content.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY,
				ACCESS_TYPE.READ));
		// And they must have download permission to access table content.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY,
				ACCESS_TYPE.DOWNLOAD));
	}

	private void validateTableWriteAccess(UserInfo userInfo, String tableId) throws UnauthorizedException, DatastoreException,
			NotFoundException {
		// They must have update permission to change table content
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY,
				ACCESS_TYPE.UPDATE));
		// And they must have upload permission to change table content.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canAccess(userInfo, tableId, ObjectType.ENTITY,
				ACCESS_TYPE.UPLOAD));
	}

	@Override
	public Set<String> getFileHandleIdsAssociatedWithTable(String tableId,
			List<String> toTest) {
		Set<Long> longSet = new HashSet<Long>(toTest.size());
		TableModelUtils.convertStringToLong(toTest, longSet);
		Set<Long> results = getFileHandleIdsAssociatedWithTable(tableId, longSet);
		Set<String> resultString = new HashSet<String>(results.size());
		TableModelUtils.convertLongToString(results, resultString);
		return resultString;
	}

	@Override
	public Set<Long> getFileHandleIdsAssociatedWithTable(final String tableId,
			final Set<Long> toTest) {
		try {
			return this.tryRunWithTableNonexclusiveLock(null, tableId, READ_LOCK_TIMEOUT_SEC, new ProgressingCallable<Set<Long>, Void>(){

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

}
