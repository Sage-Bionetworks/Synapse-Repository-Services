package org.sagebionetworks.repo.manager.statistics.records;

import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;

/**
 * Provider that is used to translate a {@link StatisticsEvent} into a {@link StatisticsEventLogRecord} to be sent out
 * to a AWS firehose stream.
 * 
 * @author Marco
 *
 */
public interface StatisticsEventLogRecordProvider<E extends StatisticsEvent> {

	Class<E> getEventClass();

	/**
	 * @param event The input event
	 * @return The name of the firehose stream for the given event
	 */
	String getStreamName(E event);

	/**
	 * @param event The input event
	 * @return True if the given event should be sent to the stream
	 */
	boolean sendToStream(E event);

	/**
	 * @param event The input event
	 * @return A {@link StatisticsEventLogRecord} that represents the given input event
	 */
	StatisticsEventLogRecord getRecordForEvent(E event);

}
