package org.sagebionetworks.repo.manager.statistics.project;

import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.statistics.ProjectResolver;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.statistics.StatisticsFileEventLogRecordProvider;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;

public class StatisticsFileEventLogRecordProviderStub extends StatisticsFileEventLogRecordProvider {

	private String streamName;

	public StatisticsFileEventLogRecordProviderStub(String streamName, Long projectId, LoggerProvider logProvider) {
		super(new ProjectResolver() {

			@Override
			public Long resolveProject(FileHandleAssociateType associationType, String associationId)
					throws UnsupportedOperationException, NotFoundException, IllegalStateException {
				return projectId;
			}
		}, logProvider);
		this.streamName = streamName;
	}

	@Override
	public String getStreamName(StatisticsFileEvent event) {
		return streamName;
	}

}
