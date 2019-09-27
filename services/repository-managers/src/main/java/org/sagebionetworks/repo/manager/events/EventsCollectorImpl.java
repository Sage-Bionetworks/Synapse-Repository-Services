package org.sagebionetworks.repo.manager.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;

@Service
public class EventsCollectorImpl implements EventsCollector, EventsQueue {

	@FunctionalInterface
	private static interface Action {
		void proceed();
	}

	private AwsKinesisFirehoseLogger firehoseLogger;

	private EventLogRecordProviderFactory logRecordProviderFactory;

	private TransactionSynchronizationProxy transactionSynchronization;

	private ConcurrentLinkedQueue<SynapseEvent> queue = new ConcurrentLinkedQueue<SynapseEvent>();

	@Autowired
	public EventsCollectorImpl(AwsKinesisFirehoseLogger firehoseLogger,
			EventLogRecordProviderFactory logRecordProviderFactory, TransactionSynchronizationProxy transactionSynchronization) {
		this.firehoseLogger = firehoseLogger;
		this.logRecordProviderFactory = logRecordProviderFactory;
		this.transactionSynchronization = transactionSynchronization;
	}

	@Override
	public <E extends SynapseEvent> void collectEvent(final E event) {
		ValidateArgument.required(event, "event");

		afterCommit(() -> queue.add(event));
	}

	@Override
	public <E extends SynapseEvent> void collectEvents(List<E> events) {
		ValidateArgument.required(events, "events");

		afterCommit(() -> queue.addAll(events));
	}

	@Override
	public void flush() {

		if (queue.isEmpty()) {
			return;
		}

		List<SynapseEvent> eventsBatch = new LinkedList<>();

		for (SynapseEvent event = queue.poll(); event != null; event = queue.poll()) {
			eventsBatch.add(event);
		}

		log(eventsBatch);

	}

	private void afterCommit(Action action) {

		if (transactionSynchronization.isActualTransactionActive()) {
			transactionSynchronization.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.proceed();
				}
			});
		} else {
			action.proceed();
		}
	}

	private <E extends SynapseEvent> void log(List<E> events) {

		Map<String, List<AwsKinesisLogRecord>> recordsMap = getRecordsMap(events);

		recordsMap.forEach((streamName, records) -> {
			if (!records.isEmpty()) {
				firehoseLogger.logBatch(streamName, records);
			}
		});
	}

	private <E extends SynapseEvent> Map<String, List<AwsKinesisLogRecord>> getRecordsMap(List<E> events) {

		if (events.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<AwsKinesisLogRecord>> recordsMap = new HashMap<>();

		for (E event : events) {

			EventLogRecordProvider<E> provider = getRecordProvider(event);

			String streamName = provider.getStreamName(event);

			List<AwsKinesisLogRecord> records = recordsMap.get(streamName);

			if (records == null) {
				recordsMap.put(streamName, records = new ArrayList<>());
			}

			provider.getRecordForEvent(event).ifPresent(records::add);
		}

		return recordsMap;
	}

	@SuppressWarnings("unchecked")
	private <E extends SynapseEvent> EventLogRecordProvider<E> getRecordProvider(E event) {
		Class<E> eventClass = (Class<E>) event.getClass();
		return logRecordProviderFactory.getLogRecordProvider(eventClass);
	}

}
