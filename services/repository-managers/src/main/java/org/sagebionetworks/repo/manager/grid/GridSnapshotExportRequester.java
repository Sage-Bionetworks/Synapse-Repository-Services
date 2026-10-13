package org.sagebionetworks.repo.manager.grid;

import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridSnapshotExportRequester {

	private final GridReplicaSnapshotManager snapshotManager;
	private final GridEventResponsePublisher gridEventResponsePublisher;

	public GridSnapshotExportRequester(GridReplicaSnapshotManager snapshotManager,
			GridEventResponsePublisher gridEventResponsePublisher) {
		this.snapshotManager = snapshotManager;
		this.gridEventResponsePublisher = gridEventResponsePublisher;
	}

	/**
	 * Ask the session's internal replica to export a snapshot when it has applied enough patches since the latest
	 * snapshot.
	 *
	 * @param internal The session's INTERNAL replica connection.
	 */
	public void requestSnapshotExportIfNeeded(GridConnectionInfo internal) {
		ValidateArgument.required(internal, "internal");
		ValidateArgument.required(internal.getSessionId(), "internal.sessionId");
		ValidateArgument.required(internal.getReplicaId(), "internal.replicaId");
		if (!snapshotManager.isSnapshotNeeded(internal.getSessionId(), internal.getReplicaId())) {
			return;
		}
		gridEventResponsePublisher.publishEventResponse(
				new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internal.getConnectionId()),
				JsonRxMessageType.Notification, "new-snapshot");
	}
}
