package org.sagebionetworks.repo.manager.table;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.sagebionetworks.common.util.progress.AutoProgressingCallable;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreRunner;
import org.springframework.beans.factory.annotation.Autowired;

public class TableManagerSupportImpl implements TableManagerSupport {
	
	public static final long TABLE_PROCESSING_TIMEOUT_MS = 1000*60*10; // 10 mins
	
	public static final long AUTO_PROGRESS_FREQUENCY_MS = 5*1000; // 5 seconds

	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	TimeoutUtils timeoutUtils;
	@Autowired
	TransactionalMessenger transactionalMessenger;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	ColumnModelDAO columnModelDao;
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	TableRowTruthDAO tableTruthDao;
	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	WriteReadSemaphoreRunner writeReadSemaphoreRunner;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	ExecutorService tableSupportExecutorService;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableRowManager#getTableStatusOrCreateIfNotExists(java.lang.String)
	 */
	@RequiresNewReadCommitted
	@Override
	public TableStatus getTableStatusOrCreateIfNotExists(String tableId) throws NotFoundException {
		try {
			TableStatus status = tableStatusDAO.getTableStatus(tableId);
			if(!TableState.AVAILABLE.equals(status.getState())){
				// Processing or Failed.
				// Is progress being made?
				if(timeoutUtils.hasExpired(TABLE_PROCESSING_TIMEOUT_MS, status.getChangedOn().getTime())){
					// progress has not been made so trigger another update
					return setTableToProcessingAndTriggerUpdate(tableId);
				}else{
					// progress has been made so just return the status
					return status;
				}
			}
			// Status is Available, is the index synchronized with the truth?
			if(isIndexSynchronizedWithTruth(tableId)){
				// Available and synchronized.
				return status;
			}else{
				// Available but not synchronized, so change the state to processing.
				return setTableToProcessingAndTriggerUpdate(tableId);
			}
			
		} catch (NotFoundException e) {
			// make sure the table exists
			if (!isTableAvailable(tableId)) {
				throw new NotFoundException("Table " + tableId + " not found");
			}
			return setTableToProcessingAndTriggerUpdate(tableId);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#setTableToProcessingAndTriggerUpdate(java.lang.String)
	 */
	@WriteTransactionReadCommitted
	@Override
	public TableStatus setTableToProcessingAndTriggerUpdate(String tableId) {
		ValidateArgument.required(tableId, "tableId");
		// lookup the table type.
		ObjectType tableType = getTableType(tableId);
		// we get here, if the index for this table is not (yet?) being build. We need to kick off the
		// building of the index and report the table as unavailable
		String token = tableStatusDAO.resetTableStatusToProcessing(tableId);
		// notify all listeners.
		transactionalMessenger.sendMessageAfterCommit(tableId, tableType, token, ChangeType.UPDATE);
		// status should exist now
		return tableStatusDAO.getTableStatus(tableId);
	}

	@RequiresNewReadCommitted
	@Override
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException,
			NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(tableId, resetToken, tableChangeEtag);
	}

	@RequiresNewReadCommitted
	@Override
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, Exception error)
			throws ConflictingUpdateException, NotFoundException {
		String errorMessage = error.getMessage();
		StringWriter writer = new StringWriter();
		error.printStackTrace(new PrintWriter(writer));
		String errorDetails = writer.toString();
		tableStatusDAO.attemptToSetTableStatusToFailed(tableId, resetToken, errorMessage, errorDetails);
	}

	@RequiresNewReadCommitted
	@Override
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToUpdateTableProgress(tableId, resetToken, progressMessage, currentProgress, totalProgress);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#startTableProcessing(java.lang.String)
	 */
	@RequiresNewReadCommitted
	@Override
	public String startTableProcessing(String tableId) {
		return tableStatusDAO.resetTableStatusToProcessing(tableId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#isIndexSynchronizedWithTruth(java.lang.String)
	 */
	@Override
	public boolean isIndexSynchronizedWithTruth(String tableId) {
		// MD5 of the table's schema
		String truthSchemaMD5Hex = getSchemaMD5Hex(tableId);
		// get the truth version
		long truthLastVersion = getTableVersion(tableId);
		// compare the truth with the index.
		return this.tableConnectionFactory.getConnection(tableId).doesIndexStateMatch(tableId, truthLastVersion, truthSchemaMD5Hex);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#isIndexWorkRequired(java.lang.String)
	 */
	@Override
	public boolean isIndexWorkRequired(String tableId) {
		// Does the table exist and not in the trash?
		if(!isTableAvailable(tableId)){
			return false;
		}
		// work is needed if the index is out-of-sych.
		if(!isIndexSynchronizedWithTruth(tableId)){
			return true;
		}
		// work is needed if the current state is processing.
		TableStatus status = tableStatusDAO.getTableStatus(tableId);
		return TableState.PROCESSING.equals(status.getState());
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#setTableDeleted(java.lang.String)
	 */
	@Override
	public void setTableDeleted(String deletedId, ObjectType tableType) {
		transactionalMessenger.sendDeleteMessageAfterCommit(deletedId, tableType);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#getSchemaMD5Hex(java.lang.String)
	 */
	@Override
	public String getSchemaMD5Hex(String tableId) {
		List<ColumnModel> truthSchema = columnModelDao
				.getColumnModelsForObject(tableId);
		return TableModelUtils.createSchemaMD5HexCM(truthSchema);
	}

	/**
	 * Get the version of the last change applied to a table entity.
	 * 
	 * @param tableId
	 * @return returns -1 if there are no changes applied to the table.
	 */
	long getVersionOfLastTableEntityChange(String tableId) {
		TableRowChange change = tableTruthDao.getLastTableRowChange(tableId);
		if (change != null) {
			return change.getRowVersion();
		} else {
			return -1;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#isTableAvailable(java.lang.String)
	 */
	@Override
	public boolean isTableAvailable(String tableId) {
		return nodeDao.isNodeAvailable(KeyFactory.stringToKey(tableId));
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#getTableType(java.lang.String)
	 */
	@Override
	public ObjectType getTableType(String tableId) {
		EntityType type = getTableEntityType(tableId);
		return getObjectTypeForEntityType(type);
	}
	
	/**
	 * Convert an EntityType to an Object.
	 * @param type
	 * @return
	 */
	public static ObjectType getObjectTypeForEntityType(EntityType type) {
		switch (type) {
		case table:
			return ObjectType.TABLE;
		case entityview:
			return ObjectType.ENTITY_VIEW;
		default:
			throw new IllegalArgumentException("unknown table type: " + type);
		}

	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#calculateFileViewCRC32(java.lang.String)
	 */
	@Override
	public Long calculateFileViewCRC32(String tableId) {
		// Start with all container IDs that define the view's scope
		Set<Long> viewContainers = getAllContainerIdsForViewScope(tableId);
		ViewType type = getViewType(tableId);
		TableIndexDAO indexDao = this.tableConnectionFactory.getConnection(tableId);
		return indexDao.calculateCRC32ofEntityReplicationScope(type, viewContainers);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#getScopeContainerCount(java.util.Set)
	 */
	@Override
	public int getScopeContainerCount(Set<Long> scopeIds) {
		if(scopeIds == null || scopeIds.isEmpty()){
			return 0;
		}
		Set<Long> scopeSet = getAllContainerIdsForScope(scopeIds);
		return scopeSet.size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableViewTruthManager#getAllContainerIdsForViewScope(java.lang.String)
	 */
	@Override
	public Set<Long> getAllContainerIdsForViewScope(String viewIdString) {
		ValidateArgument.required(viewIdString, "viewId");
		Long viewId = KeyFactory.stringToKey(viewIdString);
		// Lookup the scope for this view.
		Set<Long> scope = viewScopeDao.getViewScope(viewId);
		return getAllContainerIdsForScope(scope);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#getAllContainerIdsForScope(java.util.Set)
	 */
	@Override
	public Set<Long> getAllContainerIdsForScope(Set<Long> scope) {
		// Add all containers from the given scope
		Set<Long> allContainersInScope = new HashSet<Long>(scope);
		for(Long container: scope){
			List<Long> containers = nodeDao.getAllContainerIds(container);
			allContainersInScope.addAll(containers);
		}
		return allContainersInScope;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#getTableVersion(java.lang.String)
	 */
	@Override
	public long getTableVersion(String tableId) {
		// Determine the type of able
		ObjectType type = getTableType(tableId);
		switch (type) {
		case TABLE:
			// For TableEntity the version of the last change set is used.
			return getVersionOfLastTableEntityChange(tableId);
		case ENTITY_VIEW:
			// For FileViews the CRC of all files in the view is used.
			return calculateFileViewCRC32(tableId);
		default:
			throw new IllegalArgumentException("unknown table type: " + type);
		}
	}
	
	@Override
	public <R> R tryRunWithTableExclusiveLock(ProgressCallback<Void> callback,
			String tableId, int timeoutSec, ProgressingCallable<R, Void> callable)
			throws Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithWriteLock(callback, key, timeoutSec, callable);
	}

	@Override
	public <R, T> R tryRunWithTableNonexclusiveLock(
			ProgressCallback<T> callback, String tableId, int lockTimeoutSec,
			ProgressingCallable<R, T> callable) throws Exception
			{
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithReadLock(callback, key, lockTimeoutSec, callable);
	}
	
	@Override
	public EntityType validateTableReadAccess(UserInfo userInfo, String tableId)
			throws UnauthorizedException, DatastoreException, NotFoundException {
		// They must have read permission to access table content.
		AuthorizationManagerUtil
				.checkAuthorizationAndThrowException(authorizationManager
						.canAccess(userInfo, tableId, ObjectType.ENTITY,
								ACCESS_TYPE.READ));

		// Lookup the entity type for this table.
		EntityType entityTpe = getTableEntityType(tableId);
		ObjectType type = getObjectTypeForEntityType(entityTpe);
		// User must have the download permission to read from a TableEntity.
		if(ObjectType.TABLE.equals(type)){
			// And they must have download permission to access table content.
			AuthorizationManagerUtil
					.checkAuthorizationAndThrowException(authorizationManager
							.canAccess(userInfo, tableId, ObjectType.ENTITY,
									ACCESS_TYPE.DOWNLOAD));
		}
		return entityTpe;
	}
	
	@Override
	public void validateTableWriteAccess(UserInfo userInfo, String tableId)
			throws UnauthorizedException, DatastoreException, NotFoundException {
		// They must have update permission to change table content
		AuthorizationManagerUtil
				.checkAuthorizationAndThrowException(authorizationManager
						.canAccess(userInfo, tableId, ObjectType.ENTITY,
								ACCESS_TYPE.UPDATE));
		// And they must have upload permission to change table content.
		AuthorizationManagerUtil
				.checkAuthorizationAndThrowException(authorizationManager
						.canAccess(userInfo, tableId, ObjectType.ENTITY,
								ACCESS_TYPE.UPLOAD));
	}
	
	@Override
	public List<ColumnModel> getColumnModelsForTable(String tableId) throws DatastoreException, NotFoundException {
		return columnModelDao.getColumnModelsForObject(tableId);
	}

	@Override
	public void lockOnTableId(String tableId) {
		columnModelDao.lockOnOwner(tableId);
	}
	
	@Override
	public ColumnModel getColumnModel(EntityField field){
		ValidateArgument.required(field, "field");
		return columnModelDao.createColumnModel(field.getColumnModel());
	}
	
	@Override
	public List<ColumnModel> getColumnModels(EntityField... fields) {
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(EntityField field: fields){
			results.add(getColumnModel(field));
		}
		return results;
	}


	@Override
	public Set<Long> getAccessibleBenefactors(UserInfo user,
			Set<Long> benefactorIds) {
		return authorizationManager.getAccessibleBenefactors(user, benefactorIds);
	}

	@Override
	public EntityType getTableEntityType(String tableId) {
		return nodeDao.getNodeTypeById(tableId);
	}
	
	@Override
	public ViewType getViewType(String tableId){
		return viewScopeDao.getViewType(KeyFactory.stringToKey(tableId));
	}

	@Override
	public List<ColumnModel> getDefaultTableViewColumns(ViewType viewType) {
		if(!ViewType.file.equals(viewType)){
			throw new IllegalArgumentException("Unsupported type: "+viewType);
		}
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		for(EntityField field: EntityField.values()){
			list.add(getColumnModel(field));
		}
		return list;
	}

	@Override
	public Set<Long> getEntityPath(String entityId) {
		List<EntityHeader> headers = nodeDao.getEntityPath(entityId);
		Set<Long> results = new HashSet<Long>(headers.size());
		for(EntityHeader header: headers){
			results.add(KeyFactory.stringToKey(header.getId()));
		}
		return results;
	}


	@Override
	public <R> R callWithAutoProgress(ProgressCallback<Void> callback, Callable<R> callable) throws Exception {
		AutoProgressingCallable<R> auto = new AutoProgressingCallable<R>(
				tableSupportExecutorService, callable, AUTO_PROGRESS_FREQUENCY_MS);
		return auto.call(callback);
	}

	@Override
	public List<ColumnModel> getColumnModel(List<String> ids, boolean keepOrder) {
		return columnModelDao.getColumnModel(ids, keepOrder);
	}

	@WriteTransactionReadCommitted
	@Override
	public void rebuildTable(UserInfo userInfo, String tableId) {
		if (!userInfo.isAdmin())
			throw new UnauthorizedException("Only an administrator may access this service.");
		// purge
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		if (indexDao != null) {
			indexDao.deleteTable(tableId);
			indexDao.deleteSecondaryTables(tableId);
		}
		String resetToken = tableStatusDAO.resetTableStatusToProcessing(tableId);
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(getTableType(tableId));
		message.setObjectId(KeyFactory.stringToKey(tableId).toString());
		message.setObjectEtag(resetToken);
		transactionalMessenger.sendMessageAfterCommit(message);
	}
}
