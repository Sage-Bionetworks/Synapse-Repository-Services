package org.sagebionetworks.table.cluster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.search.RowSearchContent;
import org.sagebionetworks.table.cluster.search.TableRowData;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.util.Callback;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;

/**
 * This is an abstraction for table index CRUD operations.
 * 
 * @author John
 *
 */
public interface TableIndexDAO {

	/**
	 * Create a table with the given name if it does not exist.
	 * 
	 * @param indexDescription defines the index
	 */
	void createTableIfDoesNotExist(IndexDescription indexDescription);

	/**
	 * Alter the given table as needed. The table will be changed according to the
	 * passed list of column changes. This includes, additions, deletions, and
	 * updates.
	 * 
	 * @param tableId
	 * @param changes
	 * @param alterTemp When true the temporary table will be altered. When false
	 *                  the original table will be altered.
	 * @return True if the table was altered. False if the table was not changed.
	 */
	boolean alterTableAsNeeded(IdAndVersion tableId, List<ColumnChangeDetails> changes, boolean alterTemp);

	/**
	 * 
	 * @param connection
	 * @param tableId
	 * @return
	 */
	void deleteTable(IdAndVersion tableId);

	/**
	 * Create or update the rows passed in the given RowSet.
	 * 
	 * @param grouping group of rows that change the same columns.
	 * @return
	 */
	void createOrUpdateOrDeleteRows(IdAndVersion tableId, Grouping grouping);

	/**
	 * Query a RowSet from the table.
	 * 
	 * @param query
	 * @return
	 */
	RowSet query(ProgressCallback callback, QueryTranslator query);

	/**
	 * Run a simple count query.
	 * 
	 * @param sql
	 * @param parameters
	 * @return
	 */
	Long countQuery(String sql, Map<String, Object> parameters);

	/**
	 * Provides the means to stream over query results without keeping the row data
	 * in memory.
	 * 
	 * @param query
	 * @param handler
	 * @return
	 */
	boolean queryAsStream(ProgressCallback callback, QueryTranslator query, RowHandler handler);

	/**
	 * Get the row count for this table.
	 * 
	 * @param tableId
	 * @return The row count of the table. If the table does not exist then null.
	 */
	Long getRowCountForTable(IdAndVersion tableId);

	/**
	 * Get the max complete version we currently have for this table.
	 * 
	 * @param tableId
	 * @param version the max complete version to remember
	 * @return The max complete version of the table. If the table does not exist
	 *         then -1L.
	 */
	Long getMaxCurrentCompleteVersionForTable(IdAndVersion tableId);
	
	/**
	 * 
	 * @param tableId
	 * @return The current status of the search enabled flag in the building process of the table
	 */
	boolean isSearchEnabled(IdAndVersion tableId);

	/**
	 * Set the max complete version for this table
	 * 
	 * @param tableId
	 * @param highestVersion
	 */
	void setMaxCurrentCompleteVersionForTable(IdAndVersion tableId, Long highestVersion);
	
	/**
	 * Set the MD5 hex of the table's current schema.
	 * 
	 * @param tableId
	 * @param schemaMD5Hex
	 */
	void setCurrentSchemaMD5Hex(IdAndVersion tableId, String schemaMD5Hex);

	/**
	 * Get the MD5 hex of the table's current schema.
	 * 
	 * @param tableId
	 * @return Optional.empty() if there is no stored state for this table.
	 */
	Optional<String> getCurrentSchemaMD5Hex(IdAndVersion tableId);
	
	void setSearchEnabled(IdAndVersion tableId, boolean searchStatus);
	
	/**
	 * Does the current index schema MD5 hash match the MD5 has of the provided
	 * schema? This is a fast and efficient check to determine if additional work is
	 * needed in order to make the index match the provided schema.
	 * 
	 * @param tableId
	 * @param schema
	 * @return True if the index hash matches the hash of the provided schema.
	 */
	boolean doesIndexHashMatchSchemaHash(IdAndVersion tableId, List<ColumnModel> schema);

	/**
	 * Create all of the secondary tables used for an index if they do not exist.
	 * 
	 * @param tableId
	 */
	void createSecondaryTables(IdAndVersion tableId);

	/**
	 * Get the connection
	 * 
	 * @return
	 */
	JdbcTemplate getConnection();

	/**
	 * run calls within a read transaction
	 * 
	 * @param callable
	 * @return
	 */
	<T> T executeInReadTransaction(TransactionCallback<T> callable);

	/**
	 * Run the passed callable within a write transaction.
	 * 
	 * @param callable
	 * @return
	 */
	<T> T executeInWriteTransaction(TransactionCallback<T> callable);

	/**
	 * Apply the passed set of file handle Ids to the given table index.
	 * 
	 * @param tableId
	 * @param fileHandleIds
	 */
	void applyFileHandleIdsToTable(IdAndVersion tableId, Set<Long> fileHandleIds);

	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of FileHandleIds
	 * that are actually associated with the table.
	 * 
	 * @param toTest
	 * @param objectId
	 * @return
	 */
	Set<Long> getFileHandleIdsAssociatedWithTable(Set<Long> toTest, IdAndVersion tableId);

	/**
	 * Does the state of the index match the given data?
	 * 
	 * @param tableId
	 * @param versionNumber
	 * @param schemaMD5Hex
	 * @return
	 */
	boolean doesIndexStateMatch(IdAndVersion tableId, long versionNumber, String schemaMD5Hex, boolean searchEnabled);

	/**
	 * Get the distinct Long values for a given column ID.
	 * 
	 * @param id
	 * @return
	 */
	Set<Long> getDistinctLongValues(IdAndVersion tableId, String columnIds);

	/**
	 * Get list of Column ids for existing index tables a multi-value column in the
	 * provided tableId 
	 * <p>Note: This call is expensive and should only be executed if
	 * it has already been determined that an index change is needed, by calling
	 * {@link #doesIndexHashMatchSchemaHash(IdAndVersion, List)}.
	 * See: PLFM-7458
	 * </p>
	 * 
	 * @param tableId
	 * @return
	 */
	Set<Long> getMultivalueColumnIndexTableColumnIds(IdAndVersion tableId);

	/**
	 * Creates an index table for the multi-value column described in the
	 * columnModel.
	 * 
	 * @param tableId
	 * @param columnModel
	 * @param alterTemp
	 */
	void createMultivalueColumnIndexTable(IdAndVersion tableId, ColumnModel columnModel, boolean alterTemp);

	/**
	 * Drop the multi-value column index table associated with the table id and
	 * column id
	 * 
	 * @param tableId
	 * @param columnId
	 * @param alterTemp
	 */
	void deleteMultivalueColumnIndexTable(IdAndVersion tableId, Long columnId, boolean alterTemp);

	/**
	 * Drop the multi-value column index table associated with the table id and
	 * column id
	 * 
	 * @param columnId
	 * @param tableId
	 * @param alterTemp
	 */
	void updateMultivalueColumnIndexTable(IdAndVersion tableId, Long oldColumnId, ColumnModel newColumn,
			boolean alterTemp);

	/**
	 * Truncate all of the data in the given table.
	 * 
	 * @param tableId
	 */
	void truncateTable(IdAndVersion tableId);

	/**
	 * Get information about each column of a database table.
	 * 
	 * @param tableId
	 * @return
	 */
	List<DatabaseColumnInfo> getDatabaseInfo(IdAndVersion tableId);
	
	/**
	 * Provide the cardinality for the given columns and table.
	 * 
	 * Note: A single query will be executed, and the results added to the passed
	 * info list.
	 * 
	 * @param list
	 * @param tableId
	 */
	void provideCardinality(List<DatabaseColumnInfo> list, IdAndVersion tableId);

	/**
	 * Provide the index name for each column in the table.
	 * 
	 * @param list
	 * @param tableId
	 */
	void provideIndexName(List<DatabaseColumnInfo> list, IdAndVersion tableId);

	/**
	 * The provided column data is used to optimize the indices on the given table.
	 * Indices are added until either all columns have an index or the maximum
	 * number of indices per table is reached. When a table has more columns than
	 * the maximum number of indices, indices are assigned to columns with higher
	 * cardinality before columns with low cardinality.
	 * 
	 * @param list             The current column information of this table used for
	 *                         the optimization.
	 * @param tableId          The table to optimize.
	 * @param maxNumberOfIndex The maximum number of indices allowed on a single
	 *                         table.
	 */
	void optimizeTableIndices(List<DatabaseColumnInfo> list, IdAndVersion tableId, int maxNumberOfIndex);

	/**
	 * Populate the separate index table for the given list column.
	 * 
	 * @param tableId
	 * @param listColumn
	 * @param rowIds     Optional. When included, only rows with the given IDs will
	 *                   be populated.
	 * @param alterTemp
	 */
	void populateListColumnIndexTable(IdAndVersion tableId, ColumnModel listColumn, Set<Long> rowIds,
			boolean alterTemp);

	/**
	 * Delete rows from an a specific list column's index table.
	 * 
	 * @param tableId
	 * @param listColumn
	 * @param rowIds
	 */
	void deleteFromListColumnIndexTable(IdAndVersion tableId, ColumnModel listColumn, Set<Long> rowIds);

	/**
	 * Create a temporary table like the given table.
	 * 
	 * @param tableId
	 */
	void createTemporaryTable(IdAndVersion tableId);

	/**
	 * Copy all of the data from the original table to the temporary table.
	 * 
	 * @param tableId
	 */
	void copyAllDataToTemporaryTable(IdAndVersion tableId);

	/**
	 * Delete the temporary table associated with the given table.
	 */
	void deleteTemporaryTable(IdAndVersion tableId);

	/**
	 * Count the rows in the temp table.
	 * 
	 * @param tableId
	 * @return
	 */
	long getTempTableCount(IdAndVersion tableId);

	/**
	 * Create a temporary multivalue column index table like the given table.
	 * 
	 * @param tableId
	 */
	void createTemporaryMultiValueColumnIndexTable(IdAndVersion tableId, String columnId);

	/**
	 * Copy all of the data from the original multivalue column index table to the
	 * temporary table.
	 * 
	 * @param tableId
	 */
	void copyAllDataToTemporaryMultiValueColumnIndexTable(IdAndVersion tableId, String columnId);

	/**
	 * Delete all of the temporary multivalue column index table associated with the
	 * given table.
	 */
	void deleteAllTemporaryMultiValueColumnIndexTable(IdAndVersion tableId);

	/**
	 * Count the rows in the temp multi value index table.
	 * 
	 * @param tableId
	 * @return
	 */
	long getTempTableMultiValueColumnIndexCount(IdAndVersion tableId, String columnName);

	/**
	 * Create the entity replication tables if they do not exist.
	 * 
	 */
	void createObjectReplicationTablesIfDoesNotExist();

	/**
	 * Delete all object data with the given Ids.
	 * 
	 * @param objectsType      TODO
	 * @param objectIds
	 * @param progressCallback
	 */
	void deleteObjectData(ReplicationType objectsType, List<Long> objectIds);

	/**
	 * Add the given object data to the index.
	 * 
	 * @param mainType   TODO
	 * @param objectDtos
	 */
	void addObjectData(ReplicationType mainType, List<ObjectDataDTO> objectDtos);

	/**
	 * Queries for max length of list values in a column in the temporary copy of
	 * the table (created using {@link #createTemporaryTable(IdAndVersion)})
	 * 
	 * @param tableId
	 * @param columnId
	 * @return max list value length of the column
	 */
	long tempTableListColumnMaxLength(IdAndVersion tableId, String columnId);
	
	/**
	 * 
	 * @param tableId
	 * @param columnId
	 * @param characterLimit
	 * @return The first row id if the any value in the column with the given id in the temporary copy of the given table exceeds the given character limit
	 */
	Optional<Long> tempTableColumnExceedsCharacterLimit(IdAndVersion tableId, String columnId, long characterLimit);

	/**
	 * Copy the data from the entity replication tables to the given view.
	 * 
	 * @param viewId
	 * @param scopeFilter
	 * @param currentSchema
	 */
	void copyObjectReplicationToView(Long viewId, ViewFilter filter, List<ColumnModel> currentSchema,
			ObjectFieldTypeMapper fieldTypeMapper);

	/**
	 * Get the distinct possible ColumnModels for the given scope filter
	 * 
	 * @param scopeFilter
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<ColumnModel> getPossibleColumnModelsForContainers(ViewFilter filter, Long limit, Long offset);

	/**
	 * Is the synchronization lock expired for the given object
	 * 
	 * @param mainType
	 * @param objectId
	 * @return
	 */
	boolean isSynchronizationLockExpiredForObject(ReplicationType mainType, Long objectId);

	/**
	 * Set the synchronization lock to be expired for the given object.
	 * @param mainType
	 * @param objectId
	 * @param newExpirationDateMS
	 */
	void setSynchronizationLockExpiredForObject(ReplicationType mainType, Long objectId, Long newExpirationDateMS);

	/**
	 * For each parent, get the sum of CRCs of their children.
	 * 
	 * @return Map.key = parentId and map.value = sum of children CRCs.
	 */
	Map<Long, Long> getSumOfChildCRCsForEachParent(ReplicationType mainType, List<Long> parentIds);

	/**
	 * Get the Id and Etag for each child of the given parentId.
	 * 
	 * @param mainType           TODO
	 * @param outOfSynchParentId
	 * @return
	 */
	List<IdAndEtag> getObjectChildren(ReplicationType mainType, Long parentId);

	/**
	 * Get the rowId and versions for the given query.
	 * 
	 * @param sqlSelectIdAndVersions
	 * @param parameters
	 * @return
	 */
	List<IdAndVersion> getRowIdAndVersions(String sqlSelectIdAndVersions, Map<String, Object> parameters);

	/**
	 * Get the sum of the files sizes for the given row ID and versions.
	 * 
	 * @param rowIdAndVersions
	 * @return
	 */
	long getSumOfFileSizes(ReplicationType mainType, List<IdAndVersion> rowIdAndVersions);

	/**
	 * Get the statistics about Synapse storage usage per-project.
	 * 
	 * @return
	 */
	void streamSynapseStorageStats(ReplicationType mainType, Callback<SynapseStorageProjectStats> callback);

	/**
	 * Initialize this dao by setting its database connection.
	 * 
	 * @param dataSource
	 */
	void setDataSource(DataSource dataSource);

	/**
	 * Get a single page (up to the provided limit) of rowIds that are out-of-date
	 * for the given view. A row is out-of-date if any of these conditions are true:
	 * <ul>
	 * <li>A row exists in the replication table but does not exist in the
	 * view.</li>
	 * <li>A row exists in the view but does not exist in the replication
	 * table.</li>
	 * <li>The rowId, etag, or benefactorId do not match in the view and the
	 * replication table.</li>
	 * </ul>
	 * 
	 * @param viewId      The id of the view to check.
	 * @param scopeFilter the filter to be applied to the view scope
	 * @param limit       Limit the number of rows returned.
	 * @return
	 */
	Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, ViewFilter filter, long limit);

	/**
	 * Delete a batch of rows from a view.
	 * 
	 * @param viewId
	 * @param idsToDelete
	 */
	void deleteRowsFromViewBatch(IdAndVersion viewId, Long... idsToDelete);

	/**
	 * @return the entity DTO for a given entity ID
	 */
	ObjectDataDTO getObjectData(ReplicationType mainType, Long objectId, Long objectVersion);

	/**
	 * Get the ObjectDataDTO for the current version of an object.
	 * 
	 * @param mainType
	 * @param objectId
	 * @return
	 */
	ObjectDataDTO getObjectDataForCurrentVersion(ReplicationType mainType, Long objectId);

	/**
	 * Ensure the benefactor ID within the given view snapshot are up-to-date with
	 * object replication.
	 * 
	 * @param viewId
	 */
	void refreshViewBenefactors(IdAndVersion viewId, ReplicationType mainType);

	/**
	 * Get a single page of IdAndChecksums from the replication table using the
	 * provided filter.
	 * 
	 * @param salt
	 * @param filter
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<IdAndChecksum> getIdAndChecksumsForFilter(Long salt, ViewFilter filter, Long limit, Long offset);
		
	/**
	 * @param idAndVersion The id of the table
	 * @param selectColumns The columns to fetch
	 * @param rowIds The id of the rows to restrict the data to
	 * @return A batch over the table row content matching the given set of columns for the rows with the given ids 
	 */
	List<TableRowData> getTableDataForRowIds(IdAndVersion idAndVersion, List<ColumnModel> selectColumns, Set<Long> rowIds);
	
	/**
	 * 
	 * @param idAndVersion The id of the table
	 * @param selectColumns The columns to fetch
	 * @param limit
	 * @param offset
	 * @return A page of table row content matching the given set of columns paginated according to the given limit and offset (ordered by row id)
	 */
	List<TableRowData> getTableDataPage(IdAndVersion idAndVersion, List<ColumnModel> selectColumns, long limit, long offset);

	/**
	 * Stream the data of the table with the given id to the given CSV stream writer
	 * 
	 * @param tableId
	 * @param stream
	 * @return The column model ids from the table index
	 */
	List<String> streamTableIndexData(IdAndVersion tableId, CSVWriterStream stream);
	
	/**
	 * Restore the table index using the data from the given iterator
	 * 
	 * @param idAndVersion
	 * @param input
	 * @param maxBytesPerBatch Used to limit the size of each batch of data pushed
	 *                         to the database. Only a single batch will reside in
	 *                         memory at a time. A batch will always contain at
	 *                         least one row even if the size of the row is larger
	 *                         than maxBytesPerBatch.
	 */
	void restoreTableIndexData(IdAndVersion idAndVersion, Iterator<String[]> input, long maxBytesPerBatch);
	
	/**
	 * Updates the search index content for the given batch of rows
	 * @param idAndVersion The id of the table
	 * @param searchContentRows The batch of rows to update
	 */
	void updateSearchIndex(IdAndVersion idAndVersion, List<RowSearchContent> searchContentRows);
	
	/**
	 * Clear all the content of the search index for the table with the given id
	 * @param idAndVersion The id of the table
	 */
	void clearSearchIndex(IdAndVersion idAndVersion);

	// For testing:

	/**
	 * Clear all expirations.
	 */
	void truncateReplicationSyncExpiration();

	/**
	 * Cleanup all the index tables
	 */
	void truncateIndex();
	
	/**
	 * fetch the current search content for the given set of rows
	 */
	List<RowSearchContent> fetchSearchContent(IdAndVersion idAndVersion, Set<Long> rowIds);

	/**
	 * Execute an update statement with the given parameters;
	 * @param string
	 * @param parameters
	 */
	void update(String string, Map<String, Object> parameters);

	/**
	 * Swap the two table indices
	 * 
	 * @param sourceIndexId
	 * @param targetIndexId
	 */
	void swapTableIndex(IdAndVersion sourceIndexId, IdAndVersion targetIndexId);	
	
}
