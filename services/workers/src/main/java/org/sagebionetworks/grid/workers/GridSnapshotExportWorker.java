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
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

/**
 * Exports a snapshot of a session's internal replica for each request on the {@code GRID_SNAPSHOT_EXPORT.fifo}
 * queue. The message body is the internal replica's connection ID.
 */
@Service
public class GridSnapshotExportWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridSnapshotExportWorker.class);

	static final String SEMAPHORE_KEY_PREFIX = "gridSnapshotExport-";

	private final GridManager gridManager;
	private final GridReplicaSnapshotManager snapshotManager;
	private final WriteReadSemaphore semaphore;

	public GridSnapshotExportWorker(GridManager gridManager, GridReplicaSnapshotManager snapshotManager,
			WriteReadSemaphore semaphore) {
		this.gridManager = gridManager;
		this.snapshotManager = snapshotManager;
		this.semaphore = semaphore;
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
		String sessionId = connection.get().getSessionId();
		/*
		 * The queue delivers one request per session at a time, but redelivers a request whose visibility timeout
		 * lapses while its export still runs. The lock keeps that redelivery from starting a second export.
		 */
		try {
			semaphore.tryRunWithWriteLock(
					new WriteLockRequest(progressCallback, SEMAPHORE_KEY_PREFIX + connectionId, SEMAPHORE_KEY_PREFIX + sessionId),
					callback -> {
						snapshotManager.exportSnapshotIfNeeded(connection.get());
						return null;
					});
		} catch (LockUnavilableException e) {
			// The export that holds the lock covers this request.
			log.info("A snapshot export is already running for session {}, skipping: {}", sessionId, e.getMessage());
		} catch (Exception e) {
			log.error(String.format("Failed to export a snapshot for session: %s, connectionId: %s", sessionId,
					connectionId), e);
		}
	}

}
