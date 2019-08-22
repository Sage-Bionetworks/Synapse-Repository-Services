package org.sagebionetworks.repo.manager.statistics.records;

import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;

/**
 * Factory used to get a {@link StatisticsEventLogRecordProvider} for different types of events.
 * 
 * @author Marco
 *
 */
public interface StatisticsLogRecordProviderFactory {

	/**
	 * @param <E>        The event type
	 * @param eventClass The event class
	 * @return A {@link StatisticsEventLogRecordProvider} for the given type of event
	 */
	<E extends StatisticsEvent> StatisticsEventLogRecordProvider<E> getLogRecordProvider(Class<E> eventClass);

}
