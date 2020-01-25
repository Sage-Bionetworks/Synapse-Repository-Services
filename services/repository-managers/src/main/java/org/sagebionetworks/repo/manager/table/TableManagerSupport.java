package org.sagebionetworks.repo.manager.table;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Low-level support for all of the table managers. Contains low-level business
 * logic common to all table managers.
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
	public TableStatus getTableStatusOrCreateIfNotExists(IdAndVersion tableId)
			throws NotFoundException;
	
	/**
	 * Get the current state of the given table.
	 * 
	 * @param idAndVersion
	 * @return Optional.empty() is returned if there is no state information for the
	 *         table/view.
	 */
	public Optional<TableState> getTableStatusState(IdAndVersion idAndVersion);

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
	public void attemptToSetTableStatusToAvailable(IdAndVersion tableId,
			String resetToken, String tableChangeEtag)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to set the table status to FAILED. The state will be changed will
	 * be applied as long as the passed resetToken matches the current restToken
	 * indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToFailed(IdAndVersion tableId, Exception exception)
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
	public void attemptToUpdateTableProgress(IdAndVersion tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Called by the worker when it starts to process a table. This method will
	 * change the state to processing without notifying listeners.
	 * 
	 * @param tableId
	 * @return
	 */
	public String startTableProcessing(IdAndVersion tableId);

	/**
	 * Is the table's index synchronized with the truth data?
	 * 
	 * @param tableId
	 * @return
	 */
	public boolean isIndexSynchronizedWithTruth(IdAndVersion tableId);

	/**
	 * Index work is required if the index is out-of-synch with the truth or the
	 * current state is processing.
	 * 
	 * @param tableId
	 * @return
	 */
	public boolean isIndexWorkRequired(IdAndVersion tableId);

	/**
	 * Set the table to processing and send an update message.
	 * 
	 * @param tableId
	 */
	public TableStatus setTableToProcessingAndTriggerUpdate(IdAndVersion tableId);

	/**
	 * Set the table to be deleted.
	 * 
	 * @param deletedId
	 */
	public void setTableDeleted(IdAndVersion deletedId, ObjectType tableType);

	/**
	 * The MD5 hex of a table's schema.
	 * 
	 * @param tableId
	 * @return
	 */
	String getSchemaMD5Hex(IdAndVersion tableId);

	/**
	 * Get the version of the given table. This is can be different for each
	 * table type. The value is used to indicate the current state of a table's
	 * truth.
	 * 
	 * @param tableId
	 * @return
	 */
	long getTableVersion(IdAndVersion tableId);

	/**
	 * Is the given table available.
	 * 
	 * @param tableId
	 * @return
	 */
	boolean isTableAvailable(IdAndVersion tableId);

	/**
	 * Lookup the object type for this table.
	 * 
	 * @param tableId
	 * @return
	 */
	ObjectType getTableType(IdAndVersion tableId);

	/**
	 * Get the number currently associated with a view, for consistency checks.
	 * 
	 * @param table
	 * @return
	 */
	public Long getViewStateNumber(IdAndVersion table);
	
	/**
	 * Get the set of container ids (Projects and Folders) for a view's scope.
	 * The resulting set will include the scope containers plus all folders
	 * contained within each scope.
	 * 
	 * All FileEntities within the the given view will have a parentId from the
	 * returned set.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	public Set<Long> getAllContainerIdsForViewScope(IdAndVersion idAndVersion);

	/**
	 * Get the set of container ids (Projects and Folders) for a view's scope.
	 * The resulting set will include the scope containers plus all folders
	 * contained within each scope.
	 * 
	 * All FileEntities within the the given view will have a parentId from the
	 * returned set.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	public Set<Long> getAllContainerIdsForViewScope(IdAndVersion idAndVersion, Long viewTypeMask);
	
	/**
	 * Get the set of container ids (Projects and Folders) for a given set
	 * of scope Ids. The resulting set will include the scope containers plus all folders
	 * contained within each scope.
	 * 
	 * All FileEntities within the the given view will have a parentId from the
	 * returned set.
	 * @param scope
	 * @return
	 */
	public Set<Long> getAllContainerIdsForScope(Set<Long> scope, Long viewTypeMask);


	/**
	 * <p>
	 * Attempt to acquire an exclusive lock on a table. If the lock is acquired, the
	 * passed Callable will be run while holding lock. The lock will automatically
	 * be release when the caller returns.
	 * </p>
	 * There are several possible conditions that can occur.
	 * <ul>
	 * <li>An exclusive lock has already been issued to another caller. A
	 * LockUnavilableException will be thrown for this case.</li>
	 * <li>One or more non-exclusive locks have been issued for this table. When
	 * this occurs, a reserve will placed that will block all new non-exclusive and
	 * exclusive locks. A wait loop will be started to wait for all outstanding
	 * non-exclusive locks to be release. Once all non-exclusive locks are release,
	 * the exclusive lock will be issued and the passed Caller will be run.</li>
	 * <li>Another caller has reserved the exclusive lock and is waiting for the
	 * exclusive lock. A LockUnavilableException will be thrown for this case.</li>
	 * <li>There are no outstanding non-exclusive locks, no executive lock reserver,
	 * and no exclusive lock. For this case, the reserver and exclusive lock will be
	 * acquired and the Callable will be run.</li>
	 * </ul>
	 * 
	 * @param callback
	 * @param tableId
	 * @param timeoutSeconds The maximum number of seconds the lock should be held
	 *                       before treated as expired.
	 * @param runner
	 * @return
	 * @throws Exception
	 */
	public <R> R tryRunWithTableExclusiveLock(ProgressCallback callback,
			IdAndVersion tableId, int timeoutSeconds, ProgressingCallable<R> runner)
			throws Exception;
	
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
	 * @param callback
	 * @param tableId
	 * @param timeoutSeconds The maximum number of seconds the lock should be held
	 *                       before treated as expired.
	 * @param runner
	 * @return
	 * @throws Exception
	 */
	public <R> R tryRunWithTableNonexclusiveLock(
			ProgressCallback callback, IdAndVersion tableId, int timeoutSeconds,
			ProgressingCallable<R> runner) throws Exception;
	
	/**
	 * @see TableManagerSupport#tryRunWithTableExclusiveLock(ProgressCallback, IdAndVersion, int, ProgressingCallable)
	 * @param callback
	 * @param key
	 * @param timeoutSeconds
	 * @param runner
	 * @return
	 * @throws Exception 
	 */
	public <R> R tryRunWithTableExclusiveLock(ProgressCallback callback, String key,
			int timeoutSeconds, ProgressingCallable<R> runner) throws Exception;

	/**
	 * Validate the user has read access to the given table.
	 * 
	 * @param userInfo
	 * @param tableId
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	EntityType validateTableReadAccess(UserInfo userInfo, IdAndVersion tableId)
			throws UnauthorizedException, DatastoreException, NotFoundException;

	/**
	 * Validate the user has write access to the given table.
	 * 
	 * @param userInfo
	 * @param tableId
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void validateTableWriteAccess(UserInfo userInfo, IdAndVersion tableId)
			throws UnauthorizedException, DatastoreException, NotFoundException;

	/**
	 * Touch the table and update the etag, modifiedOn, and modifiedBy.
	 * This will also lock the table.
	 * 
	 * @param user
	 * @param tableId
	 * @return
	 */
	public String touchTable(UserInfo user, String tableId);

	/**
	 * Given a set of benefactor Ids get the sub-set of benefactor IDs for which
	 * the given user has read access.
	 * 
	 * @param user
	 * @param benefactorIds
	 * @return
	 */
	public Set<Long> getAccessibleBenefactors(UserInfo user,
			Set<Long> benefactorIds);

	/**
	 * Get the ColumnModel for a given EntityField.
	 * 
	 * @param field
	 * @return
	 */
	public ColumnModel getColumnModel(EntityField field);

	/**
	 * Get the ColumnModels for the passed fields.
	 * 
	 * @param field
	 * @return
	 */
	public List<ColumnModel> getColumnModels(EntityField... field);

	/**
	 * Get the default ColumnModels for a view based on the viewTypeMask.
	 * 
	 * @param viewTypeMask Bit mask of the types included in the view.
	 * 
	 * @return
	 */
	public List<ColumnModel> getDefaultTableViewColumns(Long viewTypeMask);

	/**
	 * Get the entity type for the given table.
	 * 
	 * @param tableId
	 * @return
	 */
	public EntityType getTableEntityType(IdAndVersion tableId);

	/**
	 * Get the path of the given entity.
	 * 
	 * @param entityId
	 * @return
	 */
	public List<Long> getEntityPath(IdAndVersion entityId);

	/**
	 * Get the view type for the given table ID.
	 * 
	 * @param viewId
	 * @return
	 */
	Long getViewTypeMask(IdAndVersion viewId);

	/**
	 * Only Administrator can perform this action.
	 * Trigger a table/ view to be rebuilt.
	 * 
	 * @param userInfo
	 * @param tableId
	 */
	public void rebuildTable(UserInfo userInfo, IdAndVersion tableId);

	/**
	 * Validate that the given scope is within the size limit.
	 * 
	 * @param scopeIds
	 * @param type
	 */
	public void validateScopeSize(Set<Long> scopeIds, Long viewTypeMask);

	/**
	 * Does the given table exist?  If the table is in the trash then this will
	 * return true.  Will only return true if the table no longer exists.
	 * @param tableId
	 * @return
	 */
	public boolean doesTableExist(IdAndVersion tableId);

	/**
	 * Get the last change number for the given table ID and version pair.
	 * 
	 * @param idAndVersion Present if there is at least one change for the given pair.
	 * @return
	 */
	Optional<Long> getLastTableChangeNumber(IdAndVersion idAndVersion);

	/**
	 * Get the schema for the given id and version combination.
	 * @param idAndVersion
	 * @return
	 */
	public List<ColumnModel> getTableSchema(IdAndVersion idAndVersion);

	/**
	 * Will update the tableStatus.changedOn for the given table if its state is
	 * currently available. If the table's state is not available, this call will do
	 * nothing.
	 * 
	 * @param tableId
	 * @return True if the table's state was available and changeOn was updated.
	 */
	boolean updateChangedOnIfAvailable(IdAndVersion idAndVersion);

	/**
	 * Get table status last changed on date for the given table.
	 * @param idAndVersion
	 * @return
	 */
	Date getLastChangedOn(IdAndVersion idAndVersion);

}
