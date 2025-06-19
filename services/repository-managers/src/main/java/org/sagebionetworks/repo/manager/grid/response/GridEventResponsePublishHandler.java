package org.sagebionetworks.repo.manager.grid.response;

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

}
