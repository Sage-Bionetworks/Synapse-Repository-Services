package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class TableIndexManagerImpl implements TableIndexManager {
	
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
									.getFileHandleIdsInRowSet(currentSchema,
											rowset.getRows());
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

	}

	@Override
	public void deleteTableIndex() {
		// delete all tables for this index.
		tableIndexDao.deleteTable(tableId);
		tableIndexDao.deleteSecondayTables(tableId);
	}	

}
