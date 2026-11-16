package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Component;

/**
 * Decides when a session needs a new snapshot and queues the export request. Kept apart from
 * {@link GridReplicaSnapshotManager}, which depends on the grid manager to store snapshots, so that the grid manager
 * can request snapshots when it saves a patch.
 */
@Component
public class GridSnapshotRequester {

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

	private final GridDao gridDao;
	private final Clock clock;
	private final GridSnapshotExportPublisher exportPublisher;

	public GridSnapshotRequester(GridDao gridDao, Clock clock, GridSnapshotExportPublisher exportPublisher) {
		this.gridDao = gridDao;
		this.clock = clock;
		this.exportPublisher = exportPublisher;
	}

	/**
	 * Queue a snapshot export for the internal replica of the given connection when {@link #isSnapshotNeeded(String)}.
	 * <p>
	 * Called after a new patch is saved, so the export runs after the replica receives the patch and waits until the
	 * replica has applied every stored patch.
	 */
	public void requestSnapshotIfNeeded(GridConnectionInfo connection) {
		if (isSnapshotNeeded(connection.getSessionId())) {
			exportPublisher.publishExportRequest(connection);
		}
	}

	/**
	 * A session needs a snapshot when patches have been stored since its latest snapshot and either that snapshot is
	 * at least {@link #SNAPSHOT_INTERVAL_MS} old or there are at least {@link #PATCH_COUNT_SNAPSHOT_THRESHOLD} of them.
	 * A session without a snapshot needs one as soon as it has a patch. Reads only the stored patches and snapshots,
	 * not the replica.
	 */
	public boolean isSnapshotNeeded(String sessionId) {
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
