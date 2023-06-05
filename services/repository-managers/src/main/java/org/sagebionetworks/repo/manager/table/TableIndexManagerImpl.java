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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.change.ListColumnIndexTableChange;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.search.RowSearchContent;
import org.sagebionetworks.table.cluster.search.TableRowData;
import org.sagebionetworks.table.cluster.search.TableRowSearchProcessor;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.ListColumnRowChanges;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SearchChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.PaginationProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

import com.google.common.collect.Iterators;

/**
 * Note: This manager is created as a beans to support profiling calls to the manger. See: PLFM-5984.
 */
@Service
public class TableIndexManagerImpl implements TableIndexManager {
	public static final int BATCH_SIZE = 10_000;

	static private Logger log = LogManager.getLogger(TableIndexManagerImpl.class);

	public static final int MAX_MYSQL_INDEX_COUNT = 60; // mysql only supports a max of 64 secondary indices per table.

	public static final long MAX_BYTES_PER_BATCH = 1024*1024*5;// 5MB
	
	/**
	 * Each container can only be re-synchronized at this frequency.
	 */
	public static final long SYNCHRONIZATION_FEQUENCY_MS = 1000 * 60 * 1000; // 1000 minutes.
	
	private final TableIndexDAO tableIndexDao;
	private final TableManagerSupport tableManagerSupport;
	private final MetadataIndexProviderFactory metadataIndexProviderFactory;
	private final ObjectFieldModelResolverFactory objectFieldModelResolverFactory;
	private final TableRowSearchProcessor searchProcessor;

	public TableIndexManagerImpl(TableIndexDAO dao, TableManagerSupport tableManagerSupport, MetadataIndexProviderFactory metadataIndexProviderFactory, ObjectFieldModelResolverFactory objectFieldModelResolverFactory, TableRowSearchProcessor searchProcessor){
		ValidateArgument.required(dao, "TableIndexDao");
		ValidateArgument.required(tableManagerSupport, "TableManagerSupport");
		ValidateArgument.required(metadataIndexProviderFactory, "MetadataIndexProviderFactory");
		ValidateArgument.required(objectFieldModelResolverFactory, "ObjectFieldModelResolverFactory");
		ValidateArgument.required(searchProcessor, "RowSearchProcessor");
		this.tableIndexDao = dao;
		this.tableManagerSupport = tableManagerSupport;
		this.metadataIndexProviderFactory = metadataIndexProviderFactory;
		this.objectFieldModelResolverFactory = objectFieldModelResolverFactory;
		this.searchProcessor = searchProcessor;
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
	public void applyChangeSetToIndex(final IdAndVersion tableId, final SparseChangeSet rowset, final long changeSetVersionNumber) {
		// Validate all rows have the same version number
		// Has this version already been applied to the table index?
		final long currentVersion = tableIndexDao.getMaxCurrentCompleteVersionForTable(tableId);
		if (changeSetVersionNumber > currentVersion) {
			
			// Read the current status of the search, this is the status up to the current change
			boolean isSearchEnabled = tableIndexDao.isSearchEnabled(tableId);
			IndexDescription index = new TableIndexDescription(tableId);
			
			// apply all changes in a transaction
			tableIndexDao.executeInWriteTransaction((TransactionStatus status) -> {
				// apply all groups to the table
				for(Grouping grouping: rowset.groupByValidValues()) {
					tableIndexDao.createOrUpdateOrDeleteRows(tableId, grouping);
				}
				// Extract all file handle IDs from this set
				Set<Long> fileHandleIds = rowset.getFileHandleIdsInSparseChangeSet();
				if (!fileHandleIds.isEmpty()) {
					tableIndexDao.applyFileHandleIdsToTable(tableId, fileHandleIds);
				}
				boolean alterTemp = false;
				//once all changes to main table are applied, populate the list-type columns with the changes.
				for(ListColumnRowChanges listColumnChange : rowset.groupListColumnChanges()){
					tableIndexDao.deleteFromListColumnIndexTable(tableId, listColumnChange.getColumnModel(), listColumnChange.getRowIds());
					tableIndexDao.populateListColumnIndexTable(tableId, listColumnChange.getColumnModel(), listColumnChange.getRowIds(), alterTemp);
				}
				// Once the values are added or updated we check if the search column needs to be populated
				if (isSearchEnabled) {
					// We only consider the column that match the given type filter
					List<ColumnModel> filteredColumns = getSchemaForSearchIndex(rowset.getSchema());
					
					if (!filteredColumns.isEmpty()) {
						// Find out the set of row ids that added/updated values for searcheable columns
						Set<Long> rowIds = rowset.getCreatedOrUpdatedRowIds(filteredColumns);
						List<TableRowData> rowsData = tableIndexDao.getTableDataForRowIds(tableId, filteredColumns, rowIds);
						updateSearchIndex(index, rowsData.iterator());
					}
				}
				// set the new max version for the index
				tableIndexDao.setMaxCurrentCompleteVersionForTable(tableId, changeSetVersionNumber);
				return null;
			});
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
	public List<ColumnChangeDetails> setIndexSchema(final IndexDescription indexDescription, List<ColumnModel> newSchema){
		/*
		 * It can be expensive to gather table and schema metadata from MySQL, so we
		 * only do so if this represents an an actual schema change. See: PLFM-7458.
		 */
		if (tableIndexDao.doesIndexHashMatchSchemaHash(indexDescription.getIdAndVersion(), newSchema)) {
			return Collections.emptyList();
		}
		// Lookup the current schema of the index
		List<DatabaseColumnInfo> currentSchema = tableIndexDao.getDatabaseInfo(indexDescription.getIdAndVersion());
		// create a change that replaces the old schema as needed.
		List<ColumnChangeDetails> changes = SQLUtils.createReplaceSchemaChange(currentSchema, newSchema);
		
		updateTableSchema(indexDescription, changes);

		//apply changes to multi-value column indexes
		Set<Long> existingListColumnIndexTableNames = tableIndexDao.getMultivalueColumnIndexTableColumnIds(indexDescription.getIdAndVersion());
		List<ListColumnIndexTableChange> listColumnIndexTableChanges = listColumnIndexTableChangesFromExpectedSchema(newSchema, existingListColumnIndexTableNames);
		boolean alterTemp = false;
		applyListColumnIndexTableChanges(indexDescription.getIdAndVersion(), listColumnIndexTableChanges, alterTemp);
		return changes;
	}

	@Override
	public void deleteTableIndex(final IdAndVersion tableId) {
		// delete all tables for this index.
		tableIndexDao.deleteTable(tableId);
	}

	@Override
	public void setIndexVersion(final IdAndVersion tableId, Long indexVersion) {
		tableIndexDao.setMaxCurrentCompleteVersionForTable(tableId, indexVersion);
	}
	
	@Override
	public void setSearchEnabled(IdAndVersion tableId, boolean searchEnabled) {
		tableIndexDao.setSearchEnabled(tableId, searchEnabled);
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

	void createTableIfDoesNotExist(IndexDescription indexDescription) {
		// create the table if it does not exist
		tableIndexDao.createTableIfDoesNotExist(indexDescription);
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(indexDescription.getIdAndVersion());
	}

	@Override
	public boolean updateTableSchema(final IndexDescription indexDescription, List<ColumnChangeDetails> changes) {
		IdAndVersion tableId = indexDescription.getIdAndVersion();
		createTableIfDoesNotExist(indexDescription);
		
		boolean alterTemp = false;
		// Alter the table
		boolean wasSchemaChanged = alterTableAsNeededWithinAutoProgress(tableId, changes, alterTemp);
		
		// Determine the current schema of the table
		List<ColumnModel> currentSchema = getCurrentTableSchema(tableId);
		
		if(currentSchema.isEmpty()){
			// there are no columns in the table so truncate all rows.
			tableIndexDao.truncateTable(tableId);
		}
		
		List<String> columnIds = currentSchema.stream().map(ColumnModel::getId).collect(Collectors.toList());

		// Set the new schema MD5 unconditionally (See https://sagebionetworks.jira.com/browse/PLFM-7615)
		String schemaMD5Hex = TableModelUtils.createSchemaMD5Hex(columnIds);
		
		tableIndexDao.setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
				
		return wasSchemaChanged;
	}	
	
	@Override
	public void alterTempTableSchema(final IdAndVersion tableId, final List<ColumnChangeDetails> changes){
		boolean alterTemp = true;
		validateTableMaximumListLengthChanges(tableId,changes);
		validateSchemaChangeToMediumText(tableId, changes);
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
	
	// When a schema change changes the type of an existing column to MEDIUMTEXT, we need to make sure that the soft limit
	// on the MEDIUMTEXT is valid for existing data
	void validateSchemaChangeToMediumText(IdAndVersion tableId, List<ColumnChangeDetails> changes) {
		changes.forEach( change -> {
			ColumnModel oldColumn = change.getOldColumn();
			ColumnModel newColumn = change.getNewColumn();
			
			// Only applies to column updates
			if (oldColumn == null || newColumn == null) {
				return;
			}
			// Only applies if the new column is MEDIUMTEXT
			if (!ColumnType.MEDIUMTEXT.equals(newColumn.getColumnType())) {
				return;
			}
			// Only applies if the old column is LARGETEXT, all other columns fit in the MEDIUMTEXT
			if (!ColumnType.LARGETEXT.equals(oldColumn.getColumnType())) {
				return;
			}
			
			tableIndexDao.tempTableColumnExceedsCharacterLimit(tableId, oldColumn.getId(), ColumnConstants.MAX_MEDIUM_TEXT_CHARACTERS).ifPresent( rowId -> {
				throw new IllegalArgumentException("Cannot change column \"" + oldColumn.getName() + "\" to MEDIUMTEXT: "
						+ "The data at row " + rowId + " exceeds the MEDIUMTEXT limit of " + ColumnConstants.MAX_MEDIUM_TEXT_CHARACTERS + " characters.");
			});
			
		});
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
	public long populateViewFromEntityReplication(final Long viewId, final ViewScopeType scopeType, final List<ColumnModel> currentSchema) {
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(currentSchema, "currentSchema");
		
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(scopeType.getObjectType());
		ViewFilter filter = provider.getViewFilter(viewId);		
		// copy the data from the entity replication tables to table's index
		try {
			tableIndexDao.copyObjectReplicationToView(viewId, filter, currentSchema, provider);			
		} catch (Exception e) {
			// if the copy failed. Attempt to determine the cause.
			determineCauseOfReplicationFailure(e, currentSchema, provider, scopeType.getTypeMask(), filter);
		}

		// Returns the next version of the view
		return getCurrentVersionOfIndex(IdAndVersion.newBuilder().setId(viewId).build()) + 1;
	}
		
	@Override
	public ColumnModelPage getPossibleColumnModelsForScope(ViewScope scope, String nextPageToken, boolean excludeDerivedKeys) {
		ValidateArgument.required(scope, "scope");
		ValidateArgument.requiredNotEmpty(scope.getScope(), "scope.scopeIds");
		
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
		Set<IdAndVersion> scopeSet = scope.getScope().stream().map(IdAndVersion::parse).collect(Collectors.toSet());
		
		return getPossibleAnnotationDefinitionsForContainerIds(scopeType, scopeSet, nextPageToken, excludeDerivedKeys);
	}
	
	
	/**
	 * Get the possible annotations for the given set of container IDs.
	 * 
	 * @param containerIds
	 * @param nextPageToken Optional: Controls pagination.
	 * @return
	 */
	ColumnModelPage getPossibleAnnotationDefinitionsForContainerIds(ViewScopeType viewScopeType, Set<IdAndVersion> scope, String nextPageToken, boolean excludeDerivedKeys) {
		ValidateArgument.required(scope, "scope");
		NextPageToken token =  new NextPageToken(nextPageToken);
		ColumnModelPage results = new ColumnModelPage();		
		
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(viewScopeType.getObjectType());
		ViewFilter filter = provider.getViewFilter(viewScopeType.getTypeMask(), scope);
		
		if(filter.isEmpty()) {
			results.setResults(Collections.emptyList());
			results.setNextPageToken(null);
			return results;
		}
		
		DefaultColumnModel defaultColumnModel = provider.getDefaultColumnModel(viewScopeType.getTypeMask());
		
		// We exclude from the suggested column models the custom fields defined for the object (since they are included in the default column model itself)
		List<String> excludeKeys = getAnnotationKeysExcludeList(defaultColumnModel);
		
		if (excludeKeys != null) {
			filter = filter.newBuilder().addExcludeAnnotationKeys(new HashSet<>(excludeKeys)).build();
		}
		
		// Make sure that the derived keys are excluded if they are not requested
		if (excludeDerivedKeys) {
			filter = filter
					.newBuilder()
					.setExcludeDerivedKeys(true)
					.build();
		}

		// request one page with a limit one larger than the passed limit.
		List<ColumnModel> columns = tableIndexDao.getPossibleColumnModelsForContainers(filter, token.getLimitForQuery(), token.getOffset());
		results.setNextPageToken(token.getNextPageTokenForCurrentResults(columns));
		results.setResults(columns);
		return results;
	}
	
	@Override
	public void buildIndexToChangeNumber(final ProgressCallback progressCallback, final IdAndVersion idAndVersion,
			final Iterator<TableChangeMetaData> iterator) throws RecoverableMessageException {
		try {
			// Run with the exclusive lock on the table if we can get it.
			tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, new LockContext(ContextType.BuildTableIndex, idAndVersion), idAndVersion,
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
			
			// Try to restore the table first from an existing snapshot
			attemptToRestoreTableFromExistingSnapshot(idAndVersion, tableResetToken, targetChangeNumber.get());
			
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
			// This is not an error we can recover from.
			log.error("Unrecoverable failure to update table index " + idAndVersion, e);
			// Any other error is a table failure.
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, e);
		}
	}
	
	void attemptToRestoreTableFromExistingSnapshot(IdAndVersion idAndVersion, String tableResetToken, long targetChangeNumber) {
				
		// If there are any changes that are already applied to the table we let it build as normal
		if (tableIndexDao.getMaxCurrentCompleteVersionForTable(idAndVersion) > -1L) {
			return;
		}
		
		tableManagerSupport.getMostRecentTableSnapshot(idAndVersion).ifPresent( snapshot -> {
			IdAndVersion snapshotId = IdAndVersion.newBuilder().setId(snapshot.getTableId()).setVersion(snapshot.getVersion()).build();
			
			// Keeps the change number that correspond with the snapshot
			long snapshotChangeNumber = tableManagerSupport.getLastTableChangeNumber(snapshotId).orElseThrow(
				() -> new IllegalStateException("Expected a change number for snapshot " + snapshotId + ", but found none.")
			);
			
			log.info("Restoring table " + idAndVersion + " from snapshot " + snapshotId + "...");
			
			tableManagerSupport.attemptToUpdateTableProgress(
				idAndVersion, 
				tableResetToken, 
				"Restoring table " + idAndVersion + " from snapshot " + snapshotId, 
				snapshotChangeNumber, 
				targetChangeNumber
			);
			
			TableIndexDescription indexDescription = new TableIndexDescription(idAndVersion);
			
			// We now reset the table index with the schema of the snapshot
			List<ColumnModel> snapshotSchema = tableManagerSupport.getTableSchema(snapshotId);
			
			// Make sure the search flag is synched with the snapshot
			boolean isSearchEnabled = tableManagerSupport.isTableSearchEnabled(snapshotId);
			
			resetTableIndex(indexDescription, snapshotSchema, isSearchEnabled);
			
			// Restore the table data from the snapshot
			tableManagerSupport.restoreTableIndexFromS3(idAndVersion, snapshot.getBucket(), snapshot.getKey());
			
			// Now build the secondary indicies, using the restored snapshot schema
			buildTableIndexIndices(indexDescription, snapshotSchema);
						
			// Now sync the change number that correspond to the snapshot
			setIndexVersion(idAndVersion, snapshotChangeNumber);
			
			log.info("Restoring table " + idAndVersion + " from snapshot " + snapshotId + "...DONE");
		});
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
		List<ColumnChangeDetails> changes = setIndexSchema(new TableIndexDescription(idAndVersion), boundSchema);
		
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
		case SEARCH:
			applySearchChangeToIndex(idAndVersion, changeMetadata.loadChangeData(SearchChange.class));
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
		TableIndexDescription index = new TableIndexDescription(idAndVersion);
		List<ColumnChangeDetails> changes = schemaChangeData.getChange().getDetails();
		
		updateTableSchema(index, changes);
		
		updateSearchIndexFromSchemaChange(index, changes);

		boolean alterTemp = false;
		alterListColumnIndexTableWithSchemaChange(idAndVersion, changes, alterTemp);

		// set the new max version for the index
		setIndexVersion(idAndVersion, schemaChangeData.getChangeNumber());
	}

	void alterListColumnIndexTableWithSchemaChange(IdAndVersion idAndVersion, List<ColumnChangeDetails> columnChangeDetails, boolean alterTemp) {
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
		TableIndexDescription index = new TableIndexDescription(idAndVersion);
		// Get the change set.
		SparseChangeSet sparseChangeSet = rowChange.getChange();
		// match the schema to the change set.
		List<ColumnChangeDetails> changes = setIndexSchema(index, sparseChangeSet.getSchema());
		
		updateSearchIndexFromSchemaChange(index, changes);
		
		// attempt to apply this change set to the table.
		applyChangeSetToIndex(idAndVersion, sparseChangeSet, rowChange.getChangeNumber());
	}
	
	void applySearchChangeToIndex(IdAndVersion idAndVersion, ChangeData<SearchChange> loadChangeData) {
		IndexDescription index = new TableIndexDescription(idAndVersion);
		// This will make sure that the table is properly created if it does not exist
		createTableIfDoesNotExist(index);
				
		SearchChange change = loadChangeData.getChange();
		
		if (change.isEnabled()) {
			// When we enable the search on a table we unconditionally re-index the whole table
			updateSearchIndex(index);
		} else {
			tableIndexDao.clearSearchIndex(idAndVersion);
		}
		
		// Update the search status
		setSearchEnabled(idAndVersion, change.isEnabled());
		// set the new max version for the index
		setIndexVersion(idAndVersion, loadChangeData.getChangeNumber());
	}
	
	void updateSearchIndexFromSchemaChange(TableIndexDescription index, List<ColumnChangeDetails> changes) {
				
		if (!isRequireSearchIndexUpdate(index.getIdAndVersion(), changes)) {
			return;
		}
		
		if (!tableIndexDao.isSearchEnabled(index.getIdAndVersion())) {
			return;
		}
		
		updateSearchIndex(index);
	}
	
	@Override
	public Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, ViewFilter filter,
			long limit) {
				
		return tableIndexDao.getOutOfDateRowsForView(viewId, filter, limit);
	}
	
	@Override
	public void updateViewRowsInTransaction(IndexDescription index, ViewScopeType scopeType,
			List<ColumnModel> currentSchema, ViewFilter filter) {
		ValidateArgument.required(index, "index");
		ValidateArgument.required(scopeType, "scopeType");
		ValidateArgument.required(currentSchema, "currentSchema");
		MetadataIndexProvider provider = metadataIndexProviderFactory.getMetadataIndexProvider(scopeType.getObjectType());
		IdAndVersion viewId = index.getIdAndVersion();
		boolean isSearchEnabled = tableIndexDao.isSearchEnabled(viewId);
		List<ColumnModel> searchSchema = getSchemaForSearchIndex(currentSchema);
		// all calls are in a single transaction.
		tableIndexDao.executeInWriteTransaction((TransactionStatus status) -> {
			Set<Long> rowIdsSet = filter.getLimitObjectIds().get();
			Long[] rowsIdsArray = rowIdsSet.stream().toArray(Long[] ::new);
 			// First delete the provided rows from the view
			tableIndexDao.deleteRowsFromViewBatch(viewId, rowsIdsArray);
			try {
				// Apply any updates to the view for the given Ids
				tableIndexDao.copyObjectReplicationToView(viewId.getId(), filter, currentSchema, provider);
				populateListColumnIndexTables(viewId, currentSchema, filter.getLimitObjectIds().get());
				
				if (isSearchEnabled & !searchSchema.isEmpty()) {
					List<TableRowData> rowsData = tableIndexDao.getTableDataForRowIds(viewId, searchSchema, rowIdsSet);
					updateSearchIndex(index, rowsData.iterator());
				}
				
			} catch (Exception e) {
				// if the copy failed. Attempt to determine the cause.  This will always throw an exception.
				determineCauseOfReplicationFailure(e, currentSchema, provider, scopeType.getTypeMask(), filter);
			}
			return null;
		});
	}
	
	void determineCauseOfReplicationFailure(Exception exception, List<ColumnModel> currentSchema,
			MetadataIndexProvider provider, Long viewTypeMask, ViewFilter filter) {
		
		// This can be cause by concurrent modifications on the object replication index, we can retry later
		if (exception instanceof PessimisticLockingFailureException) {
			throw new RecoverableMessageException(exception);
		}
		
		DefaultColumnModel defaultColumnModel = provider.getDefaultColumnModel(viewTypeMask);
		
		List<String> excludeKeys = getAnnotationKeysExcludeList(defaultColumnModel);
		if(excludeKeys != null) {
			filter = filter.newBuilder().addExcludeAnnotationKeys(new HashSet<>(excludeKeys)).build();
		}

		// Calculate the schema from the annotations
		List<ColumnModel> schemaFromAnnotations = tableIndexDao.getPossibleColumnModelsForContainers(filter, Long.MAX_VALUE, 0L);
		
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
	

	@Override
	public void refreshViewBenefactors(final IdAndVersion viewId) {
		ValidateArgument.required(viewId, "viewId");
		ViewScopeType scopeType = tableManagerSupport.getViewScopeType(viewId);
		tableIndexDao.refreshViewBenefactors(viewId, scopeType.getObjectType().getMainType());
	}
	
	@Override
	public void updateObjectReplication(ReplicationType replicationType, Iterator<ObjectDataDTO> objectData) {
		updateObjectReplication(replicationType, objectData, BATCH_SIZE);
	}
	
	void updateObjectReplication(ReplicationType replicationType, Iterator<ObjectDataDTO> objectData, int batchSize) {

		Iterators.partition(objectData, batchSize).forEachRemaining(batch -> {
			try {
				List<Long> distinctIdsInBatch = batch.stream().map(i -> i.getId()).distinct().collect(Collectors.toList());
				tableIndexDao.executeInWriteTransaction((TransactionStatus status) -> {
					/*
					 * We delete all rows for all objects of the given type. This ensures that any
					 * deleted annotations or versions are also removed from the replication tables.
					 */
					tableIndexDao.deleteObjectData(replicationType, distinctIdsInBatch);

					/*
					 * Add back all of the remaining data in batches.
					 */
					tableIndexDao.addObjectData(replicationType, batch);
					return null;
				});
			}catch(Exception e) {
				// The fix for PLFM-4497 is to retry failed batches as individuals.
				if(batch.size() > 1) {
					// throwing a RecoverableMessageException will result in an attempt to update each object separately.
					throw new RecoverableMessageException(e);
				}else {
					
					throw new IllegalArgumentException(e);
				}
			}

		});
	}
		
	@Override
	public void deleteObjectData(ReplicationType objectType, List<Long> toDeleteIds) {
		if(toDeleteIds != null && !toDeleteIds.isEmpty()) {
			tableIndexDao.executeInWriteTransaction((TransactionStatus status) -> {
				tableIndexDao.deleteObjectData(objectType, toDeleteIds);
				return null;
			});
		}
	}
	
	@Override
	public Iterator<IdAndChecksum> streamOverIdsAndChecksums(Long salt, ViewFilter filter) {
		return new PaginationIterator<IdAndChecksum>((long limit, long offset) -> {
			return tableIndexDao.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		}, BATCH_SIZE);
	}
	
	@Override
	public boolean isViewSynchronizeLockExpired(ReplicationType type, IdAndVersion idAndVersion) {
		return tableIndexDao.isSynchronizationLockExpiredForObject(type,idAndVersion.getId());
	}

	@Override
	public void resetViewSynchronizeLock(ReplicationType type, IdAndVersion idAndVersion) {
		// re-set the expiration for all containers that were synchronized.
		long newExpirationDateMs = System.currentTimeMillis() + SYNCHRONIZATION_FEQUENCY_MS;
		tableIndexDao.setSynchronizationLockExpiredForObject(type, idAndVersion.getId(), newExpirationDateMs);
	}
		
	@Override
	public List<ColumnModel> resetTableIndex(IndexDescription index) {
		List<ColumnModel> schema = tableManagerSupport.getTableSchema(index.getIdAndVersion());
		boolean isSearchEnabled = tableManagerSupport.isTableSearchEnabled(index.getIdAndVersion());
		return resetTableIndex(index, schema, isSearchEnabled);
	}
	
	@Override
	public List<ColumnModel> resetTableIndex(IndexDescription index, List<ColumnModel> schema, boolean isSearchEnabled) {
		// Clear the table index
		deleteTableIndex(index.getIdAndVersion());
		
		// Set the schema
		setIndexSchema(index, schema);

		// Sync the search flag
		setSearchEnabled(index.getIdAndVersion(), isSearchEnabled);
		
		return schema;
	}

	@Override
	public void buildTableIndexIndices(IndexDescription index, List<ColumnModel> schema) {
		// Optimize the table
		optimizeTableIndices(index.getIdAndVersion());
		
		
		// Makes sure to populate the multi-value indices
		populateListColumnIndexTables(index.getIdAndVersion(), schema);
		
		// Re-build the search index if needed
		if (tableIndexDao.isSearchEnabled(index.getIdAndVersion())) {
			updateSearchIndex(index);
		}
		
		// For tables we also need to populate the file handle index (See https://sagebionetworks.jira.com/browse/PLFM-7678)
		if (TableType.table.equals(index.getTableType())) {
			populateFileHandleIndex(index, schema);
		}
	}
	
	void populateFileHandleIndex(IndexDescription index, List<ColumnModel> schema) {
		// Only consider the columns that points to file handles
		List<ColumnModel> filesSchema = schema.stream().filter(column -> ColumnType.FILEHANDLEID.equals(column.getColumnType())).collect(Collectors.toList());
		
		if (filesSchema.isEmpty()) {
			return;
		}
		
		Iterator<TableRowData> filesDataIterator = new PaginationIterator<>((limit, offset) -> {
			return tableIndexDao.getTableDataPage(index.getIdAndVersion(), filesSchema, limit, offset);
		}, BATCH_SIZE);
	
		Iterators.partition(filesDataIterator, BATCH_SIZE).forEachRemaining(batch -> {
			Set<Long> fileHandleIdBatch = batch.stream()
				.flatMap(rowData -> rowData.getRowValues().stream())
				.filter(cellValue -> cellValue != null && !StringUtils.isEmpty(cellValue.getRawValue()))
				.map(cellValue -> Long.valueOf(cellValue.getRawValue()))
				.collect(Collectors.toSet());
			
			tableIndexDao.applyFileHandleIdsToTable(index.getIdAndVersion(), fileHandleIdBatch);
		});
	}
	
	/**
	 * @param tableId The id of the table
	 * @return The schema currently used by the table in the index
	 */
	List<ColumnModel> getCurrentTableSchema(IdAndVersion tableId) {
		// Get the current schema.
		List<DatabaseColumnInfo> tableInfo = tableIndexDao.getDatabaseInfo(tableId);
		// Determine the current schema
		return SQLUtils.extractSchemaFromInfo(tableInfo);
	}
	
	/**
	 * @param schema
	 * @return The sub-schema consisting of columns that are eligible to be added to the search index
	 */
	List<ColumnModel> getSchemaForSearchIndex(List<ColumnModel> schema) {
		return schema.stream()
				.filter(TableIndexManagerImpl::isColumnEligibleForSearchIndex)
				.collect(Collectors.toList());
	}
	
	/**
	 * Updates the search index for the table with the given id
	 * 
	 * @param tableId
	 */
	void updateSearchIndex(IndexDescription index) {
		List<ColumnModel> currentSchema = getCurrentTableSchema(index.getIdAndVersion());
		List<ColumnModel> searchIndexSchema = getSchemaForSearchIndex(currentSchema);
		
		if (searchIndexSchema.isEmpty()) {
			// On a schema change it might happen that now no columns are eligible for indexing, for such cases
			// we simply clear the search index
			tableIndexDao.clearSearchIndex(index.getIdAndVersion());
			return;
		}
				
		Iterator<TableRowData> tableRowsIterator = new PaginationIterator<>((PaginationProvider<TableRowData>) (limit, offset) -> {
			return tableIndexDao.getTableDataPage(index.getIdAndVersion(), searchIndexSchema, limit, offset);
		}, BATCH_SIZE);
		
		updateSearchIndex(index, tableRowsIterator);
	}
	
	/**
	 * Given an iterator over the rows of a table updates the search index re-computing the index values for each
	 * row returned by the iterator
	 * 
	 * @param tableId
	 * @param tableRowDataIterator
	 */
	void updateSearchIndex(IndexDescription index, Iterator<TableRowData> tableRowDataIterator) {
		boolean includeRowId = index.addRowIdToSearchIndex();
		Iterators.partition(tableRowDataIterator, BATCH_SIZE).forEachRemaining(batch -> {
			List<RowSearchContent> transformedBatch = batch.stream()
				.map((rawData) -> mapTableRowDataToSearchContent(rawData, includeRowId))
				.collect(Collectors.toList());
			
			if (!transformedBatch.isEmpty()) {
				tableIndexDao.updateSearchIndex(index.getIdAndVersion(), transformedBatch);
			}
		});
	}
	
	private RowSearchContent mapTableRowDataToSearchContent(TableRowData rowData, boolean includeRowId) {
		String processedValue = searchProcessor.process(rowData, includeRowId);
		return new RowSearchContent(rowData.getRowId(), processedValue);
	}
	
	/**
	 * A change in the schema of a table requires a full re-index of the table if:
	 * 
	 * 1. A column that was eligible for the search index was deleted
	 * 2. An existing column non-eligible for the search index was updated to a column that is now eligible
	 * 3. An existing column eligible for the search index was updated to a column that is now not eligible
	 * 
	 * @param tableId
	 * @param changes
	 * @return True if the given changes for the table require a full re-index of the search index of the table
	 */
	static boolean isRequireSearchIndexUpdate(IdAndVersion tableId, List<ColumnChangeDetails> changes) {
		if (changes.isEmpty()) {
			return false;
		}
		
		for (ColumnChangeDetails change : changes) {
			// For a new column there is not data yet so no need to re-index
			if (change.getOldColumn() == null) {
				continue;
			}
			// A deleted column: requires re-indexing only if the type of the column is eligible for the search index
			if (change.getNewColumn() == null && isColumnEligibleForSearchIndex(change.getOldColumn())) {
				return true;
			}
			// An updated column: requires re-indexing if the old column or the new column are eligible for the search index
			if (change.getNewColumn() != null && (isColumnEligibleForSearchIndex(change.getNewColumn()) || isColumnEligibleForSearchIndex(change.getOldColumn()))) {
				return true;
			}
			
		}
		
		return false;
	}

	/**
	 * @param column
	 * @return True if the given column model has a data type that is eligible to be added to the search index
	 */
	private static boolean isColumnEligibleForSearchIndex(ColumnModel column) {
		return TableConstants.SEARCH_TYPES.contains(column.getColumnType());
	}
	
	@Override
	public Long populateMaterializedViewFromDefiningSql(List<ColumnModel> viewSchema, QueryTranslator definingSql) {
		IndexDescription indexDescription = definingSql.getIndexDescription();
		
		return tableIndexDao.executeInWriteTransaction((TransactionStatus status) -> {
			String insertSql = SQLTranslatorUtils.createMaterializedViewInsertSql(viewSchema, definingSql.getOutputSQL(), indexDescription);
			tableIndexDao.update(insertSql, definingSql.getParameters());
			return 1L;
		});
	}
	
	@Override
	public void swapTableIndex(IndexDescription source, IndexDescription target) {
		tableIndexDao.swapTableIndex(source.getIdAndVersion(), target.getIdAndVersion());
	}
	
}
