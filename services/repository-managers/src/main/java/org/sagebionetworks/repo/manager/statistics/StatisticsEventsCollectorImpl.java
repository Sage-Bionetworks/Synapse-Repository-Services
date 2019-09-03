package org.sagebionetworks.repo.manager.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsEventLogRecord;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsEventLogRecordProvider;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsLogRecordProviderFactory;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;

@Service
public class StatisticsEventsCollectorImpl implements StatisticsEventsCollector, StatisticsEventsQueue {
	
	@FunctionalInterface
	private static interface Action {
		void proceed();
	}

	private AwsKinesisFirehoseLogger firehoseLogger;

	private StatisticsLogRecordProviderFactory logRecordProviderFactory;

	private TransactionSynchronizationProxy transactionSynchronization;

	private ConcurrentLinkedQueue<StatisticsEvent> queue = new ConcurrentLinkedQueue<StatisticsEvent>();

	@Autowired
	public StatisticsEventsCollectorImpl(AwsKinesisFirehoseLogger firehoseLogger,
			StatisticsLogRecordProviderFactory logRecordProviderFactory,
			TransactionSynchronizationProxy transationSynchronization) {
		this.firehoseLogger = firehoseLogger;
		this.logRecordProviderFactory = logRecordProviderFactory;
		this.transactionSynchronization = transationSynchronization;
	}

	@Override
	public <E extends StatisticsEvent> void collectEvent(final E event) {
		ValidateArgument.required(event, "event");

		afterCommit(() -> queue.add(event));
	}

	@Override
	public <E extends StatisticsEvent> void collectEvents(List<E> events) {
		ValidateArgument.required(events, "events");

		afterCommit(() -> queue.addAll(events));
	}

	@Override
	public void flush() {

		if (queue.isEmpty()) {
			return;
		}

		List<StatisticsEvent> eventsBatch = new LinkedList<>();

		for (StatisticsEvent event = queue.poll(); event != null; event = queue.poll()) {
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

	private <E extends StatisticsEvent> void log(List<E> events) {
		
		Map<String, List<StatisticsEventLogRecord>> recordsMap = getRecordsMap(events);

		recordsMap.forEach((streamName, records) -> {
			if (!records.isEmpty()) {
				firehoseLogger.logBatch(streamName, records);
			}
		});
	}

	private <E extends StatisticsEvent> Map<String, List<StatisticsEventLogRecord>> getRecordsMap(List<E> events) {

		if (events.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<StatisticsEventLogRecord>> recordsMap = new HashMap<>();

		for (E event : events) {

			StatisticsEventLogRecordProvider<E> provider = getRecordProvider(event);

			String streamName = provider.getStreamName(event);

			List<StatisticsEventLogRecord> records = recordsMap.get(streamName);

			if (records == null) {
				recordsMap.put(streamName, records = new ArrayList<>());
			}

			provider.getRecordForEvent(event).ifPresent(records::add);
		}

		return recordsMap;
	}

	@SuppressWarnings("unchecked")
	private <E extends StatisticsEvent> StatisticsEventLogRecordProvider<E> getRecordProvider(E event) {
		Class<E> eventClass = (Class<E>) event.getClass();
		return logRecordProviderFactory.getLogRecordProvider(eventClass);
	}

}
