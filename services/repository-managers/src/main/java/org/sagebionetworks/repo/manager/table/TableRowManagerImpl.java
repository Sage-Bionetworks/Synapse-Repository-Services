package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisherImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreRunner;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TableRowManagerImpl implements TableRowManager {
	
	static private Log log = LogFactory.getLog(RepositoryMessagePublisherImpl.class);
	
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
	int maxRowsPerBatch;
	/**
	 * Injected via spring
	 * @param tableReadTimeoutMS
	 */
	public void setTableReadTimeoutMS(long tableReadTimeoutMS) {
		this.tableReadTimeoutMS = tableReadTimeoutMS;
	}

	/**
	 * Injected via spring.
	 * @param maxRowsPerFile
	 */
	public void setMaxRowsPerBatch(int maxRowsPerFile){
		this.maxRowsPerBatch = maxRowsPerFile;
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
		appendRowsAsStream(user, tableId, models, delta.getRows().iterator(), delta.getEtag(), results);
		return results;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> models, Iterator<Row> rowStream, String etag, RowReferenceSet results) throws DatastoreException, NotFoundException, IOException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		if(models == null) throw new IllegalArgumentException("Models cannot be null");
		// Validate the user has permission to edit the table
		if(!authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException("User does not have permission to update TableEntity: "+tableId);
		}
		List<String> headers = TableModelUtils.getHeaders(models);
		List<Row> batch = new LinkedList<Row>();
		int count = 0;
		while(rowStream.hasNext()){
			batch.add(rowStream.next());
			if(batch.size() >= maxRowsPerBatch){
				// Send this batch and keep the etag.
				 etag = appendBatchOfRowsToTable(user, tableId, models, etag, results, headers, batch);
				// Clear the batch
				count += batch.size();
				batch.clear();
				log.info("Appended: "+count+" rows to table: "+tableId);
			}
		}
		// Send the last batch is there are any rows
		if(!batch.isEmpty()){
			 etag = appendBatchOfRowsToTable(user, tableId, models, etag, results, headers, batch);
		}
		// The table has change so we must reset the state.
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		return etag;
	}

	/**
	 * Append a batch of rows to a table.
	 * @param tableId
	 * @param models
	 * @param etag
	 * @param results
	 * @param headers
	 * @param batch
	 * @return
	 * @throws IOException
	 */
	private String appendBatchOfRowsToTable(UserInfo user, String tableId,
			List<ColumnModel> models, String etag, RowReferenceSet results,
			List<String> headers, List<Row> batch) throws IOException {
		// Send the batch
		RowSet delta = new RowSet();
		delta.setEtag(etag);
		delta.setHeaders(headers);
		delta.setRows(batch);
		delta.setTableId(tableId);
		RowReferenceSet rrs = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, delta);
		if(results != null){
			results.setEtag(rrs.getEtag());
			results.setHeaders(headers);
			results.setTableId(tableId);
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
	public List<TableRowChange> listRowSetsKeysForTable(String tableId) {
		return tableRowTruthDao.listRowSetsKeysForTable(tableId);
	}

	@Override
	public RowSet getRowSet(String tableId, Long rowVersion) throws IOException, NotFoundException {
		return tableRowTruthDao.getRowSet(tableId, rowVersion);
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
	public TableStatus getTableStatus(String tableId) throws NotFoundException {
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
	public RowSet query(UserInfo user, String sql, boolean isConsistent, boolean countOnly) throws DatastoreException, NotFoundException, TableUnavilableException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(sql == null) throw new IllegalArgumentException("Query SQL string cannot be null");
		// First parse the SQL
		QuerySpecification model = parserQuery(sql);
		// Do they want use to convert it to a count query?
		if(countOnly){
			model = convertToCountQuery(model);
		}
		String tableId = SqlElementUntils.getTableId(model);
		// Validate the user has read access on this object
		if(!authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)){
			throw new UnauthorizedException("User does not have READ permission on: "+tableId);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		Map<String, Long> columnNameToIdMap = TableModelUtils.createColumnNameToIdMap(columnModels);
		final SqlQuery query = new SqlQuery(model, columnNameToIdMap);
		// Does this table exist?
		if(columnModels == null | columnModels.isEmpty()){
			// there are no columns for this table so the table does not actually exist.
			// for this case the caller expects an empty result set.  See PLFM-2636
			RowSet emptyResults = new RowSet();
			emptyResults.setTableId(query.getTableId());
			return emptyResults;
		}
		// validate the size
		validateQuerySize(query, columnModels, this.maxBytesPerRequest);
		// If this is a consistent read then we need a read lock
		if(isConsistent){
			// A consistent query is only run if the table index is available and up-to-date
			// with the table state.  A read-lock on the index will be held while the query is run.
			return runConsistentQuery(tableId, query);
		}else{
			// This path queries the table index regardless of the state of the index and without a
			// read-lock.
			return query(query);
		}
	}
	

	/**
	 * Validate that a query result will be under the max size.
	 * 
	 * @param query
	 * @param columnModels
	 * @param maxBytePerRequest
	 */
	public static void validateQuerySize(SqlQuery query, List<ColumnModel> columnModels, int maxBytePerRequest){
		Long limit = null;
		if(query.getModel().getTableExpression().getPagination() != null){
			limit = query.getModel().getTableExpression().getPagination().getLimit();
		}
		Map<Long, ColumnModel> columIdToModelMap = TableModelUtils.createIDtoColumnModelMap(columnModels);
		// What are the select columns?
		List<Long> selectColumns = query.getSelectColumnIds();
		if(!selectColumns.isEmpty()){
			List<ColumnModel> seletModels = new LinkedList<ColumnModel>();
			for(Long id: selectColumns){
				ColumnModel cm = columIdToModelMap.get(id);
				seletModels.add(cm);
			}
			// First make sure we have a limit
			if(limit == null){
				throw new IllegalArgumentException("Request exceed the maximum number of bytes per request because a LIMIT was not included in the query.");
			}
			// Validate the request is under the max bytes per requested
			if(!TableModelUtils.isRequestWithinMaxBytePerRequest(seletModels, limit.intValue(), maxBytePerRequest)){
				throw new IllegalArgumentException("Request exceed the maximum number of bytes per request.  Maximum : "+maxBytePerRequest+" bytes");
			}
		}
	}
	
	/**
	 * 
	 * @param tableId
	 * @param query
	 * @return
	 * @throws TableUnavilableException
	 */
	private RowSet runConsistentQuery(final String tableId, final SqlQuery query) throws TableUnavilableException{
		try {
			// Run with a read lock.
			return tryRunWithTableNonexclusiveLock(tableId, tableReadTimeoutMS, new Callable<RowSet>() {
				@Override
				public RowSet call() throws Exception {
					// We can only run this query if the table  is available.
					TableStatus status = getTableStatus(tableId);
					if(!TableState.AVAILABLE.equals(status.getState())){
						// When the table is not available, we communicate the current status of the 
						// table in this exception.
						throw new TableUnavilableException(status);
					}
					// We can only run this 
					RowSet results =  query(query);
					results.setEtag(status.getLastTableChangeEtag());
					return results;
				}
			});
		} catch (LockUnavilableException e) {
			TableUnavilableException e1 = createTableUnavilableException(tableId);
			throw e1;
		} catch(TableUnavilableException e){
			throw e;
		} catch (Exception e) {
			// All other exception are converted to generic datastore.
			throw new DatastoreException(e);
		}
	}
	
	TableUnavilableException createTableUnavilableException(String tableId){
		// When this occurs we need to lookup the status of the table and pass that to the caller
		try {
			TableStatus status = this.getTableStatus(tableId);
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
	
	/**
	 * Convert a query to a count query.
	 * @param model
	 * @return
	 */
	private QuerySpecification convertToCountQuery(QuerySpecification model){
		try {
			return SqlElementUntils.convertToCountQuery(model);
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

}
