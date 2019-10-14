package org.sagebionetworks.repo.manager.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EventsCollectorAutowireTest {

	@Autowired
	private EventLogRecordProviderFactory logRecordProviderFactory;

	@Autowired
	private EventsCollectorClient collectorClient;

	@Autowired
	private TransactionSynchronizationProxy transactionSynchronization;

	@Mock
	private AwsKinesisFirehoseLogger firehoseLogger;

	private String streamName = "someStreamName";
	private String otherStreamName = "anotherStreamName";

	private EventsCollectorImpl statsEventsCollector;

	@BeforeEach
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		// Spies on the transaction synchronization so that we can verify calls on it
		transactionSynchronization = Mockito.spy(transactionSynchronization);
		// We mock the firehose logger
		statsEventsCollector = new EventsCollectorImpl(firehoseLogger, logRecordProviderFactory, transactionSynchronization);
		// Replace the autowired collector with ours so that we do not use firehose
		collectorClient.setEventsCollector(statsEventsCollector);
	}

	@Test
	public void testCollectEventWithNullEvent() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			statsEventsCollector.collectEvent(null);
		});
	}

	@Test
	public void testCollectEvent() {
		EventStub event = new EventStub(streamName);

		// Call under test
		statsEventsCollector.collectEvent(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(firehoseLogger, times(1)).logBatch(eq(event.getStreamName()), eq(Collections.singletonList(event)));

	}

	@Test
	public void testCollectEvents() {
		EventStub event1 = new EventStub(streamName);
		EventStub event2 = new EventStub(streamName);

		List<EventStub> events = ImmutableList.of(event1, event2);

		// Call under test
		statsEventsCollector.collectEvents(events);
		// Simulates the background timer call
		statsEventsCollector.flush();

		// Verifies that the logger is invoked only once
		verify(firehoseLogger, times(1)).logBatch(eq(event1.getStreamName()), eq(events));
	}

	@Test
	public void testCollectEventsWithDifferentStreams() {

		EventStub event1 = new EventStub(streamName);
		EventStub event2 = new EventStub(otherStreamName);

		List<EventStub> events = ImmutableList.of(event1, event2);

		List<EventStub> expectedRecords1 = ImmutableList.of(event1);

		List<EventStub> expectedRecords2 = ImmutableList.of(event2);

		// Call under test
		statsEventsCollector.collectEvents(events);
		// Simulates the background timer call
		statsEventsCollector.flush();

		// Verifies that the logger is invoked once per stream type
		verify(firehoseLogger, times(1)).logBatch(eq(event1.getStreamName()), eq(expectedRecords1));
		verify(firehoseLogger, times(1)).logBatch(eq(event2.getStreamName()), eq(expectedRecords2));
	}

	@Test
	public void testInvokationWithoutTransaction() {
		EventStub event = new EventStub(streamName);

		// Call under test
		collectorClient.collectEventWithoutTransaction(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(transactionSynchronization, times(1)).isActualTransactionActive();
		verify(transactionSynchronization, never()).registerSynchronization(any());
		verify(firehoseLogger, times(1)).logBatch(any(), any());

	}

	@Test
	public void testInvokationWithTransaction() {
		EventStub event = new EventStub(streamName);

		// Call under test
		collectorClient.collectEventWithTransaction(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(transactionSynchronization, times(1)).isActualTransactionActive();
		verify(transactionSynchronization, times(1)).registerSynchronization(any());
		verify(firehoseLogger, times(1)).logBatch(any(), any());

	}
}
