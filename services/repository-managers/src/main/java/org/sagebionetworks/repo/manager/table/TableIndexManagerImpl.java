package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SchemaChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class TableIndexManagerImpl implements TableIndexManager {
	
	static private Logger log = LogManager.getLogger(TableIndexManagerImpl.class);

	public static final int MAX_MYSQL_INDEX_COUNT = 60; // mysql only supports a max of 64 secondary indices per table.
	
	private final TableIndexDAO tableIndexDao;
	private final TableManagerSupport tableManagerSupport;
	
	public TableIndexManagerImpl(TableIndexDAO dao, TableManagerSupport tableManagerSupport){
		if(dao == null){
			throw new IllegalArgumentException("TableIndexDAO cannot be null");
		}
		if(tableManagerSupport == null){
			throw new IllegalArgumentException("TableManagerSupport cannot be null");
		}
		this.tableIndexDao = dao;
		this.tableManagerSupport = tableManagerSupport;
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
					.executeInWriteTransaction(new TransactionCallback<Void>() {
						@Override
						public Void doInTransaction(TransactionStatus status) {
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
							// set the new max version for the index
							tableIndexDao.setMaxCurrentCompleteVersionForTable(
									tableId, changeSetVersionNumber);
							return null;
						}
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
	public void setIndexSchema(final IdAndVersion tableId, boolean isTableView, List<ColumnModel> newSchema){
		// Lookup the current schema of the index
		List<DatabaseColumnInfo> currentSchema = tableIndexDao.getDatabaseInfo(tableId);
		// create a change that replaces the old schema as needed.
		List<ColumnChangeDetails> changes = SQLUtils.createReplaceSchemaChange(currentSchema, newSchema);
		updateTableSchema(tableId, isTableView, changes);
	}

	@Override
	public void deleteTableIndex(final IdAndVersion tableId) {
		// delete all tables for this index.
		tableIndexDao.deleteTable(tableId);
		tableIndexDao.deleteSecondaryTables(tableId);
	}
	
	@Override
	public String getCurrentSchemaMD5Hex(final IdAndVersion tableId) {
		return tableIndexDao.getCurrentSchemaMD5Hex(tableId);
	}
	
	@Override
	public void setIndexVersion(final IdAndVersion tableId, Long versionNumber) {
		tableIndexDao.setMaxCurrentCompleteVersionForTable(
				tableId, versionNumber);
	}
	
	@Override
	public void setIndexVersionAndSchemaMD5Hex(final IdAndVersion tableId, Long viewCRC, String schemaMD5Hex) {
		tableIndexDao.setIndexVersionAndSchemaMD5Hex(tableId, viewCRC, schemaMD5Hex);
	}
	
	
	@Override
	public boolean updateTableSchema(final IdAndVersion tableId, boolean isTableView, List<ColumnChangeDetails> changes) {
		// create the table if it does not exist
		tableIndexDao.createTableIfDoesNotExist(tableId, isTableView);
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(tableId);
		boolean alterTemp = false;
		// Alter the table
		boolean wasSchemaChanged = alterTableAsNeededWithProgress(tableId, changes, alterTemp);
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
	public boolean alterTempTableSchmea(final IdAndVersion tableId, final List<ColumnChangeDetails> changes){
		boolean alterTemp = true;
		return alterTableAsNeededWithProgress(tableId, changes, alterTemp);
	}
	
	/**
	 * Alter the table schema using an auto-progressing callback.
	 * @param progressCallback
	 * @param tableId
	 * @param changes
	 * @return
	 * @throws Exception
	 */
	boolean alterTableAsNeededWithProgress(final IdAndVersion tableId, final List<ColumnChangeDetails> changes, final boolean alterTemp){
		try {
			return alterTableAsNeededWithinAutoProgress(tableId, changes, alterTemp);
		} catch (Exception e) {
			throw new RuntimeException(e);
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
	public void createTemporaryTableCopy(final IdAndVersion tableId, ProgressCallback callback) {
		// creating a temp table can take a long time so auto-progress is used.
		try {
			// create the table.
			tableIndexDao.createTemporaryTable(tableId);
			// copy all the data from the original to the temp.
			tableIndexDao.copyAllDataToTemporaryTable(tableId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	@Override
	public void deleteTemporaryTableCopy(final IdAndVersion tableId, ProgressCallback callback) {
		// delete
		tableIndexDao.deleteTemporaryTable(tableId);
	}
	@Override
	public Long populateViewFromEntityReplication(final Long tableId, final ProgressCallback callback, final Long viewTypeMask,
			final Set<Long> allContainersInScope, final List<ColumnModel> currentSchema) {
		ValidateArgument.required(callback, "callback");
		try {
			return populateViewFromEntityReplicationWithProgress(tableId,
					viewTypeMask, allContainersInScope, currentSchema);
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw ((RuntimeException) e);
			} else {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Populate the view table from the entity replication tables.
	 * After the view has been populated a the sum of the cyclic redundancy check (CRC)
	 * will be calculated on the concatenation of ROW_ID & ETAG of the resulting table.
	 * 
	 * @param viewType
	 * @param allContainersInScope
	 * @param currentSchema
	 * @return The CRC32 of the concatenation of ROW_ID & ETAG of the table after the update.
	 * @throws Exception 
	 */
	Long populateViewFromEntityReplicationWithProgress(final Long tableId, Long viewTypeMask, Set<Long> allContainersInScope, List<ColumnModel> currentSchema) throws Exception{
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		ValidateArgument.required(currentSchema, "currentSchema");
		// copy the data from the entity replication tables to table's index
		try {
			tableIndexDao.copyEntityReplicationToTable(tableId, viewTypeMask, allContainersInScope, currentSchema);
		} catch (Exception e) {
			// if the copy failed. Attempt to determine the cause.
			determineCauseOfReplicationFailure(e, currentSchema,  allContainersInScope, viewTypeMask);
		}
		// calculate the new CRC32;
		return tableIndexDao.calculateCRC32ofTableView(tableId);
	}
	
	/**
	 * Attempt to determine the cause of a replication failure.
	 * 
	 * @param exception The exception thrown during replication.
	 * @param currentSchema
	 * @throws Exception 
	 */
	public void determineCauseOfReplicationFailure(Exception exception, List<ColumnModel> currentSchema, Set<Long> containersInScope, Long viewTypeMask) throws Exception{
		// Calculate the schema from the annotations
		List<ColumnModel> schemaFromAnnotations = tableIndexDao.getPossibleColumnModelsForContainers(containersInScope, viewTypeMask, Long.MAX_VALUE, 0L);
		// check the 
		SQLUtils.determineCauseOfException(exception, currentSchema, schemaFromAnnotations);
		// Have not determined the cause so throw the original exception
		throw exception;
	}
	
	@Override
	public ColumnModelPage getPossibleColumnModelsForView(
			final Long viewId, String nextPageToken) {
		ValidateArgument.required(viewId, "viewId");
		IdAndVersion idAndVersion = IdAndVersion.newBuilder().setId(viewId).build();
		Long type = tableManagerSupport.getViewTypeMask(idAndVersion);
		Set<Long> containerIds = tableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, type);
		return getPossibleAnnotationDefinitionsForContainerIds(containerIds, type, nextPageToken);
	}
	
	@Override
	public ColumnModelPage getPossibleColumnModelsForScope(
			ViewScope scope, String nextPageToken) {
		ValidateArgument.required(scope, "scope");
		ValidateArgument.required(scope.getScope(), "scope.scopeIds");
		long viewTypeMask = ViewTypeMask.getViewTypeMask(scope);
		// lookup the containers for the given scope
		Set<Long> scopeSet = new HashSet<Long>(KeyFactory.stringToKey(scope.getScope()));
		Set<Long> containerIds = tableManagerSupport.getAllContainerIdsForScope(scopeSet, viewTypeMask);
		return getPossibleAnnotationDefinitionsForContainerIds(containerIds, viewTypeMask, nextPageToken);
	}
	
	
	/**
	 * Get the possible annotations for the given set of container IDs.
	 * 
	 * @param containerIds
	 * @param nextPageToken Optional: Controls pagination.
	 * @return
	 */
	ColumnModelPage getPossibleAnnotationDefinitionsForContainerIds(
			Set<Long> containerIds, Long viewTypeMask, String nextPageToken) {
		ValidateArgument.required(containerIds, "containerIds");
		NextPageToken token =  new NextPageToken(nextPageToken);
		ColumnModelPage results = new ColumnModelPage();
		if(containerIds.isEmpty()){
			results.setResults(new LinkedList<ColumnModel>());
			results.setNextPageToken(null);
			return results;
		}
		// request one page with a limit one larger than the passed limit.
		List<ColumnModel> columns = tableIndexDao.getPossibleColumnModelsForContainers(containerIds, viewTypeMask, token.getLimitForQuery(), token.getOffset());
		results.setNextPageToken(token.getNextPageTokenForCurrentResults(columns));
		results.setResults(columns);
		return results;
	}
	
	@Override
	public void buildIndexToChangeNumber(final ProgressCallback progressCallback, final IdAndVersion idAndVersion,
			final Iterator<TableChangeMetaData> iterator, final long targetChangeNumber) throws RecoverableMessageException {
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
		// Attempt to run with
		try {

			// Run with the exclusive lock on the table if we can get it.
			String lastEtag = tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, idAndVersion, 120,
					(ProgressCallback callback) -> {
						return buildIndexToChangeNumberWithExclusiveLock(idAndVersion, iterator, targetChangeNumber,
								tableResetToken);
					});
			log.info("Completed index update for: " + idAndVersion);
			tableManagerSupport.attemptToSetTableStatusToAvailable(idAndVersion, tableResetToken, lastEtag);
		} catch (LockUnavilableException | TableUnavailableException | InterruptedException| IOException e) {
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			// Any other error is a table failure.
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, tableResetToken, e);
			// This is not an error we can recover from.
			log.info("Unrecoverable failure to update table index: "+idAndVersion);
		}
	}
	
	/**
	 * Note: The caller must be holding an exclusive lock on table while calling this method.
	 * Build the table index
	 * @param tableId
	 * @param iterator
	 * @param lastChangeNumber
	 * @param tableResetToken
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	String buildIndexToChangeNumberWithExclusiveLock(final IdAndVersion idAndVersion, final Iterator<TableChangeMetaData> iterator,
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
				appleyChangeToIndex(idAndVersion, changeMetadata);
				lastEtag = changeMetadata.getETag();
			}
		}

		if(!idAndVersion.getVersion().isPresent()) {
			/*
			 * When building a table to the current version, we unconditionally apply the
			 * current table schema to the index as a workaround for PLFM-5639. This is a
			 * fix for tables with schema changes that were not captured in the table's
			 * history.
			 */
			List<ColumnModel> boundSchema = tableManagerSupport.getColumnModelsForTable(idAndVersion);
			boolean isTableView = false;
			setIndexSchema(idAndVersion, isTableView, boundSchema);
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
	void appleyChangeToIndex(IdAndVersion idAndVersion, TableChangeMetaData changeMetadata) throws NotFoundException, IOException {
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
		setIndexVersion(idAndVersion, schemaChangeData.getChangeNumber());
	}
	
	/**
	 * Apply the provided row change set to the provied table's index.
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

}
