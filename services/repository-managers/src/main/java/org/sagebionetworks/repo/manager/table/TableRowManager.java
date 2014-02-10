package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for Table Row management.
 * 
 * @author jmhill
 * 
 */
public interface TableRowManager {

	/**
	 * Append a set of rows to a table.
	 * 
	 * @param user
	 * @param delta
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws IOException
	 */
	public RowReferenceSet appendRows(UserInfo user, String tableId,
			List<ColumnModel> models, RowSet delta) throws DatastoreException,
			NotFoundException, IOException;

	/**
	 * Update the status of a table if the etag matches the expected value.
	 * 
	 * @param expectedEtag
	 * @param newStatus
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the current etag of the table does not match the
	 *             passed etag.
	 */
	public TableStatus updateTableStatus(String expectedEtag,
			TableStatus newStatus) throws ConflictingUpdateException;

	/**
	 * Get the current ColumnModel list for a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<ColumnModel> getColumnModelsForTable(String tableId);

	/**
	 * List the changes that have been applied to a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);

	/**
	 * Get a specific RowSet.
	 * 
	 * @param tableId
	 * @param rowVersion
	 * @return
	 */
	public RowSet getRowSet(String tableId, Long rowVersion);

	/**
	 * <p>
	 * Attempt to acquire an exclusive lock on a table. If the lock is acquired,
	 * the passed Callable will be run while holding lock. The lock will
	 * automatically be release when the caller returns.
	 * </p>
	 * There are several possible conditions that can occur.
	 * <ul>
	 * <li>An exclusive lock has already been issued to another caller. A
	 * LockUnavilableException will be thrown for this case.</li>
	 * <li>One or more non-exclusive locks have been issued for this table. When
	 * this occurs, a reserve will placed that will block all new non-exclusive
	 * and exclusive locks. A wait loop will be started to wait for all
	 * outstanding non-exclusive locks to be release. Once all non-exclusive
	 * locks are release, the exclusive lock will be issued and the passed
	 * Caller will be run.</li>
	 * <li>Another caller has reserved the exclusive lock and is waiting for the
	 * exclusive lock. A LockUnavilableException will be thrown for this case.</li>
	 * <li>There are no outstanding non-exclusive locks, no executive lock
	 * reserver, and no exclusive lock. For this case, the reserver and
	 * exclusive lock will be acquired and the Callable will be run.</li>
	 * </ul>
	 * 
	 * @param tableId
	 * @param runner
	 * @throws LockUnavilableException
	 *             Thrown when an exclusive lock cannot be acquired.
	 * 
	 * @return
	 */
	public <T> T runWithTableExclusiveLock(String tableId, Callable<T> runner)
			throws LockUnavilableException;

	/**
	 * <p>
	 * Attempt to acquire a non-exclusive lock on a table. If the lock is
	 * acquired, the passed Callable will be run while holding lock. The lock
	 * will automatically be release when the caller returns.
	 * </p>
	 * There are several possible conditions that can occur.
	 * <ul>
	 * <li>An exclusive lock has already been issued to another caller. A
	 * LockUnavilableException will be thrown for this case.</li>
	 * <li>One or more non-exclusive locks have been issued for this table. When
	 * this occurs another new non-exclusive lock will be acquired and the
	 * passed Callable will be run. There is no limit to the number of
	 * non-exclusive locks that can be issued for a single table.</li>
	 * <li>Another caller has reserved the exclusive lock and is waiting for the
	 * exclusive lock. A LockUnavilableException will be thrown for this case.</li>
	 * <li>There are no outstanding locks on this table at all. A new
	 * non-exclusive lock will be issue and the passed Callable will be run.</li>
	 * </ul>
	 * 
	 * @param tableId
	 * @param runner
	 * @return
	 * @throws LockUnavilableException
	 */
	public <T> T runWithTableNonexclusiveLock(String tableId, Callable<T> runner)
			throws LockUnavilableException;

}
