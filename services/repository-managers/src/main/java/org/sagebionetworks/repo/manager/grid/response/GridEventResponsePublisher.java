package org.sagebionetworks.repo.manager.grid.response;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.event.JsonRxMessageType;

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

}
