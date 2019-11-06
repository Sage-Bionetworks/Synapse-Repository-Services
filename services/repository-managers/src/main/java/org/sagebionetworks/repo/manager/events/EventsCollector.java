package org.sagebionetworks.repo.manager.events;

import java.util.List;

/**
 * Main entry point to stream {@link SynapseEvent}s to a firehose stream.
 * 
 * @author Marco
 *
 */
public interface EventsCollector {

	/**
	 * Accepts the given {@link SynapseEvent} in order to stream it to firehose. If this call is invoked
	 * within a transaction if will be called only after the transaction is committed.
	 * 
	 * @param event The event to stream
	 */
	<E extends SynapseEvent> void collectEvent(E event);

	/**
	 * Accepts the given list of {@link SynapseEvent}s in order to stream them to firehose. If
	 * this call is invoked within a transaction if will be called only after the transaction is committed.
	 * 
	 * @param events A batch of events to stream
	 */
	<E extends SynapseEvent> void collectEvents(List<E> events);

}
