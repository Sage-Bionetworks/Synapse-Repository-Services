package org.sagebionetworks.repo.manager.grid.response;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.schema.adapter.JSONEntity;

/**
 * This handler is used by internal replicas to send messages to the "hub".
 */
public interface InternalReplicaToHubEventPublisher {

	/**
	 * Publish an internal event after the current transaction commits.
	 * 
	 * @param context
	 * @param messageJson
	 */
	void publishEventAfterCommit(EventContext context, String messageJson);

	void publishEventAfterCommit(EventContext context, JsonRxMessage message);

	/**
	 * Publish an internal event event (without a transaction).
	 * 
	 * @param context
	 * @param message
	 */
	void publishEvent(EventContext context, JsonRxMessage message);

	/**
	 * Publish an internal event after the current transaction commits.
	 * 
	 * @param <T>
	 * @param context
	 * @param type
	 * @param payload
	 */
	<T extends JSONEntity> void publishEventAfterCommit(EventContext context, JsonRxMessageType type, String method,
			T payload);
}
