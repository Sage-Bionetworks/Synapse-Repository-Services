package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.FileProvider;
import org.springframework.stereotype.Component;

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

	private final GridIndexManager gridIndexManager;
	private final SnapshotStore snapshotStore;
	private final FileProvider fileProvider;
	private final GridDao gridDao;
	private final Clock clock;
	private final GridSnapshotExportPublisher exportPublisher;

	public GridReplicaSnapshotManager(GridIndexManager gridIndexManager, SnapshotStore snapshotStore,
			FileProvider fileProvider, GridDao gridDao, Clock clock, GridSnapshotExportPublisher exportPublisher) {
		this.gridIndexManager = gridIndexManager;
		this.snapshotStore = snapshotStore;
		this.fileProvider = fileProvider;
		this.gridDao = gridDao;
		this.clock = clock;
		this.exportPublisher = exportPublisher;
	}

	/**
	 * Queue a snapshot export for the replica when patches have been stored since the session's latest snapshot and
	 * either that snapshot is at least {@link #SNAPSHOT_INTERVAL_MS} old or there are at least
	 * {@link #PATCH_COUNT_SNAPSHOT_THRESHOLD} of them. A session without a snapshot gets one as soon as it has a
	 * patch.
	 * <p>
	 * Call when the replica has applied every stored patch, so the export, which runs later, includes all the patches
	 * measured here.
	 */
	public void requestSnapshotIfNeeded(GridConnectionInfo connection) {
		if (isSnapshotNeeded(connection.getSessionId())) {
			exportPublisher.publishExportRequest(connection);
		}
	}

	/**
	 * Export the replica's current state as the session's latest snapshot, unless the conditions of
	 * {@link #requestSnapshotIfNeeded(GridConnectionInfo)} no longer hold because another export has saved a snapshot
	 * since the request was queued.
	 */
	public void exportSnapshotIfNeeded(GridConnectionInfo connection) {
		String sessionId = connection.getSessionId();
		Long replicaId = connection.getReplicaId();
		Long createdByUserId = connection.getCreatedBy();

		if (!isSnapshotNeeded(sessionId)) {
			log.info("Session {} does not need a new snapshot. Skipping export.", sessionId);
			return;
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
	}

	private boolean isSnapshotNeeded(String sessionId) {
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
}
