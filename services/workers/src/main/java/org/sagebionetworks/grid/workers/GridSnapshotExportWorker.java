package org.sagebionetworks.grid.workers;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

/**
 * Exports a snapshot of a session's internal replica for each request on the {@code GRID_SNAPSHOT_EXPORT.fifo}
 * queue. The message body is the internal replica's connection ID. Requests are grouped by session, so a session's
 * exports never overlap.
 */
@Service
public class GridSnapshotExportWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridSnapshotExportWorker.class);

	private final GridManager gridManager;
	private final GridReplicaSnapshotManager snapshotManager;

	public GridSnapshotExportWorker(GridManager gridManager, GridReplicaSnapshotManager snapshotManager) {
		this.gridManager = gridManager;
		this.snapshotManager = snapshotManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) {
		ValidateArgument.required(message, "message");
		String connectionId = message.getBody();
		Optional<GridConnectionInfo> connection = gridManager.getConnectionInfoOptional(connectionId);
		if (connection.isEmpty()) {
			log.info("No active connection found for connectionId: {}, the snapshot export will be skipped.",
					connectionId);
			return;
		}
		try {
			snapshotManager.exportSnapshotIfNeeded(connection.get());
		} catch (Exception e) {
			log.error(String.format("Failed to export a snapshot for session: %s, connectionId: %s",
					connection.get().getSessionId(), connectionId), e);
		}
	}

}
