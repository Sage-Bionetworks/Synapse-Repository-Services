package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;

/**
 * The 'truth' of a table consists of metadata in the main repository RDS and
 * changes sets that consist of compressed CSV files stored in S3. Each change
 * set is tracked in the main repository database. In order to query a table, an
 * 'index' must first be built from a table's metadata and change sets. A
 * table's index is built in a separate database cluster for scalability.
 * 
 * This class is used to manage each table's index in the database cluster.
 * 
 * @author John
 * 
 */
public interface TableIndexManager {

	/**
	 * Represents a connection to a table's index.
	 */
	public interface TableIndexConnection {
	}

	/**
	 * Create a connection to a table's index. This will be used for all
	 * subsequent calls to the table. Do not cache or hold onto connection
	 * beyond the scope of a single operation.
	 * 
	 * @param tableId
	 * @return
	 * @throws TableIndexConnectionUnavailableException
	 *             Thrown if a connection cannot be made at this time.
	 */
	public TableIndexConnection connectToTableIndex(String tableId)
			throws TableIndexConnectionUnavailableException;

	/**
	 * For a given tableId, get the current version of the index. This is the
	 * version number of the last change set applied to index.
	 * 
	 * @param tableId
	 * @return
	 */
	public long getCurrentVersionOfIndex(TableIndexConnection connection);

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
	 * @param connection
	 *            Connection to a table's index. See:
	 *            {@link #connectToTableIndex(String)}.
	 * @param rowset
	 *            Rowset to apply.
	 * @param currentSchema
	 *            The current schema of the table.
	 * @param changeSetVersionNumber
	 *            The version number of the changeset. Note, this version number
	 *            must match the version number of each row in the passed
	 *            changeset.
	 */
	public void applyChangeSetToIndex(TableIndexConnection connection,
			RowSet rowset, List<ColumnModel> currentSchema,
			long changeSetVersionNumber);

	/**
	 * Set the current schema of a table's index.
	 * 
	 * @param connection
	 *            Connection to a table's index. See:
	 *            {@link #connectToTableIndex(String)}.
	 * @param currentSchema
	 */
	public void setIndexSchema(TableIndexConnection connection,
			List<ColumnModel> currentSchema);
}
