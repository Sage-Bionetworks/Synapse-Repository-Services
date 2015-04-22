package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker updates the current row cache to support the tables features. It will listen to table changes and apply
 * them to the RDS current cache table
 */
public class TableCurrentCacheWorker implements Worker {

	static private Logger log = LogManager.getLogger(TableCurrentCacheWorker.class);

	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	NodeInheritanceManager nodeInheritanceManager;
	@Autowired
	StackConfiguration stackConfiguration;

	List<Message> messages;
	WorkerProgress workerProgress;

	public TableCurrentCacheWorker() {
	}

	// testing only constructor
	TableCurrentCacheWorker(List<Message> messages, TableRowManager tableRowManager, StackConfiguration configuration,
			WorkerProgress workerProgress, NodeInheritanceManager nodeInheritanceManager) {
		this.messages = messages;
		this.tableRowManager = tableRowManager;
		this.stackConfiguration = configuration;
		this.workerProgress = workerProgress;
		this.nodeInheritanceManager = nodeInheritanceManager;
	}

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		// If the feature is disabled then we simply swallow all messages
		if (!stackConfiguration.getTableEnabled()) {
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
					tableRowManager.removeCaches(tableId);
					processedMessages.add(message);
				} else {
					// make sure the table is not in the trash
					try {
						if (nodeInheritanceManager.isNodeInTrash(tableId)) {
							processedMessages.add(message);
							continue;
						}
					} catch (NotFoundException e) {
						// if the table no longer exists, we want to stop trying
						processedMessages.add(message);
						continue;
					}
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
			status = tableRowManager.getTableStatusOrCreateIfNotExists(tableId);
		} catch (NotFoundException e) {
			// if the table doesn't exist, we assume the message was old and we consider it handled
			log.info("Updating current versions for " + tableId + " aborted because table does not exist");
			return;
		}

		// If the reset-tokens do not match this message should be ignored
		if (!tableResetToken.equals(status.getResetToken())) {
			// This is an old message so we ignore it
			return;
		}

		log.info("Updating current versions for " + tableId);
		tableRowManager.updateLatestVersionCache(tableId, new ProgressCallback<Long>() {
			@Override
			public void progressMade(Long version) {
				workerProgress.progressMadeForMessage(message);
			}
		});
		log.info("Updating current versions for " + tableId + " done");
	}
}
