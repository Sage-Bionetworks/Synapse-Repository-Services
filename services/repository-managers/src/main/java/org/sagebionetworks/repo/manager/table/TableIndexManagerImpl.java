package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class TableIndexManagerImpl implements TableIndexManager {
	
	public static final int MAX_MYSQL_INDEX_COUNT = 63; // mysql only supports a max of 64 secondary indices per table.
	
	private final TableIndexDAO tableIndexDao;
	private final TableManagerSupport tableManagerSupport;
	private final String tableId;
	
	public TableIndexManagerImpl(TableIndexDAO dao, TableManagerSupport tableManagerSupport, String tableId){
		if(dao == null){
			throw new IllegalArgumentException("TableIndexDAO cannot be null");
		}
		if(tableManagerSupport == null){
			throw new IllegalArgumentException("TableManagerSupport cannot be null");
		}
		if(tableId == null){
			throw new IllegalArgumentException("TableId cannot be null");
		}
		this.tableIndexDao = dao;
		this.tableManagerSupport = tableManagerSupport;
		this.tableId = tableId;
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
	public long getCurrentVersionOfIndex() {
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
	public void applyChangeSetToIndex(final RowSet rowset, final List<ColumnModel> currentSchema,
			final long changeSetVersionNumber) {
		// Validate all rows have the same version number
		TableModelUtils.validateRowVersions(rowset.getRows(),
				changeSetVersionNumber);
		// Has this version already been applied to the table index?
		final long currentVersion = tableIndexDao
				.getMaxCurrentCompleteVersionForTable(tableId);
		if (changeSetVersionNumber > currentVersion) {
			// apply all changes in a transaction
			tableIndexDao
					.executeInWriteTransaction(new TransactionCallback<Void>() {
						@Override
						public Void doInTransaction(TransactionStatus status) {
							// apply the change to the index
							tableIndexDao.createOrUpdateOrDeleteRows(rowset,
									currentSchema);
							// Extract all file handle IDs from this set
							Set<Long> fileHandleIds = TableModelUtils
									.getFileHandleIdsInRowSet(rowset);
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
	
	@Override
	public void applyChangeSetToIndex(final RowSet rowset,
			final List<ColumnModel> currentSchema) {
		// apply all changes in a transaction
		tableIndexDao
				.executeInWriteTransaction(new TransactionCallback<Void>() {
					@Override
					public Void doInTransaction(TransactionStatus status) {
						// apply the change to the index
						tableIndexDao.createOrUpdateOrDeleteRows(rowset,
								currentSchema);
						return null;
					}
				});
		
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
	public boolean isVersionAppliedToIndex(long versionNumber) {
		final long currentVersion = tableIndexDao.getMaxCurrentCompleteVersionForTable(tableId);
		return currentVersion >= versionNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableIndexManager#setIndexSchema
	 * (org
	 * .sagebionetworks.repo.manager.table.TableIndexManager.TableIndexConnection
	 * , java.util.List)
	 */
	@Override
	public void setIndexSchema(ProgressCallback<Void> progressCallback, List<ColumnModel> newSchema) {
		// Lookup the current schema of the index
		List<DatabaseColumnInfo> currentSchema = tableIndexDao.getDatabaseInfo(tableId);
		// create a change that replaces the old schema as needed.
		List<ColumnChangeDetails> changes = SQLUtils.createReplaceSchemaChange(currentSchema, newSchema);
		updateTableSchema(progressCallback, changes);
	}

	@Override
	public void deleteTableIndex() {
		// delete all tables for this index.
		tableIndexDao.deleteTable(tableId);
		tableIndexDao.deleteSecondayTables(tableId);
	}
	
	@Override
	public String getCurrentSchemaMD5Hex() {
		return tableIndexDao.getCurrentSchemaMD5Hex(tableId);
	}
	
	@Override
	public void setIndexVersion(Long versionNumber) {
		tableIndexDao.setMaxCurrentCompleteVersionForTable(
				tableId, versionNumber);
	}
	
	@Override
	public void setIndexVersionAndSchemaMD5Hex(Long viewCRC, String schemaMD5Hex) {
		tableIndexDao.setIndexVersionAndSchemaMD5Hex(tableId, viewCRC, schemaMD5Hex);
	}
	
	
	@Override
	public boolean updateTableSchema(ProgressCallback<Void> progressCallback, List<ColumnChangeDetails> changes) {
		// create the table if it does not exist
		tableIndexDao.createTableIfDoesNotExist(tableId);
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(tableId);
		boolean alterTemp = false;
		// Alter the table
		boolean wasSchemaChanged = alterTableAsNeededWithProgress(progressCallback, tableId, changes, alterTemp);
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
			String schemaMD5Hex = TableModelUtils.createSchemaMD5HexCM(currentSchema);
			tableIndexDao.setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
		}
		return wasSchemaChanged;
	}
	
	@Override
	public boolean alterTempTableSchmea(ProgressCallback<Void> progressCallback, final String tableId, final List<ColumnChangeDetails> changes){
		boolean alterTemp = true;
		return alterTableAsNeededWithProgress(progressCallback, tableId, changes, alterTemp);
	}
	
	/**
	 * Alter the table schema using an auto-progressing callback.
	 * @param progressCallback
	 * @param tableId
	 * @param changes
	 * @return
	 * @throws Exception
	 */
	private boolean alterTableAsNeededWithProgress(ProgressCallback<Void> progressCallback, final String tableId, final List<ColumnChangeDetails> changes, final boolean alterTemp){
		 try {
			return  tableManagerSupport.callWithAutoProgress(progressCallback, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return tableIndexDao.alterTableAsNeeded(tableId, changes, alterTemp);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableIndexManager#optimizeTableIndices()
	 */
	@Override
	public void optimizeTableIndices() {
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
	public void createTemporaryTableCopy(ProgressCallback<Void> callback) {
		// creating a temp table can take a long time so auto-progress is used.
		 try {
			tableManagerSupport.callWithAutoProgress(callback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					// create the table.
					tableIndexDao.createTemporaryTable(tableId);
					// copy all the data from the original to the temp.
					tableIndexDao.copyAllDataToTemporaryTable(tableId);
					return null;
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	@Override
	public void deleteTemporaryTableCopy(ProgressCallback<Void> callback) {
		// deleting a temp table can take a long time so auto-progress is used.
		 try {
			tableManagerSupport.callWithAutoProgress(callback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					// create the table.
					tableIndexDao.deleteTemporaryTable(tableId);
					return null;
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public Long populateViewFromEntityReplication(final ProgressCallback<Void> callback, final ViewType viewType,
			final Set<Long> allContainersInScope, final List<ColumnModel> currentSchema) {
		ValidateArgument.required(callback, "callback");
		// this can take a long time with no chance to make progress.
		 try {
			return tableManagerSupport.callWithAutoProgress(callback, new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					// create the table.
					return populateViewFromEntityReplicationWithProgress(viewType, allContainersInScope, currentSchema);
				}
			});
		} catch (Exception e) {
			if(e instanceof RuntimeException){
				throw ((RuntimeException)e);
			}else{
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
	 */
	Long populateViewFromEntityReplicationWithProgress(ViewType viewType, Set<Long> allContainersInScope, List<ColumnModel> currentSchema){
		ValidateArgument.required(viewType, "viewType");
		ValidateArgument.required(allContainersInScope, "allContainersInScope");
		ValidateArgument.required(currentSchema, "currentSchema");
		// Lookup the etag column
		ColumnModel etagColumn = EntityField.findMatch(currentSchema, EntityField.etag);
		if(etagColumn == null){
			throw new IllegalArgumentException("The ETAG column is missing from the schema");
		}
		// Lookup the benefactor column.
		ColumnModel benefactorColumn = EntityField.findMatch(currentSchema, EntityField.benefactorId);
		if(benefactorColumn == null){
			throw new IllegalArgumentException("The BENEFACTOR column is missing from the schema");
		}
		// copy the data from the entity replication tables to table's index
		tableIndexDao.copyEntityReplicationToTable(this.tableId, viewType, allContainersInScope, currentSchema);
		// calculate the new CRC32;
		return tableIndexDao.calculateCRC32ofTableView(tableId, etagColumn.getId());
	}
}
