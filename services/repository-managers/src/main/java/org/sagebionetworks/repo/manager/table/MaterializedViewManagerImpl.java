package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterializedViewManagerImpl implements MaterializedViewManager {
	
	private static final Log LOG = LogFactory.getLog(MaterializedViewManagerImpl.class);	
	
	private static final long PAGE_SIZE_LIMIT = 1000;
	
	public static final String DEFAULT_ETAG = "DEFAULT";
	
	final private ColumnModelManager columModelManager;
	final private TableManagerSupport tableManagerSupport;
	final private TableIndexConnectionFactory connectionFactory;
	final private MaterializedViewDao materializedViewDao;

	@Autowired
	public MaterializedViewManagerImpl(ColumnModelManager columModelManager, 
			TableManagerSupport tableManagerSupport, 
			TableIndexConnectionFactory connectionFactory,
			MaterializedViewDao materializedViewDao) {
		this.columModelManager = columModelManager;
		this.tableManagerSupport = tableManagerSupport;
		this.connectionFactory = connectionFactory;
		this.materializedViewDao = materializedViewDao;
	}

	@Override
	public void validate(MaterializedView materializedView) {
		ValidateArgument.required(materializedView, "The materialized view");		
		getQuerySpecification(materializedView.getDefiningSQL());
	}
	
	@Override
	@WriteTransaction
	public void registerSourceTables(IdAndVersion idAndVersion, String definingSql) {
		ValidateArgument.required(idAndVersion, "The id of the materialized view");
		
		QueryExpression query = getQuerySpecification(definingSql);
		
		Set<IdAndVersion> newSourceTables = getSourceTableIds(query);
		Set<IdAndVersion> currentSourceTables = materializedViewDao.getSourceTablesIds(idAndVersion);
		
		if (!newSourceTables.equals(currentSourceTables)) {
			Set<IdAndVersion> toDelete = new HashSet<>(currentSourceTables);
			
			toDelete.removeAll(newSourceTables);
			
			materializedViewDao.deleteSourceTablesIds(idAndVersion, toDelete);
			materializedViewDao.addSourceTablesIds(idAndVersion, newSourceTables);
		}
		
		bindSchemaToView(idAndVersion, query);
		
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Override
	@WriteTransaction
	public void refreshDependentMaterializedViews(IdAndVersion tableId) {
		ValidateArgument.required(tableId, "The tableId");
		
		PaginationIterator<IdAndVersion> idsIterator = new PaginationIterator<IdAndVersion>(
			(long limit, long offset) -> materializedViewDao.getMaterializedViewIdsPage(tableId, limit, offset),
			PAGE_SIZE_LIMIT);
		
		// Sends an update without changing the status of the table, this allows to decide if the table should be built in a temporary space while
		// leaving the original version available for querying
		idsIterator.forEachRemaining(id -> tableManagerSupport.triggerIndexUpdate(id));
		
	}
	
	/**
	 * Extract the schema from the defining query and bind the results to the provided materialized view.
	 * 
	 * @param idAndVersion
	 * @param definingQuery
	 */
	void bindSchemaToView(IdAndVersion idAndVersion, QueryExpression definingQuery) {
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(idAndVersion);
		QueryTranslator sqlQuery = QueryTranslator.builder()
			.sql(definingQuery.toSql())
			.schemaProvider(columModelManager)
			.sqlContext(SqlContext.build)
			.indexDescription(indexDescription)
		.build();
		
		bindSchemaToView(idAndVersion, sqlQuery);
	}
	
	void bindSchemaToView(IdAndVersion idAndVersion, QueryTranslator sqlQuery) {
		// create each column as needed.
		List<String> schemaIds = sqlQuery.getSchemaOfSelect().stream()
			.map(c -> columModelManager.createColumnModel(c).getId())
			.collect(Collectors.toList());
		
		columModelManager.bindColumnsToVersionOfObject(schemaIds, idAndVersion);
	}
	
	static QueryExpression getQuerySpecification(String definingSql) {
		ValidateArgument.requiredNotBlank(definingSql, "The definingSQL of the materialized view");
		try {
			return new TableQueryParser(definingSql).queryExpression();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
		
	static Set<IdAndVersion> getSourceTableIds(QueryExpression query) {
		Set<IdAndVersion> sourceTableIds = new HashSet<>();
		
		for (TableNameCorrelation table : query.createIterable(TableNameCorrelation.class)) {
			sourceTableIds.add(IdAndVersion.parse(table.getTableName().toSql()));
		}
		
		return sourceTableIds;
	}

	@Override
	public List<String> getSchemaIds(IdAndVersion idAndVersion) {
		return columModelManager.getColumnIdsForTable(idAndVersion);
	}

	@Override
	public void deleteViewIndex(IdAndVersion idAndVersion) {
		TableIndexManager indexManager = connectionFactory.connectToTableIndex(idAndVersion);
		indexManager.deleteTableIndex(idAndVersion);
	}

	@Override
	public void createOrUpdateViewIndex(ProgressCallback callback, IdAndVersion idAndVersion) throws Exception {
		
		if (idAndVersion.getVersion().isPresent()) {
			throw new IllegalArgumentException("MaterializedView snapshots not currently supported");
		}
		
		// If the view is in the trash can or if it does not exist we do not build the index
		if (!tableManagerSupport.isTableAvailable(idAndVersion)) {
			return;
		}
		
		// We use a negative id as the temporary id of the view
		IdAndVersion temporaryId = IdAndVersion.newBuilder()
			.setId(-idAndVersion.getId())
			.setVersion(idAndVersion.getVersion().orElse(null)).build();
		
		LockContext lockContext;
		ProgressingCallable<Void> callable;
		
		if (TableState.AVAILABLE == tableManagerSupport.getTableStatusState(idAndVersion).orElse(null)) {
			
			lockContext = new LockContext(ContextType.UpdatingMaterializedView, idAndVersion);
			
			callable = (innerCallback) -> {
				rebuildAvailableViewHoldingTemporaryExclusiveLock(innerCallback, lockContext, idAndVersion, temporaryId);
				return null;	
			};
			
		} else {
			
			lockContext = new LockContext(ContextType.BuildMaterializedView, idAndVersion);
			
			callable = (innerCallback) -> {
				return tableManagerSupport.tryRunWithTableExclusiveLock(innerCallback, lockContext , idAndVersion, (innerInnerCallback) -> {
					createOrRebuildViewHoldingExclusiveLock(innerInnerCallback, lockContext, idAndVersion);
					return null;
				});
			};
			
		}
		
		// We acquire an exclusive lock on the temporary index id, so that we force the serialization of the rebuild/update operations
		// avoiding potential race conditions
		tableManagerSupport.tryRunWithTableExclusiveLock(callback, lockContext, temporaryId, callable);
	}
		
	void createOrRebuildViewHoldingExclusiveLock(ProgressCallback callback, LockContext parentContext, IdAndVersion idAndVersion)
			throws Exception {
		try {
			
			IndexDescription indexDescription = tableManagerSupport.getIndexDescription(idAndVersion);
				
			String definingSql = materializedViewDao.getMaterializedViewDefiningSql(idAndVersion)
					.orElseThrow(() -> new IllegalArgumentException("No defining SQL for: " + idAndVersion.toString()));
			
			QueryTranslator sqlQuery = QueryTranslator.builder()
				.sql(definingSql)
				.schemaProvider(columModelManager)
				.sqlContext(SqlContext.build)
				.indexDescription(indexDescription)
			.build();
	
			// schema of the current version is dynamic, while the schema of a snapshot is static.
			if (!idAndVersion.getVersion().isPresent()) {
				bindSchemaToView(idAndVersion, sqlQuery);
			}
			
			IdAndVersion[] dependentArray = getAvailableDependentIds(sqlQuery);
			
			LOG.info("Rebuilding materialized view index " + idAndVersion);
			// continue with a read lock on each dependent table.
			tableManagerSupport.tryRunWithTableNonExclusiveLock(callback, parentContext, (ProgressCallback innerCallback) -> {
				createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(sqlQuery, columModelManager.getTableSchema(idAndVersion), tableManagerSupport.isTableSearchEnabled(idAndVersion));
				return null;
			}, dependentArray);
		} catch (RecoverableMessageException e) {
			throw e;
		} catch (InvalidStatusTokenException | LockUnavilableException | TableIndexConnectionUnavailableException | TableUnavailableException e) {
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			LOG.error("Failed to build materialized view " + idAndVersion, e);
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, e);
			throw e;
		}
	}

	void rebuildAvailableViewHoldingTemporaryExclusiveLock(ProgressCallback callback, LockContext parentContext, IdAndVersion idAndVersion, IdAndVersion temporaryId) throws Exception {
		try {
			IndexDescription currentIndex = tableManagerSupport.getIndexDescription(idAndVersion);
			// Note: The dependencies must match the current index dependencies, if that was not true then the view would be rebuilt from scratch
			IndexDescription temporaryIndex = new MaterializedViewIndexDescription(temporaryId, currentIndex.getDependencies());
			
			String definingSql = materializedViewDao.getMaterializedViewDefiningSql(idAndVersion)
				.orElseThrow(() -> new IllegalArgumentException("No defining SQL for: " + idAndVersion.toString()));
			
			QueryTranslator sqlQuery = QueryTranslator.builder()
				.sql(definingSql)
				.schemaProvider(columModelManager)
				.sqlContext(SqlContext.build)
				// Use the temporary index in the query so that it populates the correct index
				.indexDescription(temporaryIndex)
			.build();

			// The schema of the dependent tables might have changes, so we do not bind it to the available version yet but we use it to build the temporary index
			List<ColumnModel> schema = sqlQuery.getSchemaOfSelect().stream().map(c -> columModelManager.createColumnModel(c)).collect(Collectors.toList());
			
			IdAndVersion[] dependentArray = getAvailableDependentIds(sqlQuery);
			
			LOG.info("Building temporary materialized view index " + temporaryId);
			
			TableIndexManager indexManager = connectionFactory.connectToTableIndex(idAndVersion);
			
			// continue with a read lock on each dependent table.
			boolean isUpToDate = tableManagerSupport.tryRunWithTableNonExclusiveLock(callback, parentContext, (ProgressCallback innerCallback) -> {
				List<String> schemaIds = schema.stream().map(ColumnModel::getId).collect(Collectors.toList());
				long version = indexManager.getVersionFromIndexDependencies(currentIndex);
				boolean isSearchEnabled = tableManagerSupport.isTableSearchEnabled(idAndVersion);
				
				// Before rebuilding the view, we check if any dependency was updated
				if (tableManagerSupport.isIndexSynchronized(idAndVersion, schemaIds, version, isSearchEnabled)) {
					return true;
				}
				
				createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(sqlQuery, schema, isSearchEnabled);

				return false;
			}, dependentArray);
			
			if (isUpToDate) {
				LOG.info("Will skip rebuilding AVAILABLE materialized view " + idAndVersion + ". The index is up to date.");
				return;
			}
			
			// If the table is not available anymore there is no need to switch since another process triggered a processing of the table
			if (TableState.AVAILABLE != tableManagerSupport.getTableStatusState(idAndVersion).orElse(null)) {
				LOG.info("Will skip swapping materialized view " + idAndVersion + " index from temporary index " + temporaryId + ". The view is NOT AVAILABLE anymore.");
				return;
			}
			
			LOG.info("Updating materialized view index " + idAndVersion + " from temporary index " + temporaryId);			
			
			// Now we switch the temporary index to the real index, we try to acquire an exclusive lock so that readers are temporarily blocked during this operation
			tableManagerSupport.tryRunWithTableExclusiveLock(callback, parentContext, idAndVersion, (innerCallback) -> {
				indexManager.swapTableIndex(temporaryIndex, currentIndex);
				columModelManager.bindColumnsToVersionOfObject(schema.stream().map(ColumnModel::getId).collect(Collectors.toList()), idAndVersion);
				tableManagerSupport.updateChangedOnIfAvailable(idAndVersion);
				return null;
			});
			
		} catch (RecoverableMessageException e) {
			throw e;
		}  catch (LockUnavilableException e) {
			throw new RecoverableMessageException(e);
		} catch (InvalidStatusTokenException | TableIndexConnectionUnavailableException | TableUnavailableException e) {
			throw e;
		} catch (Exception e) {
			LOG.error("Failed to build available materialized view " + idAndVersion, e);
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, e);
			throw e;
		}
	}
	
	void createOrRebuildViewHoldingWriteLockAndAllDependentReadLocks(QueryTranslator definingSql, List<ColumnModel> schema, boolean isSearchEnabled) {
		IdAndVersion idAndVersion = definingSql.getIndexDescription().getIdAndVersion();
		
		TableIndexManager indexManager = connectionFactory.connectToTableIndex(idAndVersion);
		
		// Start the worker
		final String token = tableManagerSupport.startTableProcessing(idAndVersion);
		
		tableManagerSupport.attemptToUpdateTableProgress(idAndVersion, token, "Building MaterializedView...", 0L, 1L);
		
		List<ColumnModel> viewSchema = indexManager.resetTableIndex(definingSql.getIndexDescription(), schema, isSearchEnabled);
		
		Long viewVersion = indexManager.populateMaterializedViewFromDefiningSql(viewSchema, definingSql);
		
		// Now build the secondary indicies
		indexManager.buildTableIndexIndices(definingSql.getIndexDescription(), viewSchema);
		
		// both the version and schema MD5 are used to determine if the view is up-to-date.
		// The schema MD5 is already set when resetting the index
		indexManager.setIndexVersion(idAndVersion, viewVersion);
		
		// Attempt to set the table to complete.
		tableManagerSupport.attemptToSetTableStatusToAvailable(idAndVersion, token, DEFAULT_ETAG);
		
		LOG.info("Materialized view " + idAndVersion + " set to AVAILABLE");
	}
	
	private IdAndVersion[] getAvailableDependentIds(QueryTranslator sqlQuery) {
		// Check if each dependency is available. Note: Getting the status of a
		// dependency can also trigger it to update.
		List<IdAndVersion> dependentTables = sqlQuery.getTableIds();
		for (IdAndVersion dependent : dependentTables) {
			TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(dependent);
			switch (status.getState()) {
			case AVAILABLE:
				break;
			case PROCESSING:
				throw new RecoverableMessageException();
			case PROCESSING_FAILED:
				throw new IllegalArgumentException("Cannot build materialized view " + sqlQuery.getIndexDescription().getIdAndVersion() + ", the dependent table " + dependent + " failed to build");
			default:
				throw new IllegalStateException("Cannot build materialized view " + sqlQuery.getIndexDescription().getIdAndVersion() + ", unsupported state for dependent table " + dependent + ": " + status.getState());
			}
		}
		
		return dependentTables.toArray(new IdAndVersion[dependentTables.size()]);
	}
}
