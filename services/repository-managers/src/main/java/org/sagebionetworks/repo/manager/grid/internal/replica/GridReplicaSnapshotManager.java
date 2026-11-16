package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Component;

@Component
public class GridReplicaSnapshotManager {

	private static final Logger log = LogManager.getLogger(GridReplicaSnapshotManager.class);

	/**
	 * How long a superseded snapshot is kept. It must outlast a presigned URL handed out for it while it was the
	 * latest (15 minutes), and the staging stack, which shares the snapshot bucket and serves the latest snapshot it
	 * has migrated, so must outlast the time between migrations (rarely more than 2-3 days).
	 */
	public static final long SUPERSEDED_SNAPSHOT_RETENTION_MS = Duration.ofDays(7).toMillis();

	private final GridIndexManager gridIndexManager;
	private final SnapshotStore snapshotStore;
	private final FileProvider fileProvider;
	private final GridDao gridDao;
	private final Clock clock;
	private final GridSnapshotRequester snapshotRequester;
	private final ProdDetector prodDetector;

	public GridReplicaSnapshotManager(GridIndexManager gridIndexManager, SnapshotStore snapshotStore,
			FileProvider fileProvider, GridDao gridDao, Clock clock, GridSnapshotRequester snapshotRequester,
			ProdDetector prodDetector) {
		this.gridIndexManager = gridIndexManager;
		this.snapshotStore = snapshotStore;
		this.fileProvider = fileProvider;
		this.gridDao = gridDao;
		this.clock = clock;
		this.snapshotRequester = snapshotRequester;
		this.prodDetector = prodDetector;
	}

	/**
	 * Export the replica's current state as the session's latest snapshot, unless
	 * {@link GridSnapshotRequester#isSnapshotNeeded(String)} no longer holds because another export has saved a
	 * snapshot since the request was queued. On the production stack, it then deletes the session's snapshots
	 * superseded more than {@link #SUPERSEDED_SNAPSHOT_RETENTION_MS} ago.
	 *
	 * @throws RecoverableMessageException if the replica has not yet applied every stored patch, so the request is
	 *                                     retried until it has and the snapshot includes the patches that triggered
	 *                                     it.
	 */
	public void exportSnapshotIfNeeded(GridConnectionInfo connection) throws RecoverableMessageException {
		String sessionId = connection.getSessionId();
		Long replicaId = connection.getReplicaId();
		Long createdByUserId = connection.getCreatedBy();

		if (!snapshotRequester.isSnapshotNeeded(sessionId)) {
			log.info("Session {} does not need a new snapshot. Skipping export.", sessionId);
			return;
		}
		// The request is queued when a patch is saved, usually before the replica has applied it.
		if (gridDao.countMissingPatchesForClock(sessionId, gridIndexManager.getClock(sessionId, replicaId)) > 0) {
			throw new RecoverableMessageException(
					"The replica of session " + sessionId + " has not applied every patch yet.");
		}

		File tempFile = null;
		try {
			tempFile = fileProvider.createTempFile("grid-snapshot-", ".cbor");
			ClockTable clockTable = gridIndexManager.exportSnapshot(sessionId, replicaId, tempFile.toPath());
			snapshotStore.saveSnapshot(sessionId, clockTable, createdByUserId, tempFile);
			log.info("Successfully exported snapshot for session {} with {} clock entries.", sessionId,
					clockTable.getClocks().size());
		} catch (IOException e) {
			throw new RuntimeException("Failed to create temp file for snapshot export", e);
		} finally {
			if (tempFile != null && !tempFile.delete()) {
				log.warn("Failed to delete temp file: {}", tempFile);
			}
		}
		deleteSupersededSnapshots(sessionId);
	}

	private void deleteSupersededSnapshots(String sessionId) {
		/*
		 * Production and staging share the snapshot bucket, so only production deletes; staging deleting a file could
		 * remove production's latest snapshot. If the stack cannot be detected, the next export retries.
		 */
		if (!prodDetector.isProductionStack().orElse(false)) {
			return;
		}
		Instant supersededBy = Instant.ofEpochMilli(clock.currentTimeMillis() - SUPERSEDED_SNAPSHOT_RETENTION_MS);
		try {
			int deleted = snapshotStore.deleteSupersededSnapshots(sessionId, supersededBy);
			log.info("Deleted {} superseded snapshots of session {}.", deleted, sessionId);
		} catch (RuntimeException e) {
			// The new snapshot is saved; the next export retries the deletion.
			log.warn("Failed to delete superseded snapshots of session " + sessionId, e);
		}
	}
}
