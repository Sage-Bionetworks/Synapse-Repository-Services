package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.ColumnChange;
import org.sagebionetworks.table.cluster.ColumnDefinition;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
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
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(tableId);
		
		if (currentSchema.isEmpty()) {
			// If there is no schema delete the table
			tableIndexDao.deleteTable(tableId);
		} else {
			// We have a schema so create or update the table
			tableIndexDao.createOrUpdateTable(currentSchema, tableId);
		}
		// Save the hash of the new schema
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(currentSchema);
		tableIndexDao.setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
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
	public void createTableIndexIfDoesNotExist() {
		// Create all of the status tables unconditionally.
		tableIndexDao.createSecondaryTables(tableId);
		// create the table if it does not exist
		tableIndexDao.createTableIfDoesNotExist(tableId);
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
		
		// Add any missing indices.
		List<ColumnDefinition> currentColumns = tableIndexDao.getCurrentTableColumns(tableId);
		List<ColumnDefinition> indicesToAdd = getColumnsThatNeedAnIndex(currentColumns, MAX_MYSQL_INDEX_COUNT);
		tableIndexDao.addIndicesToTable(tableId, indicesToAdd);
		
		// Save the hash of the new schema
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(newSchema);
		tableIndexDao.setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	/**
	 * Given the current schema of a table determine which columns need an index,
	 * while remaining under the maximum number off allowed indices.
	 * 
	 * @param schema
	 * @param The maximum number of indices allowed on a single table.
	 * @return
	 */
	public static List<ColumnDefinition> getColumnsThatNeedAnIndex(List<ColumnDefinition> schema, int maxNumberOfIndices){
		ValidateArgument.required(schema, "schema");
		List<ColumnDefinition> results = new LinkedList<ColumnDefinition>();
		int totalIndexCount = 0;
		for(ColumnDefinition columnDef: schema){
			if(columnDef.hasIndex()){
				totalIndexCount++;
			}
		}
		if(totalIndexCount >= maxNumberOfIndices){
			// cannot add any more indices.
			return results;
		}
		// 
		int maxNumberToAdd = maxNumberOfIndices-totalIndexCount;
		for(ColumnDefinition columnDef: schema){
			// skip rowId and version
			if(ROW_ID.equals(columnDef.getName().toUpperCase())){
				continue;
			}
			if(ROW_VERSION.equals(columnDef.getName().toUpperCase())){
				continue;
			}
			if(!columnDef.hasIndex()){
				results.add(columnDef);
			}
			if(results.size() == maxNumberToAdd){
				break;
			}
		}
		return results;
	}
	
}
