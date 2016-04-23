package org.sagebionetworks.repo.manager.table;

import java.util.Set;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
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
	 * 
	 * @param tableId
	 * @param tableType
	 * @return
	 */
	public TableStatus setTableToProcessingAndTriggerUpdate(String tableId,
			ObjectType tableType);

	/**
	 * Set the table to be deleted.
	 * 
	 * @param deletedId
	 */
	public void setTableDeleted(String deletedId);

	/**
	 * Validate the table is available.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 *             If the table does not exist
	 * @throws TableUnavilableException
	 *             If the table exists but is currently processing.
	 * @throws TableFailedException
	 *             If the table exists but processing failed.
	 */
	public TableStatus validateTableIsAvailable(String tableId)
			throws NotFoundException, TableUnavilableException,
			TableFailedException;
	
	
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

}
