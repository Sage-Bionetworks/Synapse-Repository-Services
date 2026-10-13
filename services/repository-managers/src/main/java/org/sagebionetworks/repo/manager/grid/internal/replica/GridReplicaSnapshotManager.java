package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.FileProvider;
import org.springframework.stereotype.Component;

@Component
public class GridReplicaSnapshotManager {

	private static final Logger log = LogManager.getLogger(GridReplicaSnapshotManager.class);

	public static final int PATCH_COUNT_SNAPSHOT_THRESHOLD = 1000;

	/**
	 * Patches can be large (a CSV import produces patches close to the maximum patch size), so a count alone
	 * can leave a client replaying many megabytes one websocket round trip at a time.
	 */
	public static final long PATCH_BYTES_SNAPSHOT_THRESHOLD = 5L * 1024 * 1024;

	private final GridIndexManager gridIndexManager;
	private final SnapshotStore snapshotStore;
	private final FileProvider fileProvider;
	private final GridDao gridDao;

	public GridReplicaSnapshotManager(GridIndexManager gridIndexManager, SnapshotStore snapshotStore,
			FileProvider fileProvider, GridDao gridDao) {
		this.gridIndexManager = gridIndexManager;
		this.snapshotStore = snapshotStore;
		this.fileProvider = fileProvider;
		this.gridDao = gridDao;
	}

	public void createSnapshotIfNeeded(GridConnectionInfo connection) {
		createSnapshotIfNeeded(connection, PATCH_COUNT_SNAPSHOT_THRESHOLD);
	}

	void createSnapshotIfNeeded(GridConnectionInfo connection, int patchCountSnapshotThreshold) {
		String sessionId = connection.getSessionId();
		Long replicaId = connection.getReplicaId();
		Long createdByUserId = connection.getCreatedBy();

		if (!isSnapshotNeeded(sessionId, replicaId, patchCountSnapshotThreshold)) {
			log.info("Session {} replica {} has applied fewer than {} patches and {} patch bytes since latest snapshot. Skipping export.",
					sessionId, replicaId, patchCountSnapshotThreshold, PATCH_BYTES_SNAPSHOT_THRESHOLD);
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

	/**
	 * A snapshot can only contain what the replica has applied, so the thresholds are measured against the patches
	 * the replica has applied since the latest snapshot, not against every patch stored for the session.
	 *
	 * @param replicaId           The replica that would export the snapshot.
	 * @param patchCountThreshold The number of applied patches that requires a snapshot.
	 * @return True if either the number or the total size of the patches the replica has applied since the latest
	 *         snapshot reaches its threshold.
	 */
	boolean isSnapshotNeeded(String sessionId, Long replicaId, int patchCountThreshold) {
		List<LogicalTimestamp> snapshotClock = gridDao.getLatestSnapshot(sessionId)
				.map(snapshot -> snapshot.getClockTable().getClocks())
				.orElse(List.of());
		List<LogicalTimestamp> replicaClock = gridIndexManager.getClock(sessionId, replicaId);
		/*
		 * Measured against the stored patches instead, a replica that lags the hub stays over the threshold after
		 * every export it makes, so it would export near-identical snapshots for each new patch (PLFM-10021). The
		 * replica's clock is at or past the snapshot clock because snapshots are exported from that replica.
		 */
		int appliedCount = gridDao.countMissingPatchesForClock(sessionId, snapshotClock)
				- gridDao.countMissingPatchesForClock(sessionId, replicaClock);
		if (appliedCount >= patchCountThreshold) {
			return true;
		}
		long appliedBytes = gridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock)
				- gridDao.sumMissingPatchBytesForClock(sessionId, replicaClock);
		return appliedBytes >= PATCH_BYTES_SNAPSHOT_THRESHOLD;
	}
}
