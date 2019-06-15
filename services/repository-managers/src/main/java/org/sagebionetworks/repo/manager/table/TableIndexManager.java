package org.sagebionetworks.repo.manager.table;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
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
	 * Get the current version of the index for this table. This is the
	 * version number of the last change set applied to index.
	 * 
	 * @return
	 */
	public long getCurrentVersionOfIndex(IdAndVersion tableId);
	
	/**
	 * The MD5 Hex string of the current schema.
	 * @return
	 */
	public String getCurrentSchemaMD5Hex(IdAndVersion tableId);

	/**
	 * Has the change set represented by the given version number already been
	 * applied to the table index?
	 * 
	 * @param connection
	 * @param versionNumber
	 * @return True if the change set for the given version has already been
	 *         applied ot the table.
	 */
	public boolean isVersionAppliedToIndex(IdAndVersion tableId, long versionNumber);

	/**
	 * Apply the given change set to a table's index. Each row in a change set
	 * must have the same version number. Each change set must be the complete
	 * set of rows for a given version. Change sets must be applied to the index
	 * in version number order, the first version number equal to zero and the
	 * last version number equals to n-1.
	 * 
	 * This method is idempotent. The latest version applied to the index is
	 * tracked and only newer versions will actually be applied to the index
	 * when called.
	 * 
	 * This method should only be used for TableEntities and not FileViews.
	 * 
	 * @param rowset
	 *            Rowset to apply.
	 * @param currentSchema
	 *            The current schema of the table.
	 * @param changeSetVersionNumber
	 *            The version number of the changeset. Note, this version number
	 *            must match the version number of each row in the passed
	 *            changeset.
	 */
	public void applyChangeSetToIndex(IdAndVersion tableId, SparseChangeSet rowset,
			long changeSetVersionNumber);

	/**
	 * Set the current schema of a table's index.
	 * 
	 * @param currentSchema
	 */
	public void setIndexSchema(IdAndVersion tableId, boolean isTableView, List<ColumnModel> currentSchema);
	
	/**
	 * 
	 * @param currentSchema
	 */
	public boolean updateTableSchema(IdAndVersion tableId, boolean isTableView, List<ColumnChangeDetails> changes);
	
	/**
	 * Delete the index for this table.
	 */
	public void deleteTableIndex(IdAndVersion tableId);

	/**
	 * Set current version of the index.
	 * @param viewCRC
	 */
	public void setIndexVersion(IdAndVersion tableId, Long viewCRC);
	
	/**
	 * Set the current version of the index and the schema MD5, both of which are used
	 * to determine if the index is up-to-date.
	 * 
	 * @param viewCRC
	 * @param schemaMD5Hex
	 */
	public void setIndexVersionAndSchemaMD5Hex(IdAndVersion tableId, Long viewCRC, String schemaMD5Hex);
	
	/**
	 * Optimize the indices of this table. Indices are added until either all
	 * columns have an index or the maximum number of indices per table is
	 * reached. When a table has more columns than the maximum number of
	 * indices, indices are assigned to columns with higher cardinality before
	 * columns with low cardinality.
	 * 
	 * Note: This method should be called after making all changes to a table.
	 */
	public void optimizeTableIndices(IdAndVersion tableId);

	/**
	 * Create a temporary copy of the table's index table.
	 * 
	 * @param callback
	 */
	public void createTemporaryTableCopy(IdAndVersion tableId, ProgressCallback callback);

	/**
	 * Delete the temporary copy of table's index.
	 * @param callback
	 */
	public void deleteTemporaryTableCopy(IdAndVersion tableId, ProgressCallback callback);

	/**
	 * Attempt to alter the schema of a temporary copy of a table.
	 * This is used to validate table schema changes.
	 * 
	 * @param progressCallback
	 * @param tableId
	 * @param changes
	 * @return
	 */
	boolean alterTempTableSchmea(IdAndVersion tableId, List<ColumnChangeDetails> changes);


	/**
	 * Populate a view table by coping all of the relevant data from the entity 
	 * replication tables.
	 * @param callback 
	 * 
	 * @param viewType
	 * @param allContainersInScope
	 * @param currentSchema
	 * @return The new CRC23 for the view.
	 */
	public Long populateViewFromEntityReplication(Long tableId, ProgressCallback callback, Long viewTypeMask,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema);
	
	/**
	 * Get the possible ColumnModel definitions based on annotation within a given scope.
	 * @param scope Defined as the list of container ids for a view.
	 * @param nextPageToken Optional: Controls pagination.
	 * @return A ColumnModel for each distinct annotation for the given scope.
	 */
	public ColumnModelPage getPossibleColumnModelsForScope(ViewScope scope, String nextPageToken);
	
	/**
	 * Get the possible ColumnModel definitions based on annotations for a given view.
	 * @param viewId The id of the view to fetch annotation definitions for.
	 * @param nextPageToken Optional: Controls pagination.
	 * @return A ColumnModel for each distinct annotation for the given scope.
	 */
	public ColumnModelPage getPossibleColumnModelsForView(Long viewId, String nextPageToken);

	/**
	 * Build the index for the given table using the provided change metadata up to and
	 * including the provided target change number.
	 * @param progressCallback 
	 * 
	 * @param tableId
	 * @param iterator
	 * @param targetChangeNumber
	 * @throws RecoverableMessageException Will RecoverableMessageException if the index cannot be built at this time.
	 */
	public void buildIndexToChangeNumber(ProgressCallback progressCallback, IdAndVersion idAndVersion, Iterator<TableChangeMetaData> iterator,
			long targetChangeNumber) throws RecoverableMessageException;
	

}
