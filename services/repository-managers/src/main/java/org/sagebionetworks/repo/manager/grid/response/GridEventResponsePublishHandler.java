package org.sagebionetworks.repo.manager.grid.response;

import java.util.List;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;

public interface GridEventResponsePublishHandler {
	
	/**
	 * The source to handle.
	 * @return
	 */
	EventSource getEventSource();
	
	/**
	 * Publish an event.
	 * @param context
	 * @param event
	 * @return
	 */
	void publishEventResponse(EventContext context, String event);

	/**
	 * Publish the same event to multiple contexts. The default implementation
	 * iterates and calls the single-message method. Implementations may override
	 * to provide parallel execution.
	 * 
	 * @param contexts List of event contexts to publish to
	 * @param event The event message to publish
	 */
	default void publishEventResponses(List<EventContext> contexts, String event) {
		for (EventContext context : contexts) {
			publishEventResponse(context, event);
		}
	}

}
