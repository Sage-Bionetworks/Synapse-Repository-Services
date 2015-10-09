package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class TableIndexManagerImpl implements TableIndexManager {

	@Autowired
	ConnectionFactory tableConnectionFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableIndexManager#
	 * getCurrentVersionOfIndex
	 * (org.sagebionetworks.repo.manager.table.TableIndexManager
	 * .TableIndexConnection)
	 */
	@Override
	public long getCurrentVersionOfIndex(final TableIndexConnection wrapper) {
		final TableIndexConnectionImpl con = convertConnection(wrapper);
		return con.getTableIndexDAO().getMaxCurrentCompleteVersionForTable(con.getTableId());
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
	public void applyChangeSetToIndex(final TableIndexConnection wrapper,
			final RowSet rowset, final List<ColumnModel> currentSchema,
			final long changeSetVersionNumber) {
		final TableIndexConnectionImpl con = convertConnection(wrapper);
		final String tableId = con.getTableId();
		final TableIndexDAO tableIndexDAO = con.getTableIndexDAO();
		// Validate all rows have the same version number
		TableModelUtils.validateRowVersions(rowset.getRows(), changeSetVersionNumber);
		// Has this version already been applied to the table index?
		final long currentVersion = tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId);
		if(changeSetVersionNumber > currentVersion){
			// apply all changes in a transaction
			tableIndexDAO.executeInWriteTransaction(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					// apply the change to the index
					tableIndexDAO.createOrUpdateOrDeleteRows(rowset, currentSchema);
					// Extract all file handle IDs from this set
					Set<String> fileHandleIds = TableModelUtils.getFileHandleIdsInRowSet(currentSchema, rowset.getRows());
					if(!fileHandleIds.isEmpty()){
						tableIndexDAO.applyFileHandleIdsToTable(tableId, fileHandleIds);
					}
					// set the new max version for the index
					tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, changeSetVersionNumber);
					return null;
				}
			});
		}
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
	public void setIndexSchema(final TableIndexConnection wrapper,
			List<ColumnModel> currentSchema) {
		final TableIndexConnectionImpl con = convertConnection(wrapper);
		final String tableId = con.getTableId();
		final TableIndexDAO tableIndexDAO = con.getTableIndexDAO();
		if (currentSchema.isEmpty()) {
			// If there is no schema delete the table
			tableIndexDAO.deleteTable(tableId);
		} else {
			// We have a schema so create or update the table
			tableIndexDAO.createOrUpdateTable(currentSchema, tableId);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableIndexManager#connectToTableIndex
	 * (java.lang.String)
	 */
	@Override
	public TableIndexConnection connectToTableIndex(final String tableId) {
		final TableIndexDAO tableIndexDAO = tableConnectionFactory
				.getConnection(tableId);
		if (tableIndexDAO == null) {
			throw new TableIndexConnectionUnavailableException(
					"Connection cannot be made at this time.");
		}
		return new TableIndexConnectionImpl(tableId, tableIndexDAO);
	}

	/**
	 * Get the actual connection.
	 * 
	 * @param connection
	 * @return
	 */
	private TableIndexConnectionImpl convertConnection(
			TableIndexConnection connection) {
		if (connection == null) {
			throw new IllegalArgumentException("Connection cannot be null");
		}
		if (!(connection instanceof TableIndexConnectionImpl)) {
			throw new IllegalArgumentException("Unknown connection type: "
					+ connection.getClass().getName());
		}
		return (TableIndexConnectionImpl) connection;
	}

	/**
	 * Hides the actual connection from the caller.
	 * 
	 */
	private static class TableIndexConnectionImpl implements
			TableIndexConnection {
		final String tableId;
		final TableIndexDAO tableIndexDAO;

		public TableIndexConnectionImpl(String tableId,
				TableIndexDAO tableIndexDAO) {
			super();
			if (tableId == null) {
				throw new IllegalArgumentException("Table Id cannot be null");
			}
			if (tableIndexDAO == null) {
				throw new IllegalArgumentException(
						"TableIndexDAO cannot be null");
			}
			this.tableId = tableId;
			this.tableIndexDAO = tableIndexDAO;
		}

		private String getTableId() {
			return tableId;
		}

		private TableIndexDAO getTableIndexDAO() {
			return tableIndexDAO;
		}

		@Override
		public int hashCode() {
			throw new UnsupportedOperationException(
					"Table index connections cannot be cached");
		}

		@Override
		public boolean equals(Object obj) {
			throw new UnsupportedOperationException(
					"Table index connections cannot be cached");
		}
	}

}
