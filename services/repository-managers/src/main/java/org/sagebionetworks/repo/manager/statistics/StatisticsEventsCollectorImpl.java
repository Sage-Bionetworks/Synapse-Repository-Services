package org.sagebionetworks.repo.manager.statistics;

import java.util.Collections;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsEvent;
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

		StatisticsEventLogRecordProvider<E> provider = logRecordProviderFactory.getLogRecordProvider(event);

		provider.getRecordForEvent(event).ifPresent(record -> {
			String streamName = provider.getStreamName(event);
			firehoseLogger.logBatch(streamName, Collections.singletonList(record));
		});

	}

}
