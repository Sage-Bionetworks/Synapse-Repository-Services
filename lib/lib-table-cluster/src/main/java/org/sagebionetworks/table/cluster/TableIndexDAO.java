package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;

/**
 * This is an abstraction for table index CRUD operations.
 * @author John
 *
 */
public interface TableIndexDAO {

	public static class ColumnDefinition {
		public String name;
		public ColumnType columnType;
		public Long maxSize;
	}

	/**
	 * Create or update a table with the given schema.
	 * 
	 * @param connection
	 * @param schema
	 * @param tableId
	 */
	public boolean createOrUpdateTable(List<ColumnModel> schema, String tableId);
	
	/**
	 * 
	 * @param connection
	 * @param tableId
	 * @return
	 */
	public boolean deleteTable(String tableId); 
	
	/**
	 * Get the current columns of a table in the passed database connection.
	 * @param tableId
	 * @return
	 */
	public List<ColumnDefinition> getCurrentTableColumns(String tableId);
	
	/**
	 * Create or update the rows passed in the given RowSet.
	 * 
	 * Note: The passed RowSet is not required to match the current schema.
	 * Columns in the Rowset that are not part of the current schema will be ignored.
	 * Columns in the current schema that are not part of the RowSet will be set to
	 * the default value of the column.
	 * @param connection
	 * @param rowset
	 * @return
	 */
	void createOrUpdateOrDeleteRows(RowSet rowset, List<ColumnModel> currentSchema);
	
	/**
	 * Query a RowSet from the table.
	 * @param query
	 * @return
	 */
	public RowSet query(ProgressCallback<Void> callback, SqlQuery query);
	
	/**
	 * Provides the means to stream over query results without keeping the row data in memory.
	 * 
	 * @param query
	 * @param handler
	 * @return
	 */
	public boolean queryAsStream(ProgressCallback<Void> callback, SqlQuery query, RowAndHeaderHandler handler);
	
	/**
	 * Get the row count for this table.
	 * 
	 * @param tableId
	 * @return The row count of the table. If the table does not exist then null.
	 */
	public Long getRowCountForTable(String tableId);
	
	/**
	 * Get the max complete version we currently have for this table.
	 * 
	 * @param tableId
	 * @param version the max complete version to remember
	 * @return The max complete version of the table. If the table does not exist then -1L.
	 */
	public Long getMaxCurrentCompleteVersionForTable(String tableId);

	/**
	 * Set the max complete version for this table
	 * 
	 * @param tableId
	 * @param highestVersion
	 */
	public void setMaxCurrentCompleteVersionForTable(String tableId, Long highestVersion);
	
	/**
	 * Set the MD5 hex of the table's current schema.
	 * 
	 * @param tableId
	 * @param schemaMD5Hex
	 */
	public void setCurrentSchemaMD5Hex(String tableId, String schemaMD5Hex);
	
	/**
	 * Get the MD5 hex of the table's current schema.
	 * @param tableId
	 * @return
	 */
	public String getCurrentSchemaMD5Hex(String tableId);

	/**
	 * Delete all of the secondary tables used for an index if they exist.
	 * 
	 * @param tableId
	 */
	public void deleteSecondayTables(String tableId);
	
	/**
	 * Create all of the secondary tables used for an index if they do not exist.
	 * @param tableId
	 */
	public void createSecondaryTables(String tableId);
	
	/**
	 * Get the connection
	 * @return
	 */
	public JdbcTemplate getConnection();

	/**
	 * run calls within a read transaction
	 * 
	 * @param callable
	 * @return
	 */
	public <T> T executeInReadTransaction(TransactionCallback<T> callable);
	
	/**
	 * Run the passed callable within a write transaction.
	 * @param callable
	 * @return
	 */
	public <T> T executeInWriteTransaction(TransactionCallback<T> callable);

	/**
	 * Apply the passed set of file handle Ids to the given table index.
	 * 
	 * @param tableId
	 * @param fileHandleIds
	 */
	public void applyFileHandleIdsToTable(String tableId,
			Set<Long> fileHandleIds);
	
	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of
	 * FileHandleIds that are actually associated with the table.
	 * @param toTest
	 * @param objectId
	 * @return
	 */
	public Set<Long> getFileHandleIdsAssociatedWithTable(
			Set<Long> toTest, String tableId);
	
	/**
	 * Does the state of the index match the given data?
	 * 
	 * @param tableId
	 * @param versionNumber
	 * @param schemaMD5Hex
	 * @return
	 */
	public boolean doesIndexStateMatch(String tableId, long versionNumber, String schemaMD5Hex);
}
