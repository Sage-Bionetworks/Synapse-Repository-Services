package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.manager.statistics.StatisticsEventsCollector;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Utility to test the statistics collection with and without transactions
 * 
 * @author Marco
 */
@Service
public class StatisticsEventsCollectorClient {

	private StatisticsEventsCollector eventsCollector;

	@Autowired
	public void setEventsCollector(StatisticsEventsCollector eventsCollector) {
		this.eventsCollector = eventsCollector;
	}

	public void collectEventWithoutTransaction(StatisticsFileEvent event) {
		eventsCollector.collectEvent(event);
	}

	@WriteTransaction
	public void collectEventWithTransaction(StatisticsFileEvent event) {
		eventsCollector.collectEvent(event);
	}

}
