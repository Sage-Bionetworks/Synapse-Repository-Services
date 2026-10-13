package org.sagebionetworks.repo.manager.grid;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridSnapshotExportRequester {

	private final GridDao gridDao;
	private final GridIndexManager gridIndexManager;
	private final GridEventResponsePublisher gridEventResponsePublisher;

	public GridSnapshotExportRequester(GridDao gridDao, GridIndexManager gridIndexManager,
			GridEventResponsePublisher gridEventResponsePublisher) {
		this.gridDao = gridDao;
		this.gridIndexManager = gridIndexManager;
		this.gridEventResponsePublisher = gridEventResponsePublisher;
	}

	/**
	 * Ask the session's internal replica to export a snapshot when it has applied enough patches since the latest
	 * snapshot.
	 */
	public void requestSnapshotExportIfNeeded(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		Optional<GridConnectionInfo> internal = gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL);
		if (internal.isEmpty()) {
			return;
		}
		List<LogicalTimestamp> snapshotClock = gridDao.getLatestSnapshot(sessionId)
				.map(snapshot -> snapshot.getClockTable().getClocks())
				.orElse(List.of());
		List<LogicalTimestamp> replicaClock = gridIndexManager.getClock(sessionId, internal.get().getReplicaId());
		if (!GridReplicaSnapshotManager.isSnapshotNeeded(gridDao, sessionId, snapshotClock, replicaClock,
				GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD)) {
			return;
		}
		gridEventResponsePublisher.publishEventResponse(
				new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internal.get().getConnectionId()),
				JsonRxMessageType.Notification, "new-snapshot");
	}
}
