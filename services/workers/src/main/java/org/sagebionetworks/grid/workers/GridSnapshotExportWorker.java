package org.sagebionetworks.grid.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

/**
 * Exports a snapshot of a session's internal replica for each request on the {@code GRID_SNAPSHOT_EXPORT.fifo}
 * queue. The message body is the session ID. Requests are grouped by session, so a session's exports never overlap.
 * While the replica has not applied every stored patch, the request goes back on the queue and is retried; the
 * queue's redrive policy caps the retries.
 */
@Service
public class GridSnapshotExportWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridSnapshotExportWorker.class);

	private final GridReplicaSnapshotManager snapshotManager;

	public GridSnapshotExportWorker(GridReplicaSnapshotManager snapshotManager) {
		this.snapshotManager = snapshotManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException {
		ValidateArgument.required(message, "message");
		String sessionId = message.getBody();
		try {
			snapshotManager.exportSnapshotIfNeeded(sessionId);
		} catch (RecoverableMessageException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to export a snapshot for session: " + sessionId, e);
		}
	}

}
