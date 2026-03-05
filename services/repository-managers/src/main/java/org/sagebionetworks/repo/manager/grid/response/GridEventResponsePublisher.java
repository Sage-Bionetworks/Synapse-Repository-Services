package org.sagebionetworks.repo.manager.grid.response;

import java.util.List;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;

public interface GridEventResponsePublisher {

	/**
	 * Publish an event.
	 * 
	 * @param context
	 * @param event
	 */
	void publishEventResponse(EventContext context, String event);

	/**
	 * Publish a new message response.
	 * 
	 * @param context
	 * @param type
	 * @param method
	 * @param body
	 */
	void publishEventResponse(EventContext context, JsonRxMessageType type, String method);
	
	/**
	 * Publish a message.
	 * @param context
	 * @param type
	 * @param requestId
	 */
	void publishEventResponse(EventContext context, JsonRxMessageType type, int requestId);
	
	
	/**
	 * Publish a message.
	 * @param context
	 * @param type
	 * @param requestId
	 * @param payload This is expected to be the raw payload (not method)
	 */
	void publishEventResponse(EventContext context, JsonRxMessageType type, int requestId, String payload);

	/**
	 * Publish the same notification message to multiple contexts. This enables
	 * parallel delivery when broadcasting to multiple connections.
	 * 
	 * @param contexts List of event contexts to publish to
	 * @param type The message type
	 * @param method The notification method name
	 */
	void publishEventResponses(List<EventContext> contexts, JsonRxMessageType type, String method);

}
