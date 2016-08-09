package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.FileEntityFields;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Low-level support for all of the table managers. Contains low-level
 * business logic common to all table managers.
 *
 */
public interface TableManagerSupport {

	/**
	 * Get the status of a table. This method is guaranteed to return a table's
	 * status if the table exists. Note: Calling this method can trigger a table
	 * to update if the table's status is out-of-date for any reason. If an
	 * update is triggered, the returned table status will be set to PROCESSING.
	 * The returned table status will only be AVAIABLE if the table's index is
	 * up-to-date (see PLFM-3383).
	 * 
	 * @param tableId
	 * @return the status
	 * @throws NotFoundException
	 *             if the table does not exist
	 */
	public TableStatus getTableStatusOrCreateIfNotExists(String tableId)
			throws NotFoundException;

	/**
	 * Attempt to set the table status to AVIALABLE. The state will be changed
	 * will be applied as long as the passed resetToken matches the current
	 * restToken indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken, String tableChangeEtag)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to set the table status to FAILED. The state will be changed will
	 * be applied as long as the passed resetToken matches the current restToken
	 * indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to update the progress of a table. Will fail if the passed
	 * rest-token does not match the current reset-token indicating the table
	 * change while it was being processed.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @param progressMessage
	 * @param currentProgress
	 * @param totalProgress
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished.
	 * @throws NotFoundException
	 */
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Called by the worker when it starts to process a table. This method will
	 * change the state to processing without notifying listeners.
	 * 
	 * @param tableId
	 * @return
	 */
	public String startTableProcessing(String tableId);

	/**
	 * Is the table's index synchronized with the truth data?
	 * 
	 * @param tableId
	 * @return
	 */
	public boolean isIndexSynchronizedWithTruth(String tableId);

	/**
	 * Index work is required if the index is out-of-synch with the truth or the
	 * current state is processing.
	 * 
	 * @param tableId
	 * @return
	 */
	public boolean isIndexWorkRequired(String tableId);

	/**
	 * Set the table to processing and send an update message.
	 * 
	 * @param tableId
	 */
	public TableStatus setTableToProcessingAndTriggerUpdate(String tableId);

	/**
	 * Set the table to be deleted.
	 * 
	 * @param deletedId
	 */
	public void setTableDeleted(String deletedId, ObjectType tableType);	
	
	/**
	 * The MD5 hex of a table's schema.
	 * 
	 * @param tableId
	 * @return
	 */
	String getSchemaMD5Hex(String tableId);

	/**
	 * Get the version of the given table. This is can be different for each
	 * table type. The value is used to indicate the current state of a table's
	 * truth.
	 * 
	 * @param tableId
	 * @return
	 */
	long getTableVersion(String tableId);

	/**
	 * Is the given table available.
	 * 
	 * @param tableId
	 * @return
	 */
	boolean isTableAvailable(String tableId);

	/**
	 * Lookup the object type for this table.
	 * 
	 * @param tableId
	 * @return
	 */
	ObjectType getTableType(String tableId);
	
	/**
	 * Calculate a Cyclic Redundancy Check (CRC) of a TableView.
	 * The CRC is calculated as SUM(CRC23(CONCAT(ID, '-', ETAG)))
	 * given the ID and ETAG of each entity within the view's scope.
	 * 
	 * Warning this call is not cheap.
	 * 
	 * @param table
	 * @return
	 */
	public Long calculateFileViewCRC32(String table);
	
	/**
	 * Calculate a Cyclic Redundancy Check (CRC) of a TableView.
	 * The CRC is calculated as SUM(CRC23(CONCAT(ID, '-', ETAG)))
	 * given the ID and ETAG of each entity within the view's scope.
	 * @param allContainersInScope
	 * @return
	 */
	public Long calculateFileViewCRC32(Set<Long> allContainersInScope, ViewType type);
	
	/**
	 * Get the set of container ids (Projects and Folders) for a view's scope.
	 * The resulting set will include the scope containers plus all folders
	 * contained within each scope.
	 * 
	 * All FileEntities within the the given view will have a parentId from the
	 * returned set.
	 * 
	 * @param viewId
	 * @return
	 */
	public Set<Long> getAllContainerIdsForViewScope(String viewId);
	
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
	 * @return
	 */
	public <R> R tryRunWithTableExclusiveLock(ProgressCallback<Void> callback, String tableId, int timeoutMS,
			ProgressingCallable<R, Void> runner) throws Exception;
	
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
	 */
	public <R, T> R tryRunWithTableNonexclusiveLock(ProgressCallback<T> callback, String tableId,
			int timeoutMS, ProgressingCallable<R, T> runner) throws Exception;

	/**
	 * Validate the user has read access to the given table.
	 * 
	 * @param userInfo
	 * @param tableId
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	EntityType validateTableReadAccess(UserInfo userInfo, String tableId)
			throws UnauthorizedException, DatastoreException, NotFoundException;

	/**
	 * Validate the user has write access to the given table.
	 * @param userInfo
	 * @param tableId
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void validateTableWriteAccess(UserInfo userInfo, String tableId)
			throws UnauthorizedException, DatastoreException, NotFoundException;
	
	/**
	 * Get the current ColumnModel list for a table.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public List<ColumnModel> getColumnModelsForTable(String tableId)
			throws DatastoreException, NotFoundException;

	/**
	 * Blocking lock on the given table ID.
	 * 
	 * @param tableId
	 */
	public void lockOnTableId(String tableId);
	
	/**
	 * Given a set of benefactor Ids get the sub-set of benefactor IDs
	 * for which the given user has read access.
	 * @param user
	 * @param benefactorIds
	 * @return
	 */
	public Set<Long> getAccessibleBenefactors(UserInfo user,
			Set<Long> benefactorIds);
	
	/**
	 * Get the ColumnModel for a given FileEntityField.
	 * 
	 * @param field
	 * @return
	 */
	public ColumnModel getColumModel(FileEntityFields field);
	
	/**
	 * Get the default ColumnModels for each primary filed of FileEntity.
	 * 
	 * @return
	 */
	public List<ColumnModel> getDefaultTableViewColumns(ViewType viewType);

	/**
	 * Get the entity type for the given table.
	 * @param tableId
	 * @return
	 */
	public EntityType getTableEntityType(String tableId);

	/**
	 * Get the path of the given entity.
	 * @param entityId
	 * @return
	 */
	public Set<Long> getEntityPath(String entityId);

	/**
	 * Get the view type for the given table ID.
	 * @param tableId
	 * @return
	 */
	ViewType getViewType(String tableId);
	
	
	/**
	 * Execute the given callback with automatic progress events generated for
	 * the provided callback. This allows the callable to run for long periods
	 * of time while maintaining progress events.
	 * 
	 * @param callback
	 *            Progress events will be generated for the provided callback at
	 *            a fix frequency regardless of the amount of time the callable
	 *            takes to execute.
	 * 
	 * @param parameter
	 *            The parameter to pass to the callback.
	 * 
	 * @param callable
	 *            The callable to be executed.
	 * @return
	 * @throws Exception 
	 */
	public <R> R callWithAutoProgress(ProgressCallback<Void> callback, Callable<R> callable) throws Exception;
	
	

}
