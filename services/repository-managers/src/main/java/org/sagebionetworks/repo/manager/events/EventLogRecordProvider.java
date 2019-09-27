package org.sagebionetworks.repo.manager.events;

import java.util.Optional;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.manager.statistics.StatisticsEventLogRecord;

/**
 * Provider that is used to translate a {@link SynapseEvent} into a {@link AwsKinesisLogRecord} to be sent out
 * to a AWS firehose stream.
 * 
 * @author Marco
 *
 */
public interface EventLogRecordProvider<E extends SynapseEvent> {

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
	Optional<AwsKinesisLogRecord> getRecordForEvent(E event);

}
