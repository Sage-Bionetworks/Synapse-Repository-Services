package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	 * When the truth data of a table changes either due to row changes or
	 * column changes the table status must be rest to PROCESSING. This is the
	 * only method that will change the reset-token. The resulting reset-token
	 * will be needed to change the status to available or failed.
	 * 
	 * @param tableId
	 * @return The new reset-token for this table status.
	 */
	public String resetTableStatusToProcessing(IdAndVersion tableId);

	/**
	 * Attempt to set the table status to AVIALABLE. The state will be changed
	 * will be applied as long as the passed resetToken matches the current
	 * restToken indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @param tableChangeEtag The etag of the last table change processed.
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToAvailable(IdAndVersion tableId,
			String resetToken, String tableChangeEtag) throws ConflictingUpdateException,
			NotFoundException;

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
	public void attemptToSetTableStatusToFailed(IdAndVersion tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to update the progress of a table.
	 * Will fail if the passed rest-token does not match the current reset-token indicating
	 * the table change while it was being processed.
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
	 * Get the current status of a table.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 *             Thrown when there is no status information for the given
	 *             table.
	 */
	public TableStatus getTableStatus(IdAndVersion tableId) throws NotFoundException;

	/**
	 * Delete the table status for this table. Called during migration if table was updated in staging and we don't want
	 * stale status
	 * 
	 */
	public void deleteTableStatus(IdAndVersion tableId);

	/**
	 * Remove all table state. This should not be called during normal operations.
	 * 
	 */
	public void clearAllTableState();
}
