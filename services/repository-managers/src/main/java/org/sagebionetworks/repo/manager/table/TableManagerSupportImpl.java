package org.sagebionetworks.repo.manager.table;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModelMapper;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewSnapshotDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableManagerSupportImpl implements TableManagerSupport {

	public static final long TABLE_PROCESSING_TIMEOUT_MS = 1000 * 60 * 10; // 10 mins

	@Autowired
	private TableStatusDAO tableStatusDAO;
	@Autowired
	private TimeoutUtils timeoutUtils;
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	@Autowired
	private ConnectionFactory tableConnectionFactory;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private TableRowTruthDAO tableTruthDao;
	@Autowired
	private ViewScopeDao viewScopeDao;
	@Autowired
	private WriteReadSemaphoreRunner writeReadSemaphoreRunner;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private ViewSnapshotDao viewSnapshotDao;
	@Autowired
	private MetadataIndexProviderFactory metadataIndexProviderFactory;
	@Autowired
	private DefaultColumnModelMapper defaultColumnMapper;
	@Autowired
	private MaterializedViewDao materializedViewDao;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableRowManager#
	 * getTableStatusOrCreateIfNotExists(java.lang.String)
	 */
	@NewWriteTransaction
	@Override
	public TableStatus getTableStatusOrCreateIfNotExists(IdAndVersion idAndVersion) throws NotFoundException {
		try {
			TableStatus status = tableStatusDAO.getTableStatus(idAndVersion);
			if (!TableState.AVAILABLE.equals(status.getState())) {
				// Processing or Failed.
				// Is progress being made?
				if (timeoutUtils.hasExpired(TABLE_PROCESSING_TIMEOUT_MS, status.getChangedOn().getTime())) {
					// progress has not been made so trigger another update
					return setTableToProcessingAndTriggerUpdate(idAndVersion);
				} else {
					// progress has been made so just return the status
					return status;
				}
			}
			// Status is Available, is the index synchronized with the truth?
			if (isIndexSynchronizedWithTruth(idAndVersion)) {
				// Send an asynchronous signal that there was activity on this table/view
				sendAsynchronousActivitySignal(idAndVersion);
				// Available and synchronized.
				return status;
			} else {
				// Available but not synchronized, so change the state to processing.
				return setTableToProcessingAndTriggerUpdate(idAndVersion);
			}

		} catch (NotFoundException e) {
			// make sure the table exists
			if (!isTableAvailable(idAndVersion)) {
				throw new NotFoundException("Table " + idAndVersion + " not found");
			}
			return setTableToProcessingAndTriggerUpdate(idAndVersion);
		}
	}

	public void sendAsynchronousActivitySignal(IdAndVersion idAndVersion) {
		// lookup the table type.
		ObjectType tableType = getTableObjectType(idAndVersion);

		// Currently we only signal views
		if (ObjectType.ENTITY_VIEW.equals(tableType)) {
			// notify all listeners.
			transactionalMessenger
					.sendMessageAfterCommit(new MessageToSend().withObjectId(idAndVersion.getId().toString())
							.withObjectVersion(idAndVersion.getVersion().orElse(null)).withObjectType(tableType)
							.withChangeType(ChangeType.UPDATE));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableManagerSupport#
	 * setTableToProcessingAndTriggerUpdate(java.lang.String)
	 */
	@WriteTransaction
	@Override
	public TableStatus setTableToProcessingAndTriggerUpdate(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		// lookup the table type.
		ObjectType tableType = getTableObjectType(idAndVersion);
		// we get here, if the index for this table is not (yet?) being build. We need
		// to kick off the
		// building of the index and report the table as unavailable
		tableStatusDAO.resetTableStatusToProcessing(idAndVersion);
		// notify all listeners.
		transactionalMessenger.sendMessageAfterCommit(new MessageToSend().withObjectId(idAndVersion.getId().toString())
				.withObjectVersion(idAndVersion.getVersion().orElse(null)).withObjectType(tableType)
				.withChangeType(ChangeType.UPDATE));
		// status should exist now
		return tableStatusDAO.getTableStatus(idAndVersion);
	}

	@NewWriteTransaction
	@Override
	public void attemptToSetTableStatusToAvailable(IdAndVersion idAndVersion, String resetToken, String tableChangeEtag)
			throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToSetTableStatusToAvailable(idAndVersion, resetToken, tableChangeEtag);
	}

	@NewWriteTransaction
	@Override
	public void attemptToSetTableStatusToFailed(IdAndVersion idAndVersion, Exception error)
			throws ConflictingUpdateException, NotFoundException {
		String errorMessage = error.getMessage();
		StringWriter writer = new StringWriter();
		error.printStackTrace(new PrintWriter(writer));
		String errorDetails = writer.toString();
		tableStatusDAO.attemptToSetTableStatusToFailed(idAndVersion, errorMessage, errorDetails);
	}

	@NewWriteTransaction
	@Override
	public void attemptToUpdateTableProgress(IdAndVersion idAndVersion, String resetToken, String progressMessage,
			Long currentProgress, Long totalProgress) throws ConflictingUpdateException, NotFoundException {
		tableStatusDAO.attemptToUpdateTableProgress(idAndVersion, resetToken, progressMessage, currentProgress,
				totalProgress);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#
	 * startTableProcessing(java.lang.String)
	 */
	@NewWriteTransaction
	@Override
	public String startTableProcessing(IdAndVersion idAndVersion) {
		return tableStatusDAO.resetTableStatusToProcessing(idAndVersion);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#
	 * isIndexSynchronizedWithTruth(java.lang.String)
	 */
	@Override
	public boolean isIndexSynchronizedWithTruth(IdAndVersion idAndVersion) {
		// MD5 of the table's schema
		String truthSchemaMD5Hex = getSchemaMD5Hex(idAndVersion);
		// get the truth version
		long truthLastVersion = getTableVersion(idAndVersion);
		// get the search flag for the node
		boolean truthSearchEnabled = isTableSearchEnabled(idAndVersion);
		// compare the truth with the index.
		return tableConnectionFactory.getConnection(idAndVersion).doesIndexStateMatch(idAndVersion, truthLastVersion,
				truthSchemaMD5Hex, truthSearchEnabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableStatusManager#isIndexWorkRequired
	 * (java.lang.String)
	 */
	@Override
	public boolean isIndexWorkRequired(IdAndVersion idAndVersion) {
		// Does the table exist and not in the trash?
		if (!isTableAvailable(idAndVersion)) {
			return false;
		}
		// work is needed if the index is out-of-sych.
		if (!isIndexSynchronizedWithTruth(idAndVersion)) {
			return true;
		}
		// work is needed if the current state is processing.
		Optional<TableState> optional = tableStatusDAO.getTableStatusState(idAndVersion);
		if (!optional.isPresent()) {
			// there is no state for this table so work is required.
			return true;
		}
		return TableState.PROCESSING.equals(optional.get());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableStatusManager#setTableDeleted(
	 * java.lang.String)
	 */
	@Override
	public void setTableDeleted(IdAndVersion idAndVersion, ObjectType tableType) {
		transactionalMessenger.sendDeleteMessageAfterCommit(idAndVersion.getId().toString(), tableType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableManagerSupport#getSchemaMD5Hex(
	 * java.lang.String)
	 */
	@Override
	public String getSchemaMD5Hex(IdAndVersion idAndVersion) {
		List<String> columnIds = columnModelManager.getColumnIdsForTable(idAndVersion);
		return TableModelUtils.createSchemaMD5Hex(columnIds);
	}

	@Override
	public Optional<Long> getLastTableChangeNumber(IdAndVersion idAndVersion) {
		if (idAndVersion.getVersion().isPresent()) {
			return this.tableTruthDao.getLastTableChangeNumber(idAndVersion.getId(), idAndVersion.getVersion().get());
		} else {
			return this.tableTruthDao.getLastTableChangeNumber(idAndVersion.getId());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableManagerSupport#isTableAvailable(
	 * java.lang.String)
	 */
	@Override
	public boolean isTableAvailable(IdAndVersion idAndVersion) {
		return nodeDao.isNodeAvailable(idAndVersion.getId());
	}

	@Override
	public boolean doesTableExist(IdAndVersion idAndVersion) {
		return nodeDao.doesNodeExist(idAndVersion.getId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableManagerSupport#getTableType(java.
	 * lang.String)
	 */
	@Override
	public ObjectType getTableObjectType(IdAndVersion idAndVersion) {
		return getTableType(idAndVersion).getObjectType();
	}

	@Override
	public Long getViewStateNumber(IdAndVersion idAndVersion) {
		if (idAndVersion.getVersion().isPresent()) {
			// The ID of the snapshot is used for this case.
			return viewSnapshotDao.getSnapshotId(idAndVersion);
		} else {
			/*
			 * By returning the version already associated with the view index, we ensure
			 * this call will not trigger a view to be rebuilt.
			 */
			TableIndexDAO indexDao = this.tableConnectionFactory.getConnection(idAndVersion);
			return indexDao.getMaxCurrentCompleteVersionForTable(idAndVersion);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableManagerSupport#getTableVersion(
	 * java.lang.String)
	 */
	@Override
	public long getTableVersion(IdAndVersion idAndVersion) {
		// Determine the type of able
		ObjectType type = getTableObjectType(idAndVersion);
		switch (type) {
		case TABLE:
			// For TableEntity the version of the last change set is used.
			Optional<Long> value = getLastTableChangeNumber(idAndVersion);
			return value.orElse(-1L);
		case ENTITY_VIEW:
		case MATERIALIZED_VIEW:
			return getViewStateNumber(idAndVersion);
		default:
			throw new IllegalArgumentException("unknown table type: " + type);
		}
	}

	@Override
	public <R> R tryRunWithTableExclusiveLock(ProgressCallback callback, IdAndVersion tableId,
			ProgressingCallable<R> callable) throws Exception {
		String key = TableModelUtils.getTableSemaphoreKey(tableId);
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithWriteLock(callback, key, callable);
	}

	@Override
	public <R> R tryRunWithTableExclusiveLock(ProgressCallback callback, String key, ProgressingCallable<R> runner)
			throws Exception {
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithWriteLock(callback, key, runner);
	}

	@Override
	public <R> R tryRunWithTableNonExclusiveLock(ProgressCallback callback, ProgressingCallable<R> runner,
			String... keys) throws Exception {
		return writeReadSemaphoreRunner.tryRunWithReadLock(callback, runner, keys);
	}

	@Override
	public <R> R tryRunWithTableNonExclusiveLock(ProgressCallback callback, ProgressingCallable<R> callable,
			IdAndVersion... tableIds) throws Exception {
		ValidateArgument.required(tableIds, "TableIds");
		List<String> keys = Arrays.stream(tableIds).map(i -> TableModelUtils.getTableSemaphoreKey(i))
				.collect(Collectors.toList());
		// The semaphore runner does all of the lock work.
		return writeReadSemaphoreRunner.tryRunWithReadLock(callback, callable, keys.toArray(new String[keys.size()]));
	}

	@Override
	public void validateTableReadAccess(UserInfo userInfo, IndexDescription indexDescription)
			throws UnauthorizedException, DatastoreException, NotFoundException {
		// They must have read permission to access table content.
		authorizationManager.canAccess(userInfo, indexDescription.getIdAndVersion().getId().toString(),
				ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		// User must have the download permission to read from a TableEntity.
		if (EntityType.table.equals(indexDescription.getTableType())) {
			// And they must have download permission to access table content.
			authorizationManager.canAccess(userInfo, indexDescription.getIdAndVersion().getId().toString(),
					ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}
		// must also have access to each dependency
		for (IndexDescription dependency : indexDescription.getDependencies()) {
			validateTableReadAccess(userInfo, dependency);
		}
	}

	@Override
	public void validateTableWriteAccess(UserInfo userInfo, IdAndVersion idAndVersion)
			throws UnauthorizedException, DatastoreException, NotFoundException {
		// They must have update permission to change table content
		authorizationManager.canAccess(userInfo, idAndVersion.getId().toString(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
	}

	@Override
	public Set<Long> getAccessibleBenefactors(UserInfo user, ObjectType benefactorType, Set<Long> benefactorIds) {
		return authorizationManager.getAccessibleBenefactors(user, benefactorType, benefactorIds);
	}

	@Override
	public TableType getTableType(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		EntityType entityType = nodeDao.getNodeTypeById(idAndVersion.getId().toString());
		return TableType.lookupByEntityType(entityType).orElseThrow(() -> new IllegalArgumentException(String.format("%s is not a table or view", idAndVersion.toString())));
	}

	@Override
	public ViewScopeType getViewScopeType(IdAndVersion idAndVersion) {
		return viewScopeDao.getViewScopeType(idAndVersion.getId());
	}

	@Override
	public List<ColumnModel> getDefaultTableViewColumns(ViewEntityType viewEntityType, Long viewTypeMask) {
		if (viewEntityType == null) {
			// To avoid breaking API changes we fallback to ENTITY
			viewEntityType = ViewEntityType.entityview;
		}

		ViewObjectType objectType = ViewObjectType.map(viewEntityType);

		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(objectType);

		DefaultColumnModel defaultColumns = provider.getDefaultColumnModel(viewTypeMask);

		return defaultColumnMapper.map(defaultColumns);
	}

	@WriteTransaction
	@Override
	public void rebuildTable(UserInfo userInfo, IdAndVersion idAndVersion) {
		if (!userInfo.isAdmin())
			throw new UnauthorizedException("Only an administrator may access this service.");
		// purge
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		if (indexDao != null) {
			indexDao.deleteTable(idAndVersion);
		}
		tableStatusDAO.resetTableStatusToProcessing(idAndVersion);
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(getTableObjectType(idAndVersion));
		message.setObjectId(idAndVersion.getId().toString());
		transactionalMessenger.sendMessageAfterCommit(message);
	}

	@Override
	public void validateScope(ViewScopeType scopeType, Set<Long> scopeIds) {
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(scopeType.getObjectType(), "scopeType.objectType");

		ViewObjectType objectType = scopeType.getObjectType();

		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(objectType);
		provider.validateScopeAndType(scopeType.getTypeMask(), scopeIds, TableConstants.MAX_CONTAINERS_PER_VIEW);
	}

	@Override
	public String touchTable(UserInfo user, String tableId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(tableId, "tableId");
		return nodeDao.touch(user.getId(), tableId);
	}

	@Override
	public List<ColumnModel> getTableSchema(IdAndVersion idAndVersion) {
		return columnModelManager.getTableSchema(idAndVersion);
	}

	@Override
	public Optional<TableState> getTableStatusState(IdAndVersion idAndVersion) throws NotFoundException {
		return tableStatusDAO.getTableStatusState(idAndVersion);
	}

	@Override
	public boolean updateChangedOnIfAvailable(IdAndVersion idAndVersion) {
		return tableStatusDAO.updateChangedOnIfAvailable(idAndVersion);
	}

	@Override
	public Date getLastChangedOn(IdAndVersion idAndVersion) {
		return tableStatusDAO.getLastChangedOn(idAndVersion);
	}

	@Override
	public boolean isTableIndexStateInvalid(IdAndVersion idAndVersion) {
		Optional<String> lastChangeEtag = tableStatusDAO.getLastChangeEtag(idAndVersion);
		if (!lastChangeEtag.isPresent()) {
			// since there is no status for this table it is not in an invalid state.
			return false;
		}
		// The table is in an invalid state if the last applied change etag does not
		// match any of the change etags in the tables history.
		return !tableTruthDao.isEtagInTablesChangeHistory(idAndVersion.getId().toString(), lastChangeEtag.get());
	}

	@Override
	public long getTableSchemaCount(IdAndVersion idAndVersion) {
		return columnModelManager.getTableSchemaCount(idAndVersion);
	}

	@Override
	public IndexDescription getIndexDescription(IdAndVersion idAndVersion) {
		TableType type = getTableType(idAndVersion);
		switch (type) {
		case table:
			return new TableIndexDescription(idAndVersion);
		case entityview:
		case dataset:
		case datasetcollection:
		case submissionview:
			return new ViewIndexDescription(idAndVersion, type);
		case materializedview:
			return new MaterializedViewIndexDescription(idAndVersion,
					materializedViewDao.getSourceTablesIds(idAndVersion).stream()
							.map(childId -> getIndexDescription(childId)).collect(Collectors.toList()));
		default:
			throw new IllegalArgumentException("Unexpected type for entity with id " + idAndVersion.toString() + ": "
					+ type + " (expected a table or view type)");
		}
	}

	@Override
	public boolean isTableSearchEnabled(IdAndVersion idAndVersion) {
		return nodeDao.isSearchEnabled(idAndVersion.getId(), idAndVersion.getVersion().orElse(null));
	}

}
