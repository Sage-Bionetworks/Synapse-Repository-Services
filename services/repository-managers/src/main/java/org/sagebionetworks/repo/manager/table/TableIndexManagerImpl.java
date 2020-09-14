package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.change.ListColumnIndexTableChange;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.manager.table.metadata.ViewScopeFilterBuilder;
import org.sagebionetworks.repo.manager.table.metadata.ViewScopeFilterProvider;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.ListColumnRowChanges;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.transaction.TransactionStatus;

public class TableIndexManagerImpl implements TableIndexManager {
	static private Logger log = LogManager.getLogger(TableIndexManagerImpl.class);

	public static final int MAX_MYSQL_INDEX_COUNT = 60; // mysql only supports a max of 64 secondary indices per table.

	public static final long MAX_BYTES_PER_BATCH = 1024*1024*5;// 5MB

	private final TableIndexDAO tableIndexDao;
	private final TableManagerSupport tableManagerSupport;
	private final MetadataIndexProviderFactory metadataIndexProviderFactory;
	private final ObjectFieldModelResolverFactory objectFieldModelResolverFactory;

	public TableIndexManagerImpl(TableIndexDAO dao, TableManagerSupport tableManagerSupport, MetadataIndexProviderFactory metadataIndexProviderFactory, ObjectFieldModelResolverFactory objectFieldModelResolverFactory){
		if(dao == null){
			throw new IllegalArgumentException("TableIndexDAO cannot be null");
		}
		if(tableManagerSupport == null){
			throw new IllegalArgumentException("TableManagerSupport cannot be null");
		}
		if(metadataIndexProviderFactory == null) {
			throw new IllegalArgumentException("MetadataIndexProviderFactory cannot be null");
		}
		if (objectFieldModelResolverFactory == null) {
			throw new IllegalArgumentException("ObjectFieldModelResolverFactory cannot be null");
		}
		this.tableIndexDao = dao;
		this.tableManagerSupport = tableManagerSupport;
		this.metadataIndexProviderFactory = metadataIndexProviderFactory;
		this.objectFieldModelResolverFactory = objectFieldModelResolverFactory;
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see org.sagebionetworks.repo.manager.table.TableIndexManager#
	 * getCurrentVersionOfIndex
	 * (org.sagebionetworks.repo.manager.table.TableIndexManager
	 * .TableIndexConnection)
	 */
	@Override
	public long getCurrentVersionOfIndex(final IdAndVersion tableId) {
		return tableIndexDao.getMaxCurrentCompleteVersionForTable(tableId);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sagebionetworks.repo.manager.table.TableIndexManager#
	 * applyChangeSetToIndex
	 * (org.sagebionetworks.repo.manager.table.TableIndexManager
	 * .TableIndexConnection, org.sagebionetworks.repo.model.table.RowSet,
	 * java.util.List, long)
	 */
	@Override
	public void applyChangeSetToIndex(final IdAndVersion tableId, final SparseChangeSet rowset,
			final long changeSetVersionNumber) {
		// Validate all rows have the same version number
		// Has this version already been applied to the table index?
		final long currentVersion = tableIndexDao
				.getMaxCurrentCompleteVersionForTable(tableId);
		if (changeSetVersionNumber > currentVersion) {
			// apply all changes in a transaction
			tableIndexDao
					.executeInWriteTransaction((TransactionStatus status) -> {
							// apply all groups to the table
							for(Grouping grouping: rowset.groupByValidValues()){
								tableIndexDao.createOrUpdateOrDeleteRows(tableId, grouping);
							}
							// Extract all file handle IDs from this set
							Set<Long> fileHandleIds = rowset.getFileHandleIdsInSparseChangeSet();
							if (!fileHandleIds.isEmpty()) {
								tableIndexDao.applyFileHandleIdsToTable(
										tableId, fileHandleIds);
							}
							boolean alterTemp = false;
							//once all changes to main table are applied, populate the list-type columns with the changes.
							for(ListColumnRowChanges listColumnChange : rowset.groupListColumnChanges()){
								tableIndexDao.deleteFromListColumnIndexTable(tableId, listColumnChange.getColumnModel(), listColumnChange.getRowIds());
								tableIndexDao.populateListColumnIndexTable(tableId, listColumnChange.getColumnModel(), listColumnChange.getRowIds(), alterTemp);
							}

							// set the new max version for the index
							tableIndexDao.setMaxCurrentCompleteVersionForTable(
									tableId, changeSetVersionNumber);
							return null;
						}
					);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableIndexManager#
	 * isVersionAppliedToIndex
	 * (org.sagebionetworks.repo.manager.table.TableIndexManager
	 * .TableIndexConnection, long)
	 */
	@Override
	public boolean isVersionAppliedToIndex(final IdAndVersion tableId, long versionNumber) {
		final long currentVersion = tableIndexDao.getMaxCurrentCompleteVersionForTable(tableId);
		return currentVersion >= versionNumber;
	}
	
	/**
	 * Set the table index schema to match the given schema.
	 * 
	 * @param progressCallback
	 * @param newSchema
	 * @param removeMissingColumns Should missing columns be removed?
	 */
	@Override
	public List<ColumnChangeDetails> setIndexSchema(final IdAndVersion tableId, boolean isTableView, List<ColumnModel> newSchema){
		// Lookup the current schema of the index
		List<DatabaseColumnInfo> currentSchema = tableIndexDao.getDatabaseInfo(tableId);
		// create a change that replaces the old schema as needed.
		List<ColumnChangeDetails> changes = SQLUtils.createReplaceSchemaChange(currentSchema, newSchema);
		updateTableSchema(tableId, isTableView, changes);

		//apply changes to multi-value column indexes
		Set<Long> existingListColumnIndexTableNames = tableIndexDao.getMultivalueColumnIndexTableColumnIds(tableId);
		List<ListColumnIndexTableChange> listColumnIndexTableChanges = listColumnIndexTableChangesFromExpectedSchema(newSchema, existingListColumnIndexTableNames);
		boolean alterTemp = false;
		applyListColumnIndexTableChanges(tableId, listColumnIndexTableChanges, alterTemp);
		return changes;
	}

	@Override
	public void deleteTableIndex(final IdAndVersion tableId) {
		// delete all tables for this index.
		tableIndexDao.deleteTable(tableId);
	}

	@Override
	public void setIndexVersionAndSchemaMD5Hex(final IdAndVersion tableId, Long viewCRC, String schemaMD5Hex) {
		tableIndexDao.setIndexVersionAndSchemaMD5Hex(tableId, viewCRC, schemaMD5Hex);
	}

	/**
	 * Given the expected schema, figure out changes that need to be applied to list column index tables.
	 * NOTE: !!!!!!!This should ONLY BE USED for reconciling schema before RowSet changes!!!!!!!!
	 *       ONLY additions and deletions will be. RENAMES and TYPE CHANGES can NOT BE HANDLED by this.
	 *       USE {@link #listColumnIndexTableChangesFromChangeDetails(List, Set)}
	 *       for SCHEMA-ONLY changes
	 * @param expectedSchema
	 * @return
	 */
	static List<ListColumnIndexTableChange> listColumnIndexTableChangesFromExpectedSchema(List<ColumnModel> expectedSchema, Set<Long> existingListIndexColumns){
		ValidateArgument.required(expectedSchema, "expectedSchema");
		ValidateArgument.required(existingListIndexColumns, "existingMultiValueIndexColumns");

		Map<Long,ColumnModel> listsColumnsOnly = expectedSchema.stream()
				.filter((columnModel) ->ColumnTypeListMappings.isList(columnModel.getColumnType()))
				.collect(Collectors.toMap((ColumnModel cm) -> Long.parseLong(cm.getId()), Function.identity()));

		List<ListColumnIndexTableChange> result = new ArrayList<>();

		for(ColumnModel columnModel : listsColumnsOnly.values()){
			long columnModelId = Long.parseLong(columnModel.getId());
			if(existingListIndexColumns.contains(columnModelId)){
				//index table already exists so skip
				continue;
			}

			//otherwise, we need to create a new column index for this
			result.add(ListColumnIndexTableChange.newAddition(columnModel));
		}

		for(Long existingIndexTableColumnId : existingListIndexColumns){
			if(listsColumnsOnly.containsKey(existingIndexTableColumnId)){
				//no deletion necessary for existing table
				continue;
			}
			result.add(ListColumnIndexTableChange.newRemoval(existingIndexTableColumnId));
		}

		return result;
	}

	/**
	 * Determine changes that need to be made to a column given the column change set and the existing set of tables for a table's list columns
	 * @param changes
	 * @param existingListIndexColumns
	 * @return
	 */
	static List<ListColumnIndexTableChange> listColumnIndexTableChangesFromChangeDetails(List<ColumnChangeDetails> changes, Set<Long> existingListIndexColumns){
		ValidateArgument.required(changes, "changes");
		ValidateArgument.required(existingListIndexColumns, "existingListIndexColumns");

		List<ListColumnIndexTableChange> result = new ArrayList<>();

		for(ColumnChangeDetails changeDetails : changes){
			ColumnModel oldColumn = changeDetails.getOldColumn();
			ColumnModel newColumn = changeDetails.getNewColumn();

			boolean oldColumnIsListType = oldColumn != null && ColumnTypeListMappings.isList(oldColumn.getColumnType());
			boolean newColumnIsListType = newColumn != null && ColumnTypeListMappings.isList(newColumn.getColumnType());

			Long oldColumnId = oldColumnIsListType ? Long.parseLong(oldColumn.getId()) : null;
			Long newColumnId = newColumnIsListType ? Long.parseLong(newColumn.getId()) : null;

			//either no change, rename, or type change
			if( oldColumnIsListType && existingListIndexColumns.contains(oldColumnId)
				&& newColumnIsListType && !existingListIndexColumns.contains(newColumnId)
			){
				//update
				result.add(ListColumnIndexTableChange.newUpdate(oldColumnId, newColumn));
			}else if (oldColumnIsListType && existingListIndexColumns.contains(oldColumnId) ){
				//no change necessary
				if(oldColumnId.equals(newColumnId)){
					continue;
				}
				//delete old column
				result.add(ListColumnIndexTableChange.newRemoval(oldColumnId) );
			} else if (newColumnIsListType && !existingListIndexColumns.contains(newColumnId) ){
				//add new column
				result.add(ListColumnIndexTableChange.newAddition(newColumn));
			}
		}

		return result;
	}

	void applyListColumnIndexTableChanges(IdAndVersion tableId, List<ListColumnIndexTableChange> changes, boolean alterTemp){
		for(ListColumnIndexTableChange change : changes){
			switch (change.getListIndexTableChangeType()){
				case ADD:
					tableIndexDao.createMultivalueColumnIndexTable(tableId, change.getNewColumnChange(), alterTemp);
					tableIndexDao.populateListColumnIndexTable(tableId, change.getNewColumnChange(), null, alterTemp);
					break;
				case REMOVE:
					tableIndexDao.deleteMultivalueColumnIndexTable(tableId, change.getOldColumnId(), alterTemp);
					break;
				case UPDATE:
					tableIndexDao.updateMultivalueColumnIndexTable(tableId, change.getOldColumnId(), change.getNewColumnChange(), alterTemp);
					break;
			}
		}
	}



	@Override
	public boolean updateTableSchema(final IdAndVersion tableId, boolean isTableView, List<ColumnChangeDetails> changes) {
		// create the table if it does not exist
		tableIndexDao.createTableIfDoesNotExist(tableId, isTableView);
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(tableId);
		boolean alterTemp = false;
		// Alter the table
		boolean wasSchemaChanged = alterTableAsNeededWithinAutoProgress(tableId, changes, alterTemp);
		if(wasSchemaChanged){
			// Get the current schema.
			List<DatabaseColumnInfo> tableInfo = tableIndexDao.getDatabaseInfo(tableId);
			// Determine the current schema
			List<ColumnModel> currentSchema = SQLUtils.extractSchemaFromInfo(tableInfo);
			if(currentSchema.isEmpty()){
				// there are no columns in the table so truncate all rows.
				tableIndexDao.truncateTable(tableId);
			}
			// Set the new schema MD5
			List<String> columnIds = TableModelUtils.getIds(currentSchema);
			String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(columnIds);
			tableIndexDao.setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		}
		return wasSchemaChanged;
	}	
	
	@Override
	public void alterTempTableSchmea(final IdAndVersion tableId, final List<ColumnChangeDetails> changes){
		boolean alterTemp = true;
		validateTableMaximumListLengthChanges(tableId,changes);
		alterTableAsNeededWithinAutoProgress(tableId, changes, alterTemp);
		alterListColumnIndexTableWithSchemaChange(tableId,changes, alterTemp);
	}

	void validateTableMaximumListLengthChanges(final IdAndVersion tableId, final List<ColumnChangeDetails> changes){
		for(ColumnChangeDetails change: changes){
			validateTableMaximumListLengthChanges(tableId, change);
		}
	}

	void validateTableMaximumListLengthChanges(IdAndVersion tableId, ColumnChangeDetails change) {
		ColumnModel oldColumn = change.getOldColumn();
		ColumnType oldColumnType = oldColumn != null ? oldColumn.getColumnType() : null;
		ColumnModel newColumn = change.getNewColumn();
		ColumnType newColumnType = newColumn != null ? newColumn.getColumnType() : null;
		if(ColumnTypeListMappings.isList(oldColumnType)
				&& ColumnTypeListMappings.isList(newColumnType)
				//we are decreasing the maximum list size
				&& oldColumn.getMaximumListLength() > newColumn.getMaximumListLength()) {

			long maximumListLengthInTable = tableIndexDao.tempTableListColumnMaxLength(tableId, oldColumn.getId());
			if (newColumn.getMaximumListLength() < maximumListLengthInTable) {
				throw new IllegalArgumentException("maximumListLength for ColumnModel \"" + newColumn.getName() +
						"\" must be at least: " + maximumListLengthInTable);
			}
		}
	}

	/**
	 * Alter a table as needed within the auto-progress using the provided changes.
	 * Note: If a column update is requested but the column does not actual exist in the index
	 * the update will be changed to an added.
	 * @param tableId
	 * @param changes
	 * @param alterTemp
	 * @return
	 */
	boolean alterTableAsNeededWithinAutoProgress(final IdAndVersion tableId, List<ColumnChangeDetails> changes, boolean alterTemp){
		// Lookup the current schema of the index.
		List<DatabaseColumnInfo> currentIndedSchema = tableIndexDao.getDatabaseInfo(tableId);
		// must also gather the names of each index currently applied to each column.
		tableIndexDao.provideIndexName(currentIndedSchema, tableId);
		// Ensure all all updated columns actually exist.
		changes = SQLUtils.matchChangesToCurrentInfo(currentIndedSchema, changes);
		return tableIndexDao.alterTableAsNeeded(tableId, changes, alterTemp);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableIndexManager#optimizeTableIndices()
	 */
	@Override
	public void optimizeTableIndices(final IdAndVersion tableId) {
		// To optimize a table's indices, statistics must be gathered
		// for each column of the table.
		List<DatabaseColumnInfo> tableInfo = tableIndexDao.getDatabaseInfo(tableId);
		// must also gather cardinality data for each column.
		tableIndexDao.provideCardinality(tableInfo, tableId);
		// must also gather the names of each index currently applied to each column.
		tableIndexDao.provideIndexName(tableInfo, tableId);
		// All of the column data is then used to optimized the indices.
		tableIndexDao.optimizeTableIndices(tableInfo, tableId, MAX_MYSQL_INDEX_COUNT);
	}

	@Override
	public void populateListColumnIndexTables(final IdAndVersion tableIdAndVersion, final List<ColumnModel> schema){
		Set<Long> rowIds = null;
		populateListColumnIndexTables(tableIdAndVersion, schema, rowIds);
	}
	
	@Override
	public void populateListColumnIndexTables(final IdAndVersion tableIdAndVersion, final List<ColumnModel> schema, Set<Long> rowIds){
		ValidateArgument.required(tableIdAndVersion, "tableIdAndVersion");
		ValidateArgument.required(schema, "schema");
		boolean alterTemp = false;

		for(ColumnModel column: schema) {
			if (ColumnTypeListMappings.isList(column.getColumnType())) {
				tableIndexDao.populateListColumnIndexTable(tableIdAndVersion, column, rowIds, alterTemp);
			}
		}
	}

	@Override
	public void createTemporaryTableCopy(final IdAndVersion tableId) {
		// creating a temp table can take a long time so auto-progress is used.
		// create the table.
		tableIndexDao.createTemporaryTable(tableId);
		// copy all the data from the original to the temp.
		tableIndexDao.copyAllDataToTemporaryTable(tableId);

		// if any multi-value column index tables exist, create a copy of them
		for(Long columnId: tableIndexDao.getMultivalueColumnIndexTableColumnIds(tableId)) {
			String colIdStr = columnId.toString();
			tableIndexDao.createTemporaryMultiValueColumnIndexTable(tableId, colIdStr);
			tableIndexDao.copyAllDataToTemporaryMultiValueColumnIndexTable(tableId, colIdStr);
		}
	}
	@Override
	public void deleteTemporaryTableCopy(final IdAndVersion tableId) {
		// delete multi-value index table first as they have a foreign key ref to the temp table
		tableIndexDao.deleteAllTemporaryMultiValueColumnIndexTable(tableId);
		// delete
		tableIndexDao.deleteTemporaryTable(tableId);
	}
	
	@Override
	public long populateViewFromEntityReplication(final Long viewId, final ViewScopeType scopeType,
			final Set<Long> allContainersInScope, final List<ColumnModel> currentSchema) {
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		ValidateArgument.required(currentSchema, "currentSchema");
		
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(scopeType.getObjectType());
		
		ViewScopeFilter scopeFilter = buildViewScopeFilter(provider, scopeType.getTypeMask(), allContainersInScope);
		
		// copy the data from the entity replication tables to table's index
		try {
			tableIndexDao.copyObjectReplicationToView(viewId, scopeFilter, currentSchema, provider);
		} catch (Exception e) {
			// if the copy failed. Attempt to determine the cause.
			determineCauseOfReplicationFailure(e, currentSchema, provider, scopeType.getTypeMask(), scopeFilter);
		}
		// calculate the new CRC32;
		return tableIndexDao.calculateCRC32ofTableView(viewId);
	}
	
	@Override
	public ColumnModelPage getPossibleColumnModelsForView(
			final Long viewId, String nextPageToken) {
		ValidateArgument.required(viewId, "viewId");
		IdAndVersion idAndVersion = IdAndVersion.newBuilder().setId(viewId).build();
		ViewScopeType scopeType = tableManagerSupport.getViewScopeType(idAndVersion);
		Set<Long> containerIds = tableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, scopeType);
		return getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, nextPageToken);
	}
	
	@Override
	public ColumnModelPage getPossibleColumnModelsForScope(ViewScope scope, String nextPageToken) {
		ValidateArgument.required(scope, "scope");
		ValidateArgument.required(scope.getScope(), "scope.scopeIds");
		
		ViewEntityType viewType = scope.getViewEntityType();
		Long viewTypeMask = scope.getViewTypeMask();
		
		// When the scope does not specify the object type we defaults to ENTITY as not to break the API
		if (viewType == null || ViewEntityType.entityview == viewType) {
			viewType = ViewEntityType.entityview;
			// Entity views require a mask 
			viewTypeMask = ViewTypeMask.getViewTypeMask(scope);
		}
		
		ViewObjectType objectType = ViewObjectType.map(viewType);	
		
		ViewScopeType scopeType = new ViewScopeType(objectType, viewTypeMask);
		// lookup the containers for the given scope
		Set<Long> scopeSet = new HashSet<Long>(KeyFactory.stringToKey(scope.getScope()));
		Set<Long> containerIds = tableManagerSupport.getAllContainerIdsForScope(scopeSet, scopeType);
		return getPossibleAnnotationDefinitionsForContainerIds(scopeType, containerIds, nextPageToken);
	}
	
	
	/**
	 * Get the possible annotations for the given set of container IDs.
	 * 
	 * @param containerIds
	 * @param nextPageToken Optional: Controls pagination.
	 * @return
	 */
	ColumnModelPage getPossibleAnnotationDefinitionsForContainerIds(ViewScopeType viewScopeType,
			Set<Long> containerIds, String nextPageToken) {
		ValidateArgument.required(containerIds, "containerIds");
		NextPageToken token =  new NextPageToken(nextPageToken);
		ColumnModelPage results = new ColumnModelPage();
		
		if(containerIds.isEmpty()){
			results.setResults(Collections.emptyList());
			results.setNextPageToken(null);
			return results;
		}
		
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(viewScopeType.getObjectType());
		DefaultColumnModel defaultColumnModel = provider.getDefaultColumnModel(viewScopeType.getTypeMask());
		
		// We exclude from the suggested column models the custom fields defined for the object (since they are included in the default column model itself)
		List<String> excludeKeys = getAnnotationKeysExcludeList(defaultColumnModel);
		
		ViewScopeFilter scopeFilter = buildViewScopeFilter(provider, viewScopeType.getTypeMask(), containerIds);
		// request one page with a limit one larger than the passed limit.
		List<ColumnModel> columns = tableIndexDao.getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, token.getLimitForQuery(), token.getOffset());
		results.setNextPageToken(token.getNextPageTokenForCurrentResults(columns));
		results.setResults(columns);
		return results;
	}
	
	@Override
	public void buildIndexToChangeNumber(final ProgressCallback progressCallback, final IdAndVersion idAndVersion,
			final Iterator<TableChangeMetaData> iterator) throws RecoverableMessageException {
		try {
			// Run with the exclusive lock on the table if we can get it.
			tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, idAndVersion,
					(ProgressCallback callback) -> {
						buildTableIndexWithLock(callback, idAndVersion, iterator);
						return null;
					});
		} catch (LockUnavilableException | TableUnavailableException | InterruptedException| IOException e) {
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			if(e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}else {
				throw new RuntimeException(e);
			}
		} 
	}
	
	/**
	 * Build a table index while holding the table's exclusive lock.  This level manages the status of the table.
	 * @param progressCallback
	 * @param idAndVersion
	 * @param iterator
	 * @throws RecoverableMessageException
	 */
	void buildTableIndexWithLock(final ProgressCallback progressCallback, final IdAndVersion idAndVersion,
			final Iterator<TableChangeMetaData> iterator) throws RecoverableMessageException {
		// Attempt to run with
		try {
			if(tableManagerSupport.isTableIndexStateInvalid(idAndVersion)) {
				log.warn("Current table index is invalid and will be rebuilt from scratch for table: " + idAndVersion);
				deleteTableIndex(idAndVersion);
			}

			// Only proceed if work is needed.
			if (!tableManagerSupport.isIndexWorkRequired(idAndVersion)) {
				log.info("Index already up-to-date for table: " + idAndVersion);
				return;
			}
			/*
			 * Before we start working on the table make sure it is in the processing mode.
			 * This will generate a new reset token and will not broadcast the change.
			 */
			final String tableResetToken = tableManagerSupport.startTableProcessing(idAndVersion);
			// Lookup the target change number for the given ID and version.
			Optional<Long> targetChangeNumber = tableManagerSupport.getLastTableChangeNumber(idAndVersion);
			if(!targetChangeNumber.isPresent()) {
				throw new NotFoundException("Snapshot for "+idAndVersion.toString()+" does not exist");
			}
			// build the table up to the latest change.
			String lastEtag = buildIndexToLatestChange(idAndVersion, iterator, targetChangeNumber.get(),
					tableResetToken);
			log.info("Completed index update for: " + idAndVersion);
			tableManagerSupport.attemptToSetTableStatusToAvailable(idAndVersion, tableResetToken, lastEtag);
		} catch (InvalidStatusTokenException e) {
			// PLFM-6069, invalid tokens should not cause the table state to be set to failed, but
			// instead should be retried later.
			log.warn("InvalidStatusTokenException occurred for "+idAndVersion+", message will be returned to the queue");
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			// Any other error is a table failure.
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, e);
			// This is not an error we can recover from.
			log.info("Unrecoverable failure to update table index: "+idAndVersion);
		}
	}
	
	/**
	 * Build the table index up to the latest change.  The caller must hold the table's exclusive lock and manage
	 * the status of the table.
	 * @param tableId
	 * @param iterator
	 * @param lastChangeNumber
	 * @param tableResetToken
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	String buildIndexToLatestChange(final IdAndVersion idAndVersion, final Iterator<TableChangeMetaData> iterator,
			final long targetChangeNumber, final String tableResetToken) throws NotFoundException, IOException {
		String lastEtag = null;
		// Inspect each change.
		while(iterator.hasNext()) {
			TableChangeMetaData changeMetadata = iterator.next();
			if(changeMetadata.getChangeNumber() > targetChangeNumber) {
				// all changes have been applied to the index.
				break;
			}
			if(!isVersionAppliedToIndex(idAndVersion, changeMetadata.getChangeNumber())) {
				// This change needs to be applied to the table
				tableManagerSupport.attemptToUpdateTableProgress(idAndVersion,
						tableResetToken, "Applying change: " + changeMetadata.getChangeNumber(), changeMetadata.getChangeNumber(),
						targetChangeNumber);
				applyChangeToIndex(idAndVersion, changeMetadata);
				lastEtag = changeMetadata.getETag();
			}
		}

		/*
		 * When building a table to the current version, we unconditionally apply the
		 * current table schema to the index as a workaround for PLFM-5639. This is a
		 * fix for tables with schema changes that were not captured in the table's
		 * history.
		 */
		List<ColumnModel> boundSchema = tableManagerSupport.getTableSchema(idAndVersion);
		boolean isTableView = false;
		List<ColumnChangeDetails> changes = setIndexSchema(idAndVersion, isTableView, boundSchema);
		if(changes != null && !changes.isEmpty()) {
			log.warn("PLFM-5639: table: "+idAndVersion.toString()+" required the following schema changes: "+changes);
		}
		// now that table is created and populated the indices on the table can be optimized.
		optimizeTableIndices(idAndVersion);
		return lastEtag;
	}
	
	/**
	 * Apply the provided change to the provided index.
	 * 
	 * @param idAndVersion
	 * @param changeMetadata
	 * @throws NotFoundException
	 * @throws IOException
	 */
	void applyChangeToIndex(IdAndVersion idAndVersion, TableChangeMetaData changeMetadata) throws NotFoundException, IOException {
		// Load the change based on the type and added the change to the index.
		switch(changeMetadata.getChangeType()) {
		case ROW:
			applyRowChangeToIndex(idAndVersion, changeMetadata.loadChangeData(SparseChangeSet.class));
			break;
		case COLUMN:
			applySchemaChangeToIndex(idAndVersion, changeMetadata.loadChangeData(SchemaChange.class));
			break;
		default:
			throw new IllegalArgumentException("Unknown type: "+changeMetadata.getChangeType());
		}
	}
	
	/**
	 * Apply the provided schema change to the provided table's index.
	 * 
	 * @param idAndVersion
	 * @param schemaChangeData
	 */
	void applySchemaChangeToIndex(IdAndVersion idAndVersion, ChangeData<SchemaChange> schemaChangeData) {
		boolean isTableView = false;
		updateTableSchema(idAndVersion, isTableView, schemaChangeData.getChange().getDetails());

		boolean alterTemp = false;
		alterListColumnIndexTableWithSchemaChange(idAndVersion, schemaChangeData.getChange().getDetails(), alterTemp);

		// set the new max version for the index
		tableIndexDao.setMaxCurrentCompleteVersionForTable(idAndVersion, schemaChangeData.getChangeNumber());
	}

	private void alterListColumnIndexTableWithSchemaChange(IdAndVersion idAndVersion, List<ColumnChangeDetails> columnChangeDetails, boolean alterTemp) {
		//apply changes to multi-value column indexes
		Set<Long> existingListColumnIndexTableNames = tableIndexDao.getMultivalueColumnIndexTableColumnIds(idAndVersion);
		List<ListColumnIndexTableChange> listColumnIndexTableChanges = listColumnIndexTableChangesFromChangeDetails(columnChangeDetails, existingListColumnIndexTableNames);

		applyListColumnIndexTableChanges(idAndVersion, listColumnIndexTableChanges, alterTemp);
	}

	/**
	 * Apply the provided row change set to the provide table's index.
	 * @param idAndVersion
	 * @param rowChange
	 */
	void applyRowChangeToIndex(IdAndVersion idAndVersion, ChangeData<SparseChangeSet> rowChange) {
		// Get the change set.
		SparseChangeSet sparseChangeSet = rowChange.getChange();
		// match the schema to the change set.
		boolean isTableView = false;
		setIndexSchema(idAndVersion, isTableView, sparseChangeSet.getSchema());
		// attempt to apply this change set to the table.
		applyChangeSetToIndex(idAndVersion, sparseChangeSet, rowChange.getChangeNumber());
	}
	
	@Override
	public void createViewSnapshot(Long viewId, ViewScopeType scopeType, Set<Long> allContainersInScope,
			List<ColumnModel> viewSchema, CSVWriterStream writter) {

		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(scopeType.getObjectType());
		
		ViewScopeFilter scopeFilter = buildViewScopeFilter(provider, scopeType.getTypeMask(), allContainersInScope);
		
		tableIndexDao.createViewSnapshotFromObjectReplication(viewId, scopeFilter, viewSchema, provider, writter);
	}
	@Override
	public void populateViewFromSnapshot(IdAndVersion idAndVersion, Iterator<String[]> input) {
		tableIndexDao.populateViewFromSnapshot(idAndVersion, input, MAX_BYTES_PER_BATCH);
	}

	@Override
	public Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, ViewScopeType scopeType, Set<Long> allContainersInScope,
			long limit) {
		
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(scopeType.getObjectType());
		
		ViewScopeFilter scopeFilter = buildViewScopeFilter(provider, scopeType.getTypeMask(), allContainersInScope);
		
		return tableIndexDao.getOutOfDateRowsForView(viewId, scopeFilter, limit);
	}
	
	@Override
	public void updateViewRowsInTransaction(IdAndVersion viewId, Set<Long> rowsIdsWithChanges, ViewScopeType scopeType,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(rowsIdsWithChanges, "rowsIdsWithChanges");
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		ValidateArgument.required(currentSchema, "currentSchema");
		
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(scopeType.getObjectType());
		
		ViewScopeFilter scopeFilter = buildViewScopeFilter(provider, scopeType.getTypeMask(), allContainersInScope);

		// all calls are in a single transaction.
		tableIndexDao.executeInWriteTransaction((TransactionStatus status) -> {
			Long[] rowsIdsArray = rowsIdsWithChanges.stream().toArray(Long[] ::new);
 			// First delete the provided rows from the view
			tableIndexDao.deleteRowsFromViewBatch(viewId, rowsIdsArray);
			try {
				// Apply any updates to the view for the given Ids
				tableIndexDao.copyObjectReplicationToView(viewId.getId(), scopeFilter, currentSchema, provider, rowsIdsWithChanges);
				populateListColumnIndexTables(viewId, currentSchema, rowsIdsWithChanges);
			} catch (Exception e) {
				// if the copy failed. Attempt to determine the cause.  This will always throw an exception.
				determineCauseOfReplicationFailure(e, currentSchema, provider, scopeType.getTypeMask(), scopeFilter);
			}
			return null;
		});
	}
	
	void determineCauseOfReplicationFailure(Exception exception, List<ColumnModel> currentSchema, MetadataIndexProvider provider, Long viewTypeMask, ViewScopeFilter scopeFilter) {
		DefaultColumnModel defaultColumnModel = provider.getDefaultColumnModel(viewTypeMask);
		
		List<String> excludeKeys = getAnnotationKeysExcludeList(defaultColumnModel);

		// Calculate the schema from the annotations
		List<ColumnModel> schemaFromAnnotations = tableIndexDao.getPossibleColumnModelsForContainers(scopeFilter, excludeKeys, Long.MAX_VALUE, 0L);
		
		ObjectFieldModelResolver objectFieldModelResolver = objectFieldModelResolverFactory.getObjectFieldModelResolver(provider);
		
		// Filter all the fields that are default object fields
		List<ColumnModel> filteredSchema = currentSchema.stream()
				.filter( model -> !objectFieldModelResolver.findMatch(model).isPresent())
				.collect(Collectors.toList());
		
		for (ColumnModel annotationModel : schemaFromAnnotations) {
			for (ColumnModel schemaModel : filteredSchema) {
				SQLUtils.determineCauseOfException(exception, schemaModel, annotationModel);
			}
		}

		// Have not determined the cause so throw the original exception
		if (exception instanceof RuntimeException) {
			throw (RuntimeException) exception;
		} else {
			throw new RuntimeException(exception);
		}
	}
	
	private List<String> getAnnotationKeysExcludeList(DefaultColumnModel defaultColumnModel) {
		if (defaultColumnModel.getCustomFields() == null || defaultColumnModel.getCustomFields().isEmpty()) {
			return null;
		}
		return defaultColumnModel.getCustomFields()
					.stream()
					.map(ColumnModel::getName)
					.collect(Collectors.toList());
	}
	
	private ViewScopeFilter buildViewScopeFilter(ViewScopeFilterProvider provider, Long viewTypeMask, Set<Long> containerIds) {
		return new ViewScopeFilterBuilder(provider, viewTypeMask)
				.withContainerIds(containerIds)
				.build();
	}
	@Override
	public void refreshViewBenefactors(final IdAndVersion viewId) {
		ValidateArgument.required(viewId, "viewId");
		ViewScopeType scopeType = tableManagerSupport.getViewScopeType(viewId);
		tableIndexDao.refreshViewBenefactors(viewId, scopeType.getObjectType());
	}

}
