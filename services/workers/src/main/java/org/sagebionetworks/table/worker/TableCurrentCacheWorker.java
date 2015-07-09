package org.sagebionetworks.table.worker;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker updates the current row cache to support the tables features. It
 * will listen to table changes and apply them to the RDS current cache table
 */
public class TableCurrentCacheWorker implements ChangeMessageDrivenRunner {

	static private Logger log = LogManager
			.getLogger(TableCurrentCacheWorker.class);

	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	NodeInheritanceManager nodeInheritanceManager;
	@Autowired
	StackConfiguration stackConfiguration;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// If the feature is disabled then we simply swallow all messages
		if (!stackConfiguration.getTableEnabled()) {
			return;
		}
		// We only care about entity messages here
		if (ObjectType.TABLE.equals((change.getObjectType()))) {
			String tableId = change.getObjectId();
			if (ChangeType.DELETE.equals(change.getChangeType())) {
				// Delete the cache for this table
				tableRowManager.removeCaches(tableId);
			} else {
				// make sure the table is not in the trash
				try {
					if (nodeInheritanceManager.isNodeInTrash(tableId)) {
						return;
					}
				} catch (NotFoundException e) {
					// if the table no longer exists, we want to stop trying
					return;
				}
				// Create or update.
				// this method does the real work.
				updateCurrentVersionCache(progressCallback, tableId, change.getObjectEtag(),
						change);
			}
		}
	}

	/**
	 * This is where a single table index is created or updated.
	 * 
	 * @param tableId
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public void updateCurrentVersionCache(final ProgressCallback<ChangeMessage> progressCallback, final String tableId,
			final String tableResetToken, final ChangeMessage message)
			throws IOException {
		// If the passed token does not match the current token then this
		// is an old message that should be removed from the queue.
		// See PLFM-2641. We must check message before we acquire the lock.
		TableStatus status;
		try {
			status = tableRowManager.getTableStatusOrCreateIfNotExists(tableId);
		} catch (NotFoundException e) {
			// if the table doesn't exist, we assume the message was old and we
			// consider it handled
			log.info("Updating current versions for " + tableId
					+ " aborted because table does not exist");
			return;
		}

		// If the reset-tokens do not match this message should be ignored
		if (!tableResetToken.equals(status.getResetToken())) {
			// This is an old message so we ignore it
			return;
		}

		log.info("Updating current versions for " + tableId);
		tableRowManager.updateLatestVersionCache(tableId,
				new org.sagebionetworks.util.ProgressCallback<Long>() {
					@Override
					public void progressMade(Long version) {
						progressCallback.progressMade(message);
					}
				});
		log.info("Updating current versions for " + tableId + " done");
	}

}
