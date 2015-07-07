package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.SetMultimap;

/**
 * This worker updates the index used to support the tables features. It will
 * listen to table changes and apply them to the RDS table that acts as the
 * index of the table feature.
 * 
 * @author John
 * 
 */
public class TableWorker implements ChangeMessageDrivenRunner {

	enum State {
		SUCCESS, UNRECOVERABLE_FAILURE, RECOVERABLE_FAILURE,
	}

	private static final long BATCH_SIZE = 16000;

	static private Logger log = LogManager.getLogger(TableWorker.class);

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	StackConfiguration configuration;
	@Autowired
	NodeInheritanceManager nodeInheritanceManager;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// If the feature is disabled then we simply swallow all messages
		if (!configuration.getTableEnabled()) {
			return;
		}
		// We only care about entity messages here
		if (ObjectType.TABLE.equals((change.getObjectType()))) {
			if (ChangeType.DELETE.equals(change.getChangeType())) {
				// Delete the table in the index
				TableIndexDAO indexDao = tableConnectionFactory
						.getConnection(change.getObjectId());
				if (indexDao != null) {
					indexDao.deleteTable(change.getObjectId());
					indexDao.deleteStatusTable(change.getObjectId());
				}
				return;
			} else {
				// Create or update.
				String tableId = change.getObjectId();
				// make sure the table is not in the trash
				try {
					if (nodeInheritanceManager.isNodeInTrash(tableId)) {
						return;
					}
				} catch (NotFoundException e) {
					// if the table no longer exists, we want to stop trying
					return;
				}
				// this method does the real work.
				State state = createOrUpdateTable(progressCallback, tableId,
						change.getObjectEtag(), change);
				if (State.RECOVERABLE_FAILURE.equals(state)) {
					throw new RecoverableMessageException();
				}
			}
		}
	}

	/**
	 * This is where a single table index is created or updated.
	 * 
	 * @param tableId
	 * @return
	 */
	public State createOrUpdateTable(
			final ProgressCallback<ChangeMessage> progressCallback,
			final String tableId, final String tableResetToken,
			final ChangeMessage change) {
		// Attempt to run with
		try {
			// If the passed token does not match the current token then this
			// is an old message that should be removed from the queue.
			// See PLFM-2641. We must check message before we acquire the lock.
			TableStatus status = tableRowManager
					.getTableStatusOrCreateIfNotExists(tableId);
			// If the reset-tokens do not match this message should be ignored
			if (!tableResetToken.equals(status.getResetToken())) {
				// This is an old message so we ignore it
				return State.SUCCESS;
			}
			log.info("Get creat index lock " + tableId);
			// Run with the exclusive lock on the table if we can get it.
			return tableRowManager.tryRunWithTableExclusiveLock(tableId,
					configuration.getTableWorkerTimeoutMS(),
					new Callable<State>() {
						@Override
						public State call() throws Exception {
							// This method does the real work.
							return createOrUpdateWhileHoldingLock(
									progressCallback, tableId, tableResetToken,
									change);
						}
					});
		} catch (LockUnavilableException e) {
			// We did not get the lock on this table.
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (ConflictingUpdateException e) {
			// This is thrown when the table gets updated while it is being
			// processed.
			// We cannot continue with this attempt.
			return State.UNRECOVERABLE_FAILURE;
		} catch (NotFoundException e) {
			// This is thrown if the table no longer exits
			return State.UNRECOVERABLE_FAILURE;
		} catch (InterruptedException e) {
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (Exception e) {
			log.error("Failed with unknown error", e);
			// Cannot recover from this.
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * This is where the table status gets stated and error handling is
	 * performed.
	 * 
	 * @param tableId
	 * @param token
	 * @return
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	private State createOrUpdateWhileHoldingLock(
			ProgressCallback<ChangeMessage> progressCallback, String tableId,
			String tableResetToken, ChangeMessage change)
			throws ConflictingUpdateException, NotFoundException {
		// Start the real work
		log.info("Create index " + tableId);
		try {
			// Save the status before we start
			// Try to get a connection.
			TableIndexDAO indexDAO = tableConnectionFactory
					.getConnection(tableId);
			// If we do not have connection we can try again later
			if (indexDAO == null) {
				return State.RECOVERABLE_FAILURE;
			}
			// This method will do the rest of the work.
			String lastEtag = synchIndexWithTable(progressCallback, indexDAO,
					tableId, tableResetToken, change);
			// We are finished set the status
			log.info("Create index " + tableId + " done");
			tableRowManager.attemptToSetTableStatusToAvailable(tableId,
					tableResetToken, lastEtag);
			return State.SUCCESS;
		} catch (TableUnavilableException e) {
			// recoverable
			tableRowManager.attemptToUpdateTableProgress(tableId,
					tableResetToken, e.getStatus().getProgressMessage(), e
							.getStatus().getProgressCurrent(), e.getStatus()
							.getProgressTotal());
			log.info("Create index " + tableId + " aborted, unavailable");
			return State.RECOVERABLE_FAILURE;
		} catch (Exception e) {
			// Failed.
			// Get the stack trace.
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			// Attempt to set the status to failed.
			tableRowManager.attemptToSetTableStatusToFailed(tableId,
					tableResetToken, e.getMessage(), writer.toString());
			// This is not an error we can recover from.
			log.info("Create index " + tableId + " aborted, unrecoverable");
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * Synchronizes the table index with the table truth data. After the
	 * successful completion of this method the table index schema will match
	 * the schema of the truth and all row changes that have not already been
	 * applied will be applied. Note: This method will do no work if the index
	 * and truth are already synchronized.
	 * 
	 * @param connection
	 * @param tableId
	 * @param status
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws TableUnavilableException
	 */
	String synchIndexWithTable(ProgressCallback<ChangeMessage> progressCallback,
			TableIndexDAO indexDao, String tableId, String resetToken,
			ChangeMessage change) throws DatastoreException, NotFoundException,
			IOException, TableUnavilableException {
		// The first task is to get the table schema in-synch.
		// Get the current schema of the table.
		List<ColumnModel> currentSchema = tableRowManager
				.getColumnModelsForTable(tableId);
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(
				currentSchema, false);
		// Create or update the table with this schema.
		tableRowManager.attemptToUpdateTableProgress(tableId, resetToken,
				"Creating table ", 0L, 100L);
		if (currentSchema.isEmpty()) {
			// If there is no schema delete the table
			indexDao.deleteTable(tableId);
		} else {
			// We have a schema so create or update the table
			indexDao.createOrUpdateTable(currentSchema, tableId);
		}
		// Now determine which changes need to be applied to the table
		Long maxCurrentCompleteVersion = indexDao
				.getMaxCurrentCompleteVersionForTable(tableId);
		// List all of the changes
		tableRowManager.attemptToUpdateTableProgress(tableId, resetToken,
				"Getting current table row versions ", 0L, 100L);

		TableRowChange lastTableRowChange = tableRowManager
				.getLastTableRowChange(tableId);
		if (lastTableRowChange == null) {
			// nothing to do, move along
			return null;
		}

		long currentProgress = 0;
		long maxRowId = tableRowManager.getMaxRowId(tableId);
		for (long rowId = 0; rowId <= maxRowId; rowId += BATCH_SIZE) {
			Map<Long, Long> currentRowVersions = tableRowManager
					.getCurrentRowVersions(tableId,
							maxCurrentCompleteVersion + 1, rowId, BATCH_SIZE);

			// gather rows by version
			SetMultimap<Long, Long> versionToRowsMap = TableModelUtils
					.createVersionToRowIdsMap(currentRowVersions);
			for (Entry<Long, Collection<Long>> versionWithRows : versionToRowsMap
					.asMap().entrySet()) {
				Set<Long> rowsToGet = (Set<Long>) versionWithRows.getValue();
				Long version = versionWithRows.getKey();

				// Keep this message invisible
				progressCallback.progressMade(change);

				// This is a change that we must apply.
				RowSet rowSet = tableRowManager.getRowSet(tableId, version,
						rowsToGet, mapper);

				tableRowManager.attemptToUpdateTableProgress(tableId,
						resetToken, "Applying rows " + rowSet.getRows().size()
								+ " to version: " + version, currentProgress,
						currentProgress);
				// apply the change to the table
				indexDao.createOrUpdateOrDeleteRows(rowSet, currentSchema);
				currentProgress += rowsToGet.size();
			}
		}

		// If we successfully updated the table and got here, then all we know
		// is that we are at least at the version
		// the table was at when we started. It is still possible that a newer
		// version came in and the we applied
		// partial updates from that version to the index. A subsequent update
		// will make things consistent again.
		indexDao.setMaxCurrentCompleteVersionForTable(tableId,
				lastTableRowChange.getRowVersion());

		return lastTableRowChange.getEtag();
	}

}
