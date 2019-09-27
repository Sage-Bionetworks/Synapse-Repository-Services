package org.sagebionetworks.repo.manager.events;

import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Utility to test the event collection with and without transactions
 * 
 * @author Marco
 */
@Service
public class EventsCollectorClient {

	private EventsCollector eventsCollector;

	@Autowired
	public void setEventsCollector(EventsCollector eventsCollector) {
		this.eventsCollector = eventsCollector;
	}

	public void collectEventWithoutTransaction(SynapseEvent event) {
		eventsCollector.collectEvent(event);
	}

	@WriteTransaction
	public void collectEventWithTransaction(SynapseEvent event) {
		eventsCollector.collectEvent(event);
	}

}
