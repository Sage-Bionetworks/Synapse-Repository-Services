package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker updates the index used to support the tables features. It will listen to table changes and apply them to
 * the RDS table that acts as the index of the table feature.
 * 
 * @author John
 * 
 */
public class TableCurrentCacheWorker implements Callable<List<Message>> {

	static private Logger log = LogManager.getLogger(TableCurrentCacheWorker.class);
	List<Message> messages;
	TableRowManager tableRowManager;
	boolean featureEnabled;
	WorkerProgress workerProgress;

	/**
	 * Worker that keeps the current version cache up to date
	 * 
	 * @param messages
	 * @param tableRowManager
	 * @param configuration
	 * @param workerProgress
	 */
	public TableCurrentCacheWorker(List<Message> messages, TableRowManager tableRowManager, StackConfiguration configuration,
			WorkerProgress workerProgress) {
		super();
		this.messages = messages;
		this.tableRowManager = tableRowManager;
		this.featureEnabled = configuration.getTableEnabled();
		this.workerProgress = workerProgress;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		// If the feature is disabled then we simply swallow all messages
		if (!featureEnabled) {
			return messages;
		}
		// process each message
		for (Message message : messages) {
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			// We only care about entity messages here
			if (ObjectType.TABLE.equals((change.getObjectType()))) {
				String tableId = change.getObjectId();
				if (ChangeType.DELETE.equals(change.getChangeType())) {
					// Delete the cache for this table
					tableRowManager.removeLatestVersionCache(tableId);
					processedMessages.add(message);
				} else {
					// Create or update.
					// this method does the real work.
					updateCurrentVersionCache(tableId, change.getObjectEtag(), message);
					processedMessages.add(message);
				}
			} else {
				// Non-table messages must be returned so they can be removed
				// from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}

	/**
	 * This is where a single table index is created or updated.
	 * 
	 * @param tableId
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public void updateCurrentVersionCache(final String tableId, final String tableResetToken, final Message message) throws IOException {
		// If the passed token does not match the current token then this
		// is an old message that should be removed from the queue.
		// See PLFM-2641. We must check message before we acquire the lock.
		TableStatus status;
		try {
			status = tableRowManager.getTableStatus(tableId);
		} catch (NotFoundException e) {
			// table no longer exists? We shouldn't keep retrying here
			log.info("Table " + tableId + " no longer exists?" + e);
			return;
		}

		// If the reset-tokens do not match this message should be ignored
		if (!tableResetToken.equals(status.getResetToken())) {
			// This is an old message so we ignore it
			return;
		}

		tableRowManager.updateLatestVersionCache(tableId, new ProgressCallback<Long>() {
			@Override
			public void progressMade(Long version) {
				workerProgress.progressMadeForMessage(message);
			}
		});
	}
}
