package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.manager.AuthorizationManager;
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
	 * @param tableReadTimeoutMS
	 */
	public void setTableReadTimeoutMS(long tableReadTimeoutMS) {
		this.tableReadTimeoutMS = tableReadTimeoutMS;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public RowReferenceSet appendRows(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta) throws DatastoreException, NotFoundException, IOException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		if(models == null) throw new IllegalArgumentException("Models cannot be null");
		if(delta == null) throw new IllegalArgumentException("RowSet cannot be null");

		// Validate the user has permission to edit the table
		if(!authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException("User does not have permission to update TableEntity: "+tableId);
		}
		// Let the DAO do the rest of the work.
		RowReferenceSet rrs = tableRowTruthDao.appendRowSetToTable(user.getId().toString(), tableId, models, delta);
		// The table has change so we must reset the state.
		tableStatusDAO.resetTableStatusToProcessing(tableId);
		return rrs;
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
			String resetToken) throws ConflictingUpdateException,
			NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken);
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
	public RowSet query(UserInfo user, String sql, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(sql == null) throw new IllegalArgumentException("Query SQL string cannot be null");
		// First parse the SQL
		QuerySpecification model = parserQuery(sql);
		String tableId = SqlElementUntils.getTableId(model);
		// Validate the user has read access on this object
		if(!authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ)){
			throw new UnauthorizedException("User does not have READ permission on: "+tableId);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		Map<String, Long> columnNameToIdMap = TableModelUtils.createColumnNameToIdMap(columnModels);
		final SqlQuery query = new SqlQuery(model, columnNameToIdMap);
		// If this is a consistent read then we need a read lock
		if(isConsistent){
			// A consistent query is only run if the table index is available and up-to-date
			// with the table state.  A read-lock on the index will be held while the query is run.
			return runConistentQuery(tableId, query);
		}else{
			// This path queries the table index regardless of the state of the index and without a
			// read-lock.
			return query(query);
		}
	}
	
	/**
	 * 
	 * @param tableId
	 * @param query
	 * @return
	 * @throws TableUnavilableException
	 */
	private RowSet runConistentQuery(final String tableId, final SqlQuery query) throws TableUnavilableException{
		try {
			// Run with a read lock.
			return tryRunWithTableNonexclusiveLock(tableId, tableReadTimeoutMS, new Callable<RowSet>() {
				@Override
				public RowSet call() throws Exception {
					// We can only run this query if the table  is available.
					TableStatus status = getTableStatus(tableId);
					if(TableState.AVAILABLE.equals(status)){
						// When the table is not available, we communicate the current status of the 
						// table in this exception.
						throw new TableUnavilableException(status);
					}
					// We can only run this 
					return query(query);
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

}
