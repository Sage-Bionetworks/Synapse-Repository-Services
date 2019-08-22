package org.sagebionetworks.repo.manager.statistics;

import java.util.List;

import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;

/**
 * Main entry point to collect statistics about {@link StatisticsEvent}s.
 * 
 * @author Marco
 *
 */
public interface StatisticsEventsCollector {

	/**
	 * Accepts the given {@link StatisticsEvent} in order to collect statistics about the event
	 * 
	 * @param event The event to collect statistics about
	 */
	<E extends StatisticsEvent> void collectEvent(E event);
	
	/**
	 * Accepts the given list of {@link StatisticsEvent}s in order to collect statistics about the batch of events
	 * 
	 * @param events A batch of events to collect statistics about
	 */
	<E extends StatisticsEvent> void collectEvents(List<E> events);

}
