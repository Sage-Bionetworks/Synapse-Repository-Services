package org.sagebionetworks.repo.manager.statistics.project;

import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEventLogRecordProvider;

public class StatisticsFileEventLogRecordProviderStub extends StatisticsFileEventLogRecordProvider {

	private String streamName;

	public StatisticsFileEventLogRecordProviderStub(String streamName, Long projectId, LoggerProvider logProvider) {
		super((associationType, associationId) -> projectId);
		this.streamName = streamName;
	}

	@Override
	public String getStreamName(StatisticsFileEvent event) {
		return streamName;
	}

}
