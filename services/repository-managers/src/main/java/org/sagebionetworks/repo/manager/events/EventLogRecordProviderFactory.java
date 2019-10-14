package org.sagebionetworks.repo.manager.events;

/**
 * Factory used to get a {@link EventLogRecordProvider} for different types of events.
 * 
 * @author Marco
 *
 */
public interface EventLogRecordProviderFactory {

	/**
	 * @param <E>        The event type
	 * @param eventClass The event class
	 * @return A {@link EventLogRecordProvider} for the given type of event
	 */
	<E extends SynapseEvent> EventLogRecordProvider<E> getLogRecordProvider(Class<E> eventClass);

}
