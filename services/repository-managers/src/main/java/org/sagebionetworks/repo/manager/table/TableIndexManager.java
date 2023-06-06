package org.sagebionetworks.repo.manager.table;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * The 'truth' of a Synapse table consists of metadata in the main repository
 * RDS and changes sets that consist of compressed CSV files stored in S3. Each
 * change set is tracked in the main repository database. In order to query a
 * table, an 'index' must first be built from a table's metadata and change
 * sets. The 'index' of a table will consist of one or more relational tables
 * that reside in one of many database instances in a cluster of database.
 * 
 * A load balancer is use to distribute the index of each table across the
 * cluster of relational database as needed. Each table will be assigned a
 * single instance in the cluster to house its index data. A connection factory
 * is used to establish a connection to the database instance assigned to a
 * table's index.
 * 
 * In order to work with a table's index a connection to the assigned relational
 * database instance must be created using the connection factory. An instance
 * of this class will wrap such a connection and provide support for all
 * operations on a single table's index. A new instance must be acquired from
 * the connection factory for each table. A table can be assigned to a different
 * relational instances so, connections to a table's index should not be
 * persisted in any way.
 * 
 * @author John
 * 
 */
public interface TableIndexManager {

	/**
	 * Get the current version of the index for this table. This is the version
	 * number of the last change set applied to index.
	 * 
	 * @return
	 */
	long getCurrentVersionOfIndex(IdAndVersion tableId);

	/**
	 * Has the change set represented by the given version number already been
	 * applied to the table index?
	 * 
	 * @param connection
	 * @param versionNumber
	 * @return True if the change set for the given version has already been applied
	 *         ot the table.
	 */
	boolean isVersionAppliedToIndex(IdAndVersion tableId, long versionNumber);

	/**
	 * Apply the given change set to a table's index. Each row in a change set must
	 * have the same version number. Each change set must be the complete set of
	 * rows for a given version. Change sets must be applied to the index in version
	 * number order, the first version number equal to zero and the last version
	 * number equals to n-1.
	 * 
	 * This method is idempotent. The latest version applied to the index is tracked
	 * and only newer versions will actually be applied to the index when called.
	 * 
	 * This method should only be used for TableEntities and not FileViews.
	 * 
	 * @param rowset                 Rowset to apply.
	 * @param currentSchema          The current schema of the table.
	 * @param changeSetVersionNumber The version number of the changeset. Note, this
	 *                               version number must match the version number of
	 *                               each row in the passed changeset.
	 */
	void applyChangeSetToIndex(IdAndVersion tableId, SparseChangeSet rowset, long changeSetVersionNumber);

	/**
	 * Set the current schema of a table's index.
	 * 
	 * @param currentSchema
	 */
	List<ColumnChangeDetails> setIndexSchema(IndexDescription indexDescription,
			List<ColumnModel> currentSchema);

	/**
	 * 
	 * @param currentSchema
	 */
	boolean updateTableSchema(IndexDescription indexDescription, List<ColumnChangeDetails> changes);

	/**
	 * Delete the index for this table.
	 */
	void deleteTableIndex(IdAndVersion tableId);

	/**
	 * Set the current version for the table index
	 * @param indexVersion
	 */
	void setIndexVersion(IdAndVersion tableId, Long indexVersion);
	
	/**
	 * Sets the status of the search flag for the given table
	 * 
	 * @param tableId
	 * @param searchEnabled
	 */
	void setSearchEnabled(IdAndVersion tableId, boolean searchEnabled);

	/**
	 * Optimize the indices of this table. Indices are added until either all
	 * columns have an index or the maximum number of indices per table is reached.
	 * When a table has more columns than the maximum number of indices, indices are
	 * assigned to columns with higher cardinality before columns with low
	 * cardinality.
	 * 
	 * Note: This method should be called after making all changes to a table.
	 */
	void optimizeTableIndices(IdAndVersion tableId);

	/**
	 * For _LIST type columns, create a separate table as an index for the multiple
	 * values in that table
	 * 
	 * @param tableIdAndVersion
	 * @param schemas
	 */
	void populateListColumnIndexTables(IdAndVersion tableIdAndVersion, List<ColumnModel> schemas);

	/**
	 * 
	 * @param tableIdAndVersion
	 * @param schema
	 * @param rowIds
	 */
	void populateListColumnIndexTables(IdAndVersion tableIdAndVersion, List<ColumnModel> schema, Set<Long> rowIds);

	/**
	 * Create a temporary copy of the table's index table.
	 *
	 */
	void createTemporaryTableCopy(IdAndVersion tableId);

	/**
	 * Delete the temporary copy of table's index.
	 */
	void deleteTemporaryTableCopy(IdAndVersion tableId);

	/**
	 * Attempt to alter the schema of a temporary copy of a table. This is used to
	 * validate table schema changes.
	 * 
	 * @param progressCallback
	 * @param tableId
	 * @param changes
	 * @return
	 */
	void alterTempTableSchema(IdAndVersion tableId, List<ColumnChangeDetails> changes);

	/**
	 * Populate a view table by coping all of the relevant data from the entity
	 * replication tables.
	 * 
	 * @param callback
	 * 
	 * @param scopeType
	 * @param currentSchema
	 * @return The new version of the view
	 */
	long populateViewFromEntityReplication(Long viewId, ViewScopeType scopeType, List<ColumnModel> currentSchema);
	
	/**
	 * Get the possible ColumnModel definitions based on annotation within a given
	 * scope.
	 * 
	 * @param scope         Defined as the list of container ids for a view.
	 * @param nextPageToken Optional: Controls pagination.
	 * @param excludeDerivedKeys True if the annotations keys derived from schemas bound to entities should be excluded from the computation
	 * @return A ColumnModel for each distinct annotation for the given scope.
	 */
	ColumnModelPage getPossibleColumnModelsForScope(ViewScope scope, String nextPageToken, boolean excludeDerivedKeys);

	/**
	 * Build the index for the given table using the provided change metadata up to
	 * and including the provided target change number.
	 * 
	 * @param progressCallback
	 * 
	 * @param tableId
	 * @param iterator
	 * @param targetChangeNumber
	 * @throws RecoverableMessageException Will RecoverableMessageException if the
	 *                                     index cannot be built at this time.
	 */
	void buildIndexToChangeNumber(ProgressCallback progressCallback, IdAndVersion idAndVersion,
			Iterator<TableChangeMetaData> iterator) throws RecoverableMessageException;

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
	 * @param viewId The id of the view to check.
	 * @param filter
	 * @param limit  Limit the number of rows returned.
	 * @return
	 */
	Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, ViewFilter filter, long limit);

	/**
	 * In a single transaction, update the provided rowIds for a view. For each
	 * rowId, all data will first be deleted from the view, then copied back to the
	 * view from the replication tables.
	 * 
	 * @param index             The index description of the view
	 * @param rowsIdsWithChanges The Ids of the rows to be updated in this
	 *                           transaction.
	 * @param viewTypeMask       The type of view this is.
	 * @param currentSchema      The current schema of the view.
	 * @param filter
	 * @param provider
	 * @return The new version of the view
	 */
	long updateViewRowsInTransaction(IndexDescription index, ViewScopeType scopeType, List<ColumnModel> currentSchema,
			ViewFilter filter);

	/**
	 * Ensure the benefactor IDs for the given view snapshot are up-to-date.
	 * 
	 * @param viewId
	 * @return An optional containing the next version of the view if any benefactor changed, empty otherwise
	 */
	Optional<Long> refreshViewBenefactors(IdAndVersion viewId);

	/**
	 * Update the object replication for the given object data.
	 * 
	 * @param objectType
	 * @param allIds     The IDs of all rows that will be updated.
	 * @param objectData
	 */
	void updateObjectReplication(ReplicationType objectType, Iterator<ObjectDataDTO> objectData);

	/**
	 * Delete the object replication data for the given objectIds.
	 * 
	 * @param objectType
	 * @param toDeleteIds
	 */
	void deleteObjectData(ReplicationType objectType, List<Long> toDeleteIds);

	/**
	 * Stream over the IdAndChecksum for all objects defined by the provided filter.
	 * The checksum must include all version of the objects that match the
	 * condition. See the following pusdo-sql:
	 * </p>
	 * <code>SELECT ID, SUM(CRC32(CONCAT(salt','-',ETAG,'-',VERSION,'-',BENEFACTOR_ID))) AS CHECK_SUM ... GROUP BY ID ORDER BY ID ASC</code>
	 * 
	 * @param salt
	 * @param filter
	 * @return
	 */
	Iterator<IdAndChecksum> streamOverIdsAndChecksums(Long salt, ViewFilter filter);

	/**
	 * Is the synchronization lock for the given view expires?
	 * 
	 * @param idAndVersion
	 * @return
	 */
	boolean isViewSynchronizeLockExpired(ReplicationType type, IdAndVersion idAndVersion);

	/**
	 * Reset the synchronized lock for the given view.
	 * @param idAndVersion
	 */
	void resetViewSynchronizeLock(ReplicationType type, IdAndVersion idAndVersion);

	/**
	 * Populate the index of a materialized view.
	 * @param viewSchema
	 * @param definingSql
	 * @return
	 */
	Long populateMaterializedViewFromDefiningSql(List<ColumnModel> viewSchema, QueryTranslator definingSql);

	/**
	 * Reset the state of the table index described by the given {@link IndexDescription}
	 * 
	 * @param index
	 * @return The schema of the table
	 */
	List<ColumnModel> resetTableIndex(IndexDescription index);
	
	/**
	 * Reset the state of the table index described by the given {@link IndexDescription} using the given schema and search status
	 * 
	 * @param index
	 * @return The schema of the table
	 */
	List<ColumnModel> resetTableIndex(IndexDescription index, List<ColumnModel> schema, boolean isSearchEnabled);
	
	/**
	 * Build all the secondary table indices for the table
	 * 
	 * @param index
	 */
	void buildTableIndexIndices(IndexDescription index, List<ColumnModel> schema);
	
	/**
	 * Moves the content of the source index into the target index.
	 * 
	 * @param target
	 * @param replacement
	 */
	void swapTableIndex(IndexDescription source, IndexDescription target);

	/**
	 * @param index
	 * @return A version representing the sum of the current versions of all the dependencies for the given index
	 */
	long getVersionFromIndexDependencies(IndexDescription index);
	
}
