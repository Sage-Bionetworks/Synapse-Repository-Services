package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreRunner;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.status.StatusEnum;
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
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
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
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.Closer;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;

public class TableRowManagerImpl implements TableRowManager {
	
	static private Log log = LogFactory.getLog(TableRowManagerImpl.class);
	
	public static final long BUNDLE_MASK_QUERY_RESULTS = 0x1;
	public static final long BUNDLE_MASK_QUERY_COUNT = 0x2;
	public static final long BUNDLE_MASK_QUERY_SELECT_COLUMNS = 0x4;
	public static final long BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE = 0x8;

	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	TableRowTruthDAO tableRowTruthDao;
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	ColumnModelDAO columnModelDAO;
	@Autowired
	ExclusiveOrSharedSemaphoreRunner exclusiveOrSharedSemaphoreRunner;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	StackStatusDao stackStatusDao;
	
	/**
	 * Injected via Spring.
	 */
	long tableReadTimeoutMS;
	
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
	 * @param tableReadTimeoutMS
	 */
	public void setTableReadTimeoutMS(long tableReadTimeoutMS) {
		this.tableReadTimeoutMS = tableReadTimeoutMS;
	}

	/**
	 * Injected via spring
	 * @param maxBytesPerChangeSet
	 */
	public void setMaxBytesPerChangeSet(int maxBytesPerChangeSet) {
		this.maxBytesPerChangeSet = maxBytesPerChangeSet;
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public RowReferenceSet appendRows(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta) throws DatastoreException, NotFoundException, IOException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		if(models == null) throw new IllegalArgumentException("Models cannot be null");
		if(delta == null) throw new IllegalArgumentException("RowSet cannot be null");
		// Validate the request is under the max bytes per requested
		validateRequestSize(models, delta.getRows().size());
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		appendRowsAsStream(user, tableId, models, delta.getRows().iterator(), delta.getEtag(), results, null);
		return results;
	}
	
	@Override
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId, List<ColumnModel> models,
			PartialRowSet rowsToAppendOrUpdateOrDelete)
			throws DatastoreException, NotFoundException, IOException {
		Validate.required(user, "User");
		Validate.required(tableId, "TableId");
		Validate.required(models, "Models");
		Validate.required(rowsToAppendOrUpdateOrDelete, "RowsToAppendOrUpdate");
		// Validate the request is under the max bytes per requested
		validateRequestSize(models, rowsToAppendOrUpdateOrDelete.getRows().size());
		// For this case we want to capture the resulting RowReferenceSet
		RowReferenceSet results = new RowReferenceSet();
		RowSet fullRowsToAppendOrUpdateOrDelete = mergeWithLastVersion(tableId, rowsToAppendOrUpdateOrDelete, models);
		appendRowsAsStream(user, tableId, models, fullRowsToAppendOrUpdateOrDelete.getRows().iterator(),
				fullRowsToAppendOrUpdateOrDelete.getEtag(), results, null);
		return results;
	}

	/**
	 * This method merges the partial row with the most current version of the row as it exists on S3. For updates, this
	 * will find the most current version, and any column not present as a key in the map will be replaced with the most
	 * recent value. For inserts, this will replace null cell values with their defaults.
	 */
	private RowSet mergeWithLastVersion(String tableId, PartialRowSet rowsToAppendOrUpdateOrDelete, List<ColumnModel> models)
			throws IOException,
			NotFoundException {
		RowSet result = new RowSet();
		TableRowChange lastTableRowChange = tableRowTruthDao.getLastTableRowChange(tableId);
		result.setEtag(lastTableRowChange == null ? null : lastTableRowChange.getEtag());
		result.setHeaders(TableModelUtils.getHeaders(models));
		result.setTableId(tableId);
		List<Row> rows = Lists.newArrayListWithCapacity(rowsToAppendOrUpdateOrDelete.getRows().size());
		Map<Long, Pair<PartialRow, Row>> rowsToUpdate = Maps.newHashMap();
		for (PartialRow partialRow : rowsToAppendOrUpdateOrDelete.getRows()) {
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
				row = resolveInsertValues(partialRow, models);
			}
			rows.add(row);
		}
		resolveUpdateValues(tableId, rowsToUpdate, models);
		result.setRows(rows);
		return result;
	}

	private Row resolveInsertValues(PartialRow partialRow, List<ColumnModel> models) {
		List<String> values = Lists.newArrayListWithCapacity(models.size());
		for (ColumnModel model : models) {
			String value = partialRow.getValues().get(model.getId());
			if (value == null) {
				value = model.getDefaultValue();
			}
			values.add(value);
		}
		Row row = new Row();
		row.setValues(values);
		return row;
	}

	private void resolveUpdateValues(String tableId, Map<Long, Pair<PartialRow, Row>> rowsToUpdate, List<ColumnModel> models)
			throws IOException, NotFoundException {
		RowSetAccessor currentRowData = tableRowTruthDao.getLatestVersionsWithRowData(tableId, rowsToUpdate.keySet(), 0L);
		for (Map.Entry<Long, Pair<PartialRow, Row>> rowToUpdate : rowsToUpdate.entrySet()) {
			Long rowId = rowToUpdate.getKey();
			PartialRow partialRow = rowToUpdate.getValue().getFirst();
			Row row = rowToUpdate.getValue().getSecond();
			RowAccessor currentRow = currentRowData.getRow(rowId);

			List<String> values = Lists.newArrayListWithCapacity(models.size());
			for (ColumnModel model : models) {
				String value;
				if (partialRow.getValues().containsKey(model.getId())) {
					value = partialRow.getValues().get(model.getId());
				} else {
					value = currentRow.getCell(model.getId());
				}
				if (value == null) {
					value = model.getDefaultValue();
				}
				values.add(value);
			}
			row.setValues(values);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public RowReferenceSet deleteRows(UserInfo user, String tableId, List<ColumnModel> models, RowSelection rowsToDelete)
			throws DatastoreException, NotFoundException, IOException {
		Validate.required(user, "user");
		Validate.required(tableId, "tableId");
		Validate.required(rowsToDelete, "rowsToDelete");

		// Validate the user has permission to edit the table
		validateTableWriteAccess(user, tableId);

		final List<String> headers = TableModelUtils.getHeaders(models);

		// create a rowset of all deletes
		List<Row> rows = Lists.transform(rowsToDelete.getRowIds(), new Function<Long, Row>() {
			@Override
			public Row apply(Long input) {
				Row row = new Row();
				row.setRowId(input);
				row.setVersionNumber(null);
				row.setValues(null);
				return row;
			}
		});
		RowSet rowSetToDelete = new RowSet();
		rowSetToDelete.setHeaders(headers);
		rowSetToDelete.setEtag(rowsToDelete.getEtag());
		rowSetToDelete.setTableId(tableId);
		// need copy of list here, as appendRowSetToTable changes rows in place
		rowSetToDelete.setRows(Lists.newArrayList(rows));
		RowReferenceSet result = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, rowSetToDelete);
		// The table has change so we must reset the state.
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAllRows(String tableId) {
		Validate.required(tableId, "tableId");
		tableRowTruthDao.deleteAllRowDataForTable(tableId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> models, Iterator<Row> rowStream, String etag,
			RowReferenceSet results, ProgressCallback<Long> progressCallback) throws DatastoreException, NotFoundException, IOException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		if(models == null) throw new IllegalArgumentException("Models cannot be null");
		validateFeatureEnabled();

		// Validate the user has permission to edit the table
		validateTableWriteAccess(user, tableId);

		// To prevent race conditions on concurrency checking we apply all changes to a single table
		// serially by locking on the table's Id.
		columnModelDAO.lockOnOwner(tableId);
		
		List<String> headers = TableModelUtils.getHeaders(models);
		// Calculate the size per row
		int maxBytesPerRow = TableModelUtils.calculateMaxRowSize(models);
		List<Row> batch = new LinkedList<Row>();
		int batchSizeBytes = 0;
		int count = 0;
		RowSet delta = new RowSet();
		delta.setEtag(etag);
		delta.setHeaders(headers);
		delta.setRows(batch);
		delta.setTableId(tableId);
		while(rowStream.hasNext()){
			batch.add(rowStream.next());
			batchSizeBytes += maxBytesPerRow;
			if(batchSizeBytes >= maxBytesPerChangeSet){
				// Validate there aren't any illegal file handle replaces
				validateFileHandles(user, tableId, models, delta, etag);
				// Send this batch and keep the etag.
				etag = appendBatchOfRowsToTable(user, models, delta, results, progressCallback);
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
			validateFileHandles(user, tableId, models, delta, etag);
			etag = appendBatchOfRowsToTable(user, models, delta, results, progressCallback);
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
	 * @param models
	 * @param etag
	 * @param results
	 * @param headers
	 * @param batch
	 * @return
	 * @throws IOException
	 * @throws ReadOnlyException If the stack status is anything other than READ_WRITE
	 */
	private String appendBatchOfRowsToTable(UserInfo user, List<ColumnModel> models, RowSet delta, RowReferenceSet results, ProgressCallback<Long> progressCallback)
			throws IOException, ReadOnlyException {
		// See PLFM-3041
		checkStackWiteStatus();
		RowReferenceSet rrs = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), delta.getTableId(), models, delta);
		if(progressCallback != null){
			progressCallback.progressMade(new Long(rrs.getRows().size()));
		}
		if(results != null){
			results.setEtag(rrs.getEtag());
			results.setHeaders(delta.getHeaders());
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
	public RowSet getRowSet(String tableId, Long rowVersion, Set<Long> rowsToGet) throws IOException, NotFoundException {
		return tableRowTruthDao.getRowSet(tableId, rowVersion, rowsToGet);
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
		Row row = tableRowTruthDao.getRowOriginal(tableId, rowRef, Collections.singletonList(column));
		return row.getValues().get(0);
	}

	@Override
	public RowSet getCellValues(UserInfo userInfo, String tableId, RowReferenceSet rowRefs, List<ColumnModel> columns) throws IOException,
			NotFoundException {
		validateTableReadAccess(userInfo, tableId);
		return tableRowTruthDao.getRowSet(rowRefs, columns);
	}

	@Override
	public <T> T tryRunWithTableExclusiveLock(String tableId, long lockTimeoutMS, Callable<T> runner)
			throws InterruptedException, Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return exclusiveOrSharedSemaphoreRunner.tryRunWithExclusiveLock(key, lockTimeoutMS, runner);
	}

	@Override
	public <T> T tryRunWithTableNonexclusiveLock(String tableId, long lockTimeoutMS, Callable<T> runner)
			throws Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return exclusiveOrSharedSemaphoreRunner.tryRunWithSharedLock(key, lockTimeoutMS, runner);
	}

	@Override
	public TableStatus getTableStatusOrCreateIfNotExists(String tableId) throws NotFoundException {
		try {
			return tableStatusDAO.getTableStatus(tableId);
		} catch (NotFoundException e) {
			// make sure the table exists
			if (!nodeDao.doesNodeExist(KeyFactory.stringToKey(tableId))) {
				throw new NotFoundException("Table " + tableId + " not found");
			}
			// we get here, if the index for this table is not (yet?) being build. We need to kick off the
			// building of the index and report the table as unavailable
			tableStatusDAO.resetTableStatusToProcessing(tableId);
			// status should exist now
			return tableStatusDAO.getTableStatus(tableId);
		}
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
	public Pair<QueryResult, Long> query(UserInfo user, String query, Long offset, Long limit, boolean runQuery, boolean runCount,
			boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException, TableFailedException {
		return query(user, createQuery(query), offset, limit, runQuery, runCount, isConsistent);
	}

	@Override
	public Pair<QueryResult, Long> query(UserInfo user, SqlQuery query, Long offset, Long limit, boolean runQuery, boolean runCount,
			boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException, TableFailedException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(query == null) throw new IllegalArgumentException("SqlQuery cannot be null");
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
		if (runQuery) {
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
					paginatedSelectList = new SelectList(Lists.transform(paginatedQuery.getSelectColumnModels(),
							new Function<ColumnModel, DerivedColumn>() {
								@Override
								public DerivedColumn apply(ColumnModel input) {
									return SQLTranslatorUtils.createDerivedColumn(input.getName());
								}
							}));
				}
				QuerySpecification paginatedModel = new QuerySpecification(SqlDirective.SQL_CALC_FOUND_ROWS, paginatedQuery.getModel()
						.getSetQuantifier(), paginatedSelectList, paginatedQuery.getModel().getTableExpression());
				paginatedQuery = new SqlQuery(paginatedModel, paginatedQuery.getTableSchema());
				// and make the count query "SELECT FOUND_ROWS()"
				SelectList selectList = new SelectList(Lists.newArrayList(SQLTranslatorUtils.createDerivedColumn(MysqlFunction.FOUND_ROWS)));
				TableExpression tableExpression = SqlElementUntils.removeOrderByClause(paginatedQuery.getModel().getTableExpression());
				countQuery = new SqlQuery(new QuerySpecification(null, null, selectList, tableExpression), paginatedQuery.getTableSchema());
			} else {
				QuerySpecification model = SqlElementUntils.convertToCountQuery(query.getModel());
				// Lookup the column models for this table
				List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(query.getTableId());
				countQuery = new SqlQuery(model, columnModels);
			}
		}

		RowSet rowSet = null;
		Long count = null;
		// If this is a consistent read then we need a read lock
		if (isConsistent) {
			// A consistent query is only run if the table index is available and up-to-date
			// with the table state. A read-lock on the index will be held while the query is run.
			Pair<RowSet, Long> result = runConsistentQuery(paginatedQuery, countQuery);
			rowSet = result.getFirst();
			count = result.getSecond();
		} else {
			// This path queries the table index regardless of the state of the index and without a
			// read-lock.
			if (paginatedQuery != null) {
				rowSet = query(paginatedQuery);
			}
			if (countQuery != null) {
				RowSet countResult = query(countQuery);
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
		if (rowSet != null) {
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
	public QueryResult queryNextPage(UserInfo user, QueryNextPageToken nextPageToken) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException {
		Query query = createQueryFromNextPageToken(nextPageToken);
		Pair<QueryResult, Long> queryResult = query(user, query.getSql(), query.getOffset(), query.getLimit(), true,
				false, query.getIsConsistent());
		return queryResult.getFirst();
	}

	@Override
	public QueryResultBundle queryBundle(UserInfo user, QueryBundleRequest queryBundle) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException {
		ValidateArgument.required(queryBundle.getQuery(), "query");
		ValidateArgument.required(queryBundle.getQuery().getSql(), "query.sql");

		QueryResultBundle bundle = new QueryResultBundle();
		// The SQL query is need for the actual query, select columns, and max rows per page.
		SqlQuery sqlQuery = createQuery(queryBundle.getQuery().getSql());

		// query
		long partMask = -1L; // default all
		if (queryBundle.getPartMask() != null) {
			partMask = queryBundle.getPartMask();
		}
		boolean runQuery = ((partMask & BUNDLE_MASK_QUERY_RESULTS) != 0);
		boolean runCount = ((partMask & BUNDLE_MASK_QUERY_COUNT) != 0);
		if (runQuery || runCount) {
			Pair<QueryResult, Long> queryResult = query(user, sqlQuery, queryBundle.getQuery().getOffset(),
					queryBundle.getQuery().getLimit(), runQuery, runCount, BooleanUtils.isNotFalse(queryBundle.getQuery().getIsConsistent()));
			bundle.setQueryResult(queryResult.getFirst());
			bundle.setQueryCount(queryResult.getSecond());
		}
		// select columns must be fetched for for the select columns or max rows per page.
		if ((partMask & BUNDLE_MASK_QUERY_SELECT_COLUMNS) > 0) {
			bundle.setSelectColumns(sqlQuery.getSelectColumnModels());
		}
		// Max rows per column
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setMaxRowsPerPage(getMaxRowsPerPage(sqlQuery.getSelectColumnModels()));
		}
		return bundle;
	}

	public static final Charset UTF8 = Charset.forName("UTF-8");

	private QueryNextPageToken createNextPageToken(SqlQuery sql, Long nextOffset, Long limit, boolean isConsistent) {
		Query query = new Query();
		StringBuilder sb = new StringBuilder(200);
		sql.getModel().toSQL(sb, null);
		query.setSql(sb.toString());
		query.setOffset(nextOffset);
		query.setLimit(limit);
		query.setIsConsistent(isConsistent);

		StringWriter writer = new StringWriter(sql.getOutputSQL().length() + 50);
		XStream xstream = new XStream();
		xstream.alias("Query", Query.class);
		xstream.toXML(query, writer);
		Closer.closeQuietly(writer);
		QueryNextPageToken nextPageToken = new QueryNextPageToken();
		nextPageToken.setToken(writer.toString());
		return nextPageToken;
	}

	private Query createQueryFromNextPageToken(QueryNextPageToken nextPageToken) {
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

	private SqlQuery createQuery(String sql) {
		// First parse the SQL
		QuerySpecification model = parserQuery(sql);
		String tableId = SqlElementUntils.getTableId(model);
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		return new SqlQuery(model, columnModels);
	}

	private SqlQuery createPaginatedQuery(SqlQuery query, Long offset, Long limit) {
		QuerySpecification model;
		try {
			model = SqlElementUntils.convertToPaginatedQuery(query.getModel(), offset, limit);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		String tableId = SqlElementUntils.getTableId(model);
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		return new SqlQuery(model, columnModels);
	}

	/**
	 * Validate that a query result will be under the max size.
	 * 
	 * @param query
	 * @param columnModels
	 * @param maxBytePerRequest
	 */
	public void validateQuerySize(SqlQuery query, int maxBytePerRequest) {
		Long limit = null;
		if(query.getModel().getTableExpression().getPagination() != null){
			limit = query.getModel().getTableExpression().getPagination().getLimit();
		}
		// What are the select columns?
		List<ColumnModel> selectColumns = query.getSelectColumnModels();
		if(!selectColumns.isEmpty()){
			// First make sure we have a limit
			if(limit == null){
				Long maxRowsPerPage = getMaxRowsPerPage(selectColumns);
				throw new IllegalArgumentException("LIMIT clause is required (between 1 and " + maxRowsPerPage + ")");
			}
			// Validate the request is under the max bytes per requested
			if(!TableModelUtils.isRequestWithinMaxBytePerRequest(selectColumns, limit.intValue(), maxBytePerRequest)){
				throw new IllegalArgumentException("Request exceed the maximum number of bytes per request.  Maximum : "+maxBytePerRequest+" bytes");
			}
		}
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
	private Pair<RowSet, Long> runConsistentQuery(final SqlQuery query, final SqlQuery countQuery) throws TableUnavilableException,
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
				public void nextRow(Row row) {
					rows.add(row);
				}

				@Override
				public void setHeaderColumnIds(List<String> headers) {
					results.setHeaders(headers);
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
				public void nextRow(Row row) {
					if (!row.getValues().isEmpty()) {
						output[1] = Long.parseLong(row.getValues().get(0));
					}
				}

				@Override
				public void setHeaderColumnIds(List<String> headers) {
				}

				@Override
				public void setEtag(String etag) {
				}
			}));
		}

		runConsistentQueryAsStream(queryHandlers);

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
	private void runConsistentQueryAsStream(final List<QueryHandler> queryHandlers) throws TableUnavilableException, NotFoundException,
			TableFailedException {
		if (queryHandlers.isEmpty()) {
			return;
		}

		final String tableId = queryHandlers.get(0).getQuery().getTableId();
		try {
			// Run with a read lock.
			tryRunWithTableNonexclusiveLock(tableId, tableReadTimeoutMS, new Callable<String>() {
				@Override
				public String call() throws Exception {
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
								indexDao.queryAsStream(queryHandler.getQuery(), queryHandler.getHandler());
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
	public DownloadFromTableResult runConsistentQueryAsStream(UserInfo user, String sql, final CSVWriterStream writer,
			boolean includeRowIdAndVersion) throws TableUnavilableException, NotFoundException, TableFailedException {
		// Convert to a query.
		final SqlQuery query = createQuery(sql);

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
		runConsistentQueryAsStream(Collections.singletonList(new QueryHandler(query, new RowAndHeaderHandler() {

			@Override
			public void nextRow(Row row) {
				String[] array = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersionFinal);
				writer.writeNext(array);
			}
			@Override
			public void setHeaderColumnIds(List<String> headers) {
				// Capture the headers
				repsonse.setHeaders(headers);
				// The headers passed here can be the column IDs. Use the IDs to create a name header or
				// return the name itself
				String[] header = TableModelUtils.createColumnNameHeader(headers, query.getcolumnNameToModelMap().values(),
						includeRowIdAndVersionFinal);
				writer.writeNext(header);
			}

			@Override
			public void setEtag(String etag) {
				repsonse.setEtag(etag);
			}
		})));
		return repsonse;
	}

	@Override
	public void updateLatestVersionCache(String tableId, ProgressCallback<Long> progressCallback) throws IOException {
		tableRowTruthDao.updateLatestVersionCache(tableId, progressCallback);
	}

	@Override
	public void removeCaches(String tableId) throws IOException {
		tableRowTruthDao.removeCaches(KeyFactory.stringToKey(tableId));
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
	private RowSet query(SqlQuery query){
		// Get a connection
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(query.getTableId());
		return indexDao.query(query);
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
	
	private void validateRequestSize(List<ColumnModel> models, int rowCount){
		// Validate the request is under the max bytes per requested
		if(!TableModelUtils.isRequestWithinMaxBytePerRequest(models, rowCount, this.maxBytesPerRequest)){
			throw new IllegalArgumentException("Request exceed the maximum number of bytes per request.  Maximum : "+this.maxBytesPerRequest+" bytes");
		}
	}

	public void setMaxBytesPerRequest(int maxBytesPerRequest) {
		this.maxBytesPerRequest = maxBytesPerRequest;
	}

	private void validateFileHandles(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta, String etag) throws IOException,
			NotFoundException {

		List<String> fileHandleColumns = Lists.newArrayList();
		for (ColumnModel cm : models) {
			if (cm.getColumnType() == ColumnType.FILEHANDLEID) {
				fileHandleColumns.add(cm.getId());
			}
		}

		if (fileHandleColumns.isEmpty()) {
			// no filehandles: success!
			return;
		}

		RowSet fileHandlesToCheck = new RowSet();
		fileHandlesToCheck.setEtag(delta.getEtag());
		fileHandlesToCheck.setHeaders(delta.getHeaders());
		fileHandlesToCheck.setTableId(delta.getTableId());
		fileHandlesToCheck.setRows(Lists.newArrayList(delta.getRows()));
		RowSetAccessor fileHandlesToCheckAccessor = TableModelUtils.getRowSetAccessor(fileHandlesToCheck);

		// eliminate all file handles that are owned by current user
		Set<String> ownedFileHandles = Sets.newHashSet();
		Set<String> unownedFileHandles = Sets.newHashSet();

		// first handle all new rows
		List<String> fileHandles = Lists.newLinkedList();
		for (Iterator<RowAccessor> rowIter = fileHandlesToCheckAccessor.getRows().iterator(); rowIter.hasNext();) {
			RowAccessor row = rowIter.next();
			if (TableModelUtils.isNullOrInvalid(row.getRow().getRowId())) {
				getFileHandles(row, fileHandleColumns, user, fileHandles);
				rowIter.remove();
			}
		}

		// check the file handles?
		if (!fileHandles.isEmpty()) {
			authorizationManager.canAccessRawFileHandlesByIds(user, fileHandles, ownedFileHandles, unownedFileHandles);

			if (!unownedFileHandles.isEmpty()) {
				// this is a new row and the user is trying to add a file handle they do not own
				throw new IllegalArgumentException("You cannot add new file ids that you do not own");
			}
		}

		if (Iterables.isEmpty(fileHandlesToCheckAccessor.getRows())) {
			// all new rows and all file handles owned by user or null: success!
			return;
		}

		// now all we have left is rows that are updated

		// collect all file handles
		fileHandles.clear();
		for (RowAccessor row : fileHandlesToCheckAccessor.getRows()) {
			getFileHandles(row, fileHandleColumns, user, fileHandles);
		}
		// check all file handles for access
		authorizationManager.canAccessRawFileHandlesByIds(user, fileHandles, ownedFileHandles, unownedFileHandles);

		for (Iterator<RowAccessor> rowIter = fileHandlesToCheckAccessor.getRows().iterator(); rowIter.hasNext();) {
			RowAccessor row = rowIter.next();
			String unownedFileHandle = checkRowForUnownedFileHandle(user, row, fileHandleColumns, ownedFileHandles, unownedFileHandles);
			if (unownedFileHandle == null) {
				// No unowned file handles, so no need to check previous values
				rowIter.remove();
			}
		}

		if (Iterables.isEmpty(fileHandlesToCheckAccessor.getRows())) {
			// all file handles null or owned by calling user: success!
			return;
		}

		RowSetAccessor latestVersions = tableRowTruthDao.getLatestVersionsWithRowData(tableId, fileHandlesToCheckAccessor.getRowIds(), 0);

		// now we need to check if any of the unowned filehandles are changing with this request
		for (RowAccessor row : fileHandlesToCheckAccessor.getRows()) {
			RowAccessor lastRowVersion = latestVersions.getRow(row.getRow().getRowId());
			for (String fileHandleColumn : fileHandleColumns) {
				String newFileHandleId = row.getCell(fileHandleColumn);
				if (newFileHandleId == null) {
					// erasing a file handle id is always allowed
					continue;
				}
				if (ownedFileHandles.contains(newFileHandleId)) {
					// we already checked. We own this one
					continue;
				}
				String oldFileHandleId = lastRowVersion.getCell(fileHandleColumn);
				if (!oldFileHandleId.equals(newFileHandleId) && !ownedFileHandles.contains(newFileHandleId)) {
					throw new IllegalArgumentException("You cannot change a file id to a new file id that you do not own: rowId="
							+ row.getRow().getRowId() + ", old file handle=" + oldFileHandleId + ", new file handle=" + newFileHandleId);
				}
			}
		}
	}

	private void getFileHandles(RowAccessor row, List<String> fileHandleColumns, UserInfo user, List<String> fileHandles) {
		for (String fileHandleColumn : fileHandleColumns) {
			String fileHandleId = row.getCell(fileHandleColumn);
			if (fileHandleId != null) {
				fileHandles.add(fileHandleId);
			}
		}
	}

	private String checkRowForUnownedFileHandle(UserInfo userInfo, RowAccessor row, List<String> fileHandleColumns,
			Set<String> ownedFileHandles, Set<String> unownedFileHandles) throws NotFoundException {
		for (String fileHandleColumn : fileHandleColumns) {
			String fileHandleId = row.getCell(fileHandleColumn);
			if (fileHandleId == null) {
				// erasing a file handle id is always allowed
				continue;
			}
			if (ownedFileHandles.contains(fileHandleId)) {
				// We own this one
				continue;
			}
			if (unownedFileHandles.contains(fileHandleId)) {
				// We don't own this one.
				return fileHandleId;
			}
			// somehow didn't show up in owned or unowned. Run separate access check
			if (authorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId).getAuthorized()) {
				continue;
			} else {
				return fileHandleId;
			}
		}
		return null;
	}
	
	/**
	 * Thows an exception if the table feature is disabled.
	 */
	public void validateFeatureEnabled(){
		if(!StackConfiguration.singleton().getTableEnabled()){
			throw new IllegalStateException("This method cannot be called when the table feature is disabled.");
		}
	}

	@Override
	public Long getMaxRowsPerPage(List<ColumnModel> models) {
		// Calculate the size
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(models);
		if(maxRowSizeBytes < 1) return null;
		return (long) (this.maxBytesPerRequest/maxRowSizeBytes);
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
}
