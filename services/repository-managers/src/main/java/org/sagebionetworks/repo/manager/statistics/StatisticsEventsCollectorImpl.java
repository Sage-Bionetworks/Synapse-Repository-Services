package org.sagebionetworks.repo.manager.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsEventLogRecord;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsEventLogRecordProvider;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsLogRecordProviderFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsEventsCollectorImpl implements StatisticsEventsCollector {

	private AwsKinesisFirehoseLogger firehoseLogger;

	private StatisticsLogRecordProviderFactory logRecordProviderFactory;

	@Autowired
	public StatisticsEventsCollectorImpl(AwsKinesisFirehoseLogger firehoseLogger,
			StatisticsLogRecordProviderFactory logRecordProviderFactory) {
		this.firehoseLogger = firehoseLogger;
		this.logRecordProviderFactory = logRecordProviderFactory;
	}

	@Override
	public <E extends StatisticsEvent> void collectEvent(E event) {
		ValidateArgument.required(event, "event");

		StatisticsEventLogRecordProvider<E> provider = getRecordProvider(event);

		String streamName = provider.getStreamName(event);

		provider.getRecordForEvent(event).ifPresent(record -> {
			firehoseLogger.logBatch(streamName, Collections.singletonList(record));
		});
	}

	@Override
	public <E extends StatisticsEvent> void collectEvents(List<E> events) {
		ValidateArgument.required(events, "events");

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
