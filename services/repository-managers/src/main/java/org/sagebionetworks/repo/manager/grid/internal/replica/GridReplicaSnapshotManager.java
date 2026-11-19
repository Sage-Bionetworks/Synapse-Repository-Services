package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Component;

/**
 * Owns the snapshot lifecycle of a session's internal replica: decides when a snapshot is needed, queues the export
 * request, exports the snapshot once the replica has caught up, and deletes superseded snapshots.
 */
@Component
public class GridReplicaSnapshotManager {

	private static final Logger log = LogManager.getLogger(GridReplicaSnapshotManager.class);

	/**
	 * Minimum age of the latest snapshot before new patches alone justify another one. Bounds how many snapshots an
	 * active session accumulates.
	 */
	public static final long SNAPSHOT_INTERVAL_MS = 60_000L;

	/**
	 * Patches since the latest snapshot that trigger a new one regardless of its age. A joining client replays each
	 * patch in at most one round trip (about a second for a large patch), so this keeps replay under a minute even
	 * when an import writes patches faster than a client can replay them.
	 */
	public static final int PATCH_COUNT_SNAPSHOT_THRESHOLD = 50;

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
	private final GridSnapshotExportPublisher exportPublisher;
	private final ProdDetector prodDetector;

	public GridReplicaSnapshotManager(GridIndexManager gridIndexManager, SnapshotStore snapshotStore,
			FileProvider fileProvider, GridDao gridDao, Clock clock, GridSnapshotExportPublisher exportPublisher,
			ProdDetector prodDetector) {
		this.gridIndexManager = gridIndexManager;
		this.snapshotStore = snapshotStore;
		this.fileProvider = fileProvider;
		this.gridDao = gridDao;
		this.clock = clock;
		this.exportPublisher = exportPublisher;
		this.prodDetector = prodDetector;
	}

	/**
	 * Queue a snapshot export for the given session when it has an internal connection to export from and
	 * {@link #isSnapshotNeeded(String)}.
	 * <p>
	 * Called after a new patch is saved, so the export runs after the internal replica receives the patch and waits
	 * until it has applied every stored patch.
	 */
	public void requestSnapshotIfNeeded(String sessionId) {
		if (gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL).isPresent()
				&& isSnapshotNeeded(sessionId)) {
			exportPublisher.publishExportRequest(sessionId);
		}
	}

	/**
	 * Export the session's internal replica as the session's latest snapshot, unless the session has no internal
	 * connection or {@link #isSnapshotNeeded(String)} no longer holds because another export has saved a snapshot
	 * since the request was queued. The internal connection is resolved now rather than when the request was queued,
	 * so a reconnect in between does not drop the request. On the production stack, it then deletes the session's
	 * snapshots superseded more than {@link #SUPERSEDED_SNAPSHOT_RETENTION_MS} ago.
	 *
	 * @throws RecoverableMessageException if the replica has not yet applied every stored patch, so the request is
	 *                                     retried until it has and the snapshot includes the patches that triggered
	 *                                     it.
	 */
	public void exportSnapshotIfNeeded(String sessionId) throws RecoverableMessageException {
		Optional<GridConnectionInfo> connection = gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL);
		if (connection.isEmpty()) {
			log.info("Session {} has no internal connection. Skipping export.", sessionId);
			return;
		}
		Long replicaId = connection.get().getReplicaId();
		Long createdByUserId = connection.get().getCreatedBy();

		if (!isSnapshotNeeded(sessionId)) {
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

	/**
	 * A session needs a snapshot when patches have been stored since its latest snapshot and either that snapshot is
	 * at least {@link #SNAPSHOT_INTERVAL_MS} old or there are at least {@link #PATCH_COUNT_SNAPSHOT_THRESHOLD} of them.
	 * A session without a snapshot needs one as soon as it has a patch. Reads only the stored patches and snapshots,
	 * not the replica.
	 */
	boolean isSnapshotNeeded(String sessionId) {
		Optional<GridSnapshot> latestSnapshot = gridDao.getLatestSnapshot(sessionId);
		List<LogicalTimestamp> snapshotClock = latestSnapshot.map(snapshot -> snapshot.getClockTable().getClocks())
				.orElse(List.of());
		/*
		 * No patches since the latest snapshot also covers a request queued behind an export that has since saved
		 * a snapshot of the same patches.
		 */
		int patchesSinceSnapshot = gridDao.countMissingPatchesForClock(sessionId, snapshotClock);
		if (patchesSinceSnapshot == 0) {
			return false;
		}
		return latestSnapshot.isEmpty() || patchesSinceSnapshot >= PATCH_COUNT_SNAPSHOT_THRESHOLD
				|| isIntervalElapsedSince(latestSnapshot.get());
	}

	private boolean isIntervalElapsedSince(GridSnapshot snapshot) {
		return clock.currentTimeMillis() - snapshot.getCreatedOn().getTime() >= SNAPSHOT_INTERVAL_MS;
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
