package org.sagebionetworks.repo.model.dao.table;

import java.util.Date;
import java.util.Optional;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This DAO provides status information about a table.
 * 
 * @author John
 * 
 */
public interface TableStatusDAO {

	/**
	 * When the truth data of a table changes either due to row changes or column
	 * changes the table status must be rest to PROCESSING. This is the only method
	 * that will change the reset-token. The resulting reset-token will be needed to
	 * change the status to available or failed.
	 * 
	 * @param tableId
	 * @return The new reset-token for this table status.
	 */
	public String resetTableStatusToProcessing(IdAndVersion tableId);

	/**
	 * Attempt to set the table status to AVAILABLE. The state will be changed will
	 * be applied as long as the passed resetToken matches the current restToken
	 * indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @param tableChangeEtag The etag of the last table change processed.
	 * @return
	 * @throws ConflictingUpdateException Thrown when the passed restToken does not
	 *                                    match the current resetToken. This
	 *                                    indicates that the table was updated
	 *                                    before processing finished so we cannot
	 *                                    change the status to available until the
	 *                                    new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToAvailable(IdAndVersion tableId, String resetToken, String tableChangeEtag)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to set the table status to FAILED. The state will be changed will be
	 * applied as long as the passed resetToken matches the current restToken
	 * indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @return
	 * @throws ConflictingUpdateException Thrown when the passed restToken does not
	 *                                    match the current resetToken. This
	 *                                    indicates that the table was updated
	 *                                    before processing finished so we cannot
	 *                                    change the status to available until the
	 *                                    new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToFailed(IdAndVersion tableId, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to update the progress of a table. Will fail if the passed rest-token
	 * does not match the current reset-token indicating the table change while it
	 * was being processed.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @param progressMessage
	 * @param currentProgress
	 * @param totalProgress
	 * @throws ConflictingUpdateException Thrown when the passed restToken does not
	 *                                    match the current resetToken. This
	 *                                    indicates that the table was updated
	 *                                    before processing finished.
	 * @throws NotFoundException
	 */
	public void attemptToUpdateTableProgress(IdAndVersion tableId, String resetToken, String progressMessage,
			Long currentProgress, Long totalProgress) throws ConflictingUpdateException, NotFoundException;

	/**
	 * Get the current status of a table.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException Thrown when there is no status information for the
	 *                           given table.
	 */
	public TableStatus getTableStatus(IdAndVersion tableId) throws NotFoundException;

	/**
	 * Get the current state of the given table.
	 * 
	 * @param idAndVersion
	 * @return Optional.empty() is returned if there is no state information for the
	 *         table/view.
	 */
	public Optional<TableState> getTableStatusState(IdAndVersion tableId) throws NotFoundException;

	/**
	 * Delete the table status for this table. Called during migration if table was
	 * updated in staging and we don't want stale status
	 * 
	 */
	public void deleteTableStatus(IdAndVersion tableId);

	/**
	 * Remove all table state. This should not be called during normal operations.
	 * 
	 */
	public void clearAllTableState();

	/**
	 * The the last changed on date for the given table.
	 * 
	 * @param tableId
	 * @return
	 */
	public Date getLastChangedOn(IdAndVersion tableId);

	/**
	 * Will update the changedOn of the given table if its state is currently
	 * available. If the table's state is not available, this call will do nothing.
	 * 
	 * @param tableId
	 * @return True if the table's state was available and changeOn was updated.
	 */
	public boolean updateChangedOnIfAvailable(IdAndVersion tableId);

	/**
	 * Get the etag of the last change that has been applied to the given table.
	 * 
	 * @param idAndVersion
	 * @return Return Optional.empty() when the there is no status for the given
	 *         table. If the table does have status, then the etag of the last
	 *         change will be returned.
	 */
	public Optional<String> getLastChangeEtag(IdAndVersion idAndVersion);
}
