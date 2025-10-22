package org.sagebionetworks.repo.manager.grid.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.springframework.stereotype.Service;

@Service
public class AgentGridEventResponsePublishHandler implements GridEventResponsePublishHandler {

	private static final Logger log = LogManager.getLogger(AgentGridEventResponsePublishHandler.class);

	@Override
	public EventSource getEventSource() {
		return EventSource.AGENT;
	}

	@Override
	public void publishEventResponse(EventContext context, String event) {
		log.info("log only context: {}, event: {}", context, event);
	}

}
