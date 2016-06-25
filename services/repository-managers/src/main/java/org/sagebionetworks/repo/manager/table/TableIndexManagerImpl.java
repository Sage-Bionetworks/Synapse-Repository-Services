package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.ColumnChange;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class TableIndexManagerImpl implements TableIndexManager {
	
	public static final int MAX_MYSQL_INDEX_COUNT = 63; // mysql only supports a max of 64 secondary indices per table.
	
	private final TableIndexDAO tableIndexDao;
	private final String tableId;
	
	public TableIndexManagerImpl(TableIndexDAO dao, String tableId){
		if(dao == null){
			throw new IllegalArgumentException("TableIndexDAO cannot be null");
		}
		if(tableId == null){
			throw new IllegalArgumentException("TableId cannot be null");
		}
		this.tableIndexDao = dao;
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
	public void setIndexSchema(List<ColumnModel> currentSchema) {
		// Replace all columns
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		for(ColumnModel newColumn: currentSchema){
			ColumnModel oldColumn = null;
			changes.add(new ColumnChange(oldColumn, newColumn));
		}
		updateTableSchema(changes);
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
	public void updateTableSchema(List<ColumnChange> changes) {
		// create the table if it does not exist
		tableIndexDao.createTableIfDoesNotExist(tableId);
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(tableId);
		
		List<ColumnModel> newSchema = new LinkedList<ColumnModel>();
		for(ColumnChange change: changes){
			if(change.getNewColumn() != null){
				newSchema.add(change.getNewColumn());
			}
		}
		
		if(newSchema.isEmpty()){
			// clear all rows from the table
			tableIndexDao.truncateTable(tableId);
		}
		// Alter the table
		tableIndexDao.alterTableAsNeeded(tableId, changes);		
		// Save the hash of the new schema
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(newSchema);
		tableIndexDao.setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
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
	
}
