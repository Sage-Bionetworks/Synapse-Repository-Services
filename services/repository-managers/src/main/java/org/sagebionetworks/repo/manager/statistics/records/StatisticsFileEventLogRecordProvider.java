package org.sagebionetworks.repo.manager.statistics.records;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileActionType;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Implementation of a log record provider that deals with file actions
 * 
 * @author Marco
 *
 * @param <E>
 */
@Service
public class StatisticsFileEventLogRecordProvider implements StatisticsEventLogRecordProvider<StatisticsFileEvent> {
	
	private static final Logger LOG = LogManager.getLogger(StatisticsFileEventLogRecordProvider.class);

	private static final Set<FileHandleAssociateType> ACCEPTED = ImmutableSet.of(FileHandleAssociateType.FileEntity,
			FileHandleAssociateType.TableEntity);

	public static final Map<StatisticsFileActionType, String> ASSOCIATED_STREAMS = ImmutableMap.of(
			StatisticsFileActionType.FILE_DOWNLOAD, "fileDownloads", 
			StatisticsFileActionType.FILE_UPLOAD, "fileUploads"
		);
	
	private ProjectResolver projectResolver;
	
	@Autowired
	public StatisticsFileEventLogRecordProvider(ProjectResolver projectResolver) {
		this.projectResolver = projectResolver;
	}

	@Override
	public Class<StatisticsFileEvent> getEventClass() {
		return StatisticsFileEvent.class;
	}

	@Override
	public String getStreamName(StatisticsFileEvent event) {
		String streamName = ASSOCIATED_STREAMS.get(event.getActionType());
		if (streamName == null) {
			throw new UnsupportedOperationException("File event action of type " + event.getActionType() + " unsupported");
		}
		return streamName;
	}

	@Override
	public Optional<StatisticsEventLogRecord> getRecordForEvent(StatisticsFileEvent event) {
		ValidateArgument.required(event, "event");
		
		if (!ACCEPTED.contains(event.getAssociationType())) {
			return Optional.empty();
		}
		
		Long projectId;
		
		try {
			projectId = projectResolver.resolveProject(event.getAssociationType(), event.getAssociationId());
		} catch (NotFoundException | IllegalStateException e) {
			LOG.warn(e.getMessage(),  e);
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
