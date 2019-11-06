package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.message.ChangeMessage;

public interface ProcessedMessageDAO {

	/**
	 * Register a message that has been processed by a worker.
	 */
	public void registerMessageProcessed(long changeNumber, String queueName);

	/**
	 * List messages that have been processed. This is used to detect messages that have been sent
	 * but not processed by a worker.
	 * @return
	 */
	public List<ChangeMessage> listNotProcessedMessages(String queueName, long limit);

}
