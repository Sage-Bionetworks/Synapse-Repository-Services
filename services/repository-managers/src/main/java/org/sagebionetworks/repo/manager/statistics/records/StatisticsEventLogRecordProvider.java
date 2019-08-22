package org.sagebionetworks.repo.manager.statistics.records;

import java.util.Optional;

import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;

/**
 * Provider that is used to translate a {@link StatisticsEvent} into a {@link StatisticsEventLogRecord} to be sent out
 * to a AWS firehose stream.
 * 
 * @author Marco
 *
 */
public interface StatisticsEventLogRecordProvider<E extends StatisticsEvent> {

	/**
	 * @return The type of event
	 */
	Class<E> getEventClass();

	/**
	 * @param event The input event
	 * @return The name of the firehose stream for the given event
	 */
	String getStreamName(E event);

	/**
	 * @param event The input event
	 * @return An optional containing a {@link StatisticsEventLogRecord} representing the given event if the event can
	 *         be sent to the stream, an empty optional otherwise
	 */
	Optional<StatisticsEventLogRecord> getRecordForEvent(E event);

}
