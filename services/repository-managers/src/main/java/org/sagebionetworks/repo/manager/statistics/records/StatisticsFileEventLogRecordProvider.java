package org.sagebionetworks.repo.manager.statistics.records;

import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileActionType;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
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

	private static final Set<FileHandleAssociateType> ACCEPTED = ImmutableSet.of(FileHandleAssociateType.FileEntity,
			FileHandleAssociateType.TableEntity);

	public static final Map<StatisticsFileActionType, String> ASSOCIATED_STREAMS = ImmutableMap.of(
			StatisticsFileActionType.FILE_DOWNLOAD, "fileDownloads", 
			StatisticsFileActionType.FILE_UPLOAD, "fileUploads"
		);

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
	public boolean sendToStream(StatisticsFileEvent event) {
		return ACCEPTED.contains(event.getAssociationType());
	}

	@Override
	public StatisticsEventLogRecord getRecordForEvent(StatisticsFileEvent event) {

		return new StatisticsFileEventLogRecord()
				.withTimestamp(event.getTimestamp())
				.withUserId(event.getUserId())
				.withFileHandleId(event.getFileHandleId())
				.withAssociation(event.getAssociationType(), event.getAssociationId())
				.withProjectId(getProjectId(event));
	}

	private Long getProjectId(StatisticsFileEvent event) {
		// TODO
		return null;
	}

}
