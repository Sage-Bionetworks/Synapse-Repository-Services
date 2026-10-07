package org.sagebionetworks.repo.manager.grid;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridSnapshotExportRequester {

	private final GridDao gridDao;
	private final GridEventResponsePublisher gridEventResponsePublisher;

	public GridSnapshotExportRequester(GridDao gridDao, GridEventResponsePublisher gridEventResponsePublisher) {
		this.gridDao = gridDao;
		this.gridEventResponsePublisher = gridEventResponsePublisher;
	}

	public void requestSnapshotExportIfNeeded(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		List<LogicalTimestamp> snapshotClock = gridDao.getLatestSnapshot(sessionId)
				.map(snapshot -> snapshot.getClockTable().getClocks())
				.orElse(List.of());
		if (!GridReplicaSnapshotManager.isSnapshotNeeded(gridDao, sessionId, snapshotClock,
				GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD)) {
			return;
		}
		gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL).ifPresent(internal ->
				gridEventResponsePublisher.publishEventResponse(
						new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internal.getConnectionId()),
						JsonRxMessageType.Notification, "new-snapshot"));
	}
}
