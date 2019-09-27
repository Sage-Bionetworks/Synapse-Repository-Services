package org.sagebionetworks.repo.manager.statistics;

import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.manager.events.EventLogRecordProvider;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

/**
 * Implementation of a log record provider that deals with file actions
 * 
 * @author Marco
 *
 * @param <E>
 */
@Service
public class StatisticsFileEventLogRecordProvider implements EventLogRecordProvider<StatisticsFileEvent> {

	private static final Set<FileHandleAssociateType> ACCEPTED = ImmutableSet.of(
		FileHandleAssociateType.FileEntity,
		FileHandleAssociateType.TableEntity
	);
	
	private ProjectResolver projectResolver;
	private Logger log;
	
	@Autowired
	public StatisticsFileEventLogRecordProvider(ProjectResolver projectResolver, LoggerProvider logProvider) {
		this.projectResolver = projectResolver;
		this.log = logProvider.getLogger(StatisticsFileEventLogRecordProvider.class.getName());
	}

	@Override
	public Class<StatisticsFileEvent> getEventClass() {
		return StatisticsFileEvent.class;
	}

	@Override
	public String getStreamName(StatisticsFileEvent event) {
		return event.getActionType().getFirehoseStreamName();
	}

	@Override
	public Optional<AwsKinesisLogRecord> getRecordForEvent(StatisticsFileEvent event) {
		ValidateArgument.required(event, "event");
		
		if (!ACCEPTED.contains(event.getAssociationType())) {
			return Optional.empty();
		}
		
		Long projectId;
		
		try {
			projectId = projectResolver.resolveProject(event.getAssociationType(), event.getAssociationId());
		} catch (NotFoundException | IllegalStateException e) {
			log.warn(e.getMessage(), e);
			return Optional.empty();
		}
		
		return Optional.of(new StatisticsFileEventLogRecord()
				.withTimestamp(event.getTimestamp())
				.withUserId(event.getUserId())
				.withFileHandleId(event.getFileHandleId())
				.withAssociation(event.getAssociationType(), event.getAssociationId())
				.withProjectId(projectId));
	}

}
