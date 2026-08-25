package org.sagebionetworks.repo.manager.grid.response;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.springframework.stereotype.Service;

/**
 * No-op publish handler for CSV import connections. Import jobs poll for
 * results via async jobs and do not receive real-time push notifications, so
 * no action is needed when the hub broadcasts patch events to import-source
 * connections.
 */
@Service
public class ImportGridEventResponsePublishHandler implements GridEventResponsePublishHandler {

	@Override
	public EventSource getEventSource() {
		return EventSource.IMPORT;
	}

	@Override
	public void publishEventResponse(EventContext context, String event) {
		// Import jobs poll for results — no real-time push needed.
	}

}
