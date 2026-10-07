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

	public void createSnapshotIfPatchCountIsExceeded(GridConnectionInfo connection) {
		createSnapshotIfPatchCountIsExceeded(connection, PATCH_COUNT_SNAPSHOT_THRESHOLD);
	}

	void createSnapshotIfPatchCountIsExceeded(GridConnectionInfo connection, int patchCountSnapshotThreshold) {
		String sessionId = connection.getSessionId();
		Long replicaId = connection.getReplicaId();
		Long createdByUserId = connection.getCreatedBy();

		List<LogicalTimestamp> snapshotClock = List.of();
		Optional<GridSnapshot> latestSnapshot = gridDao.getLatestSnapshot(sessionId);
		if (latestSnapshot.isPresent()) {
			snapshotClock = latestSnapshot.get().getClockTable().getClocks();
		}

		if (!isSnapshotNeeded(gridDao, sessionId, snapshotClock, patchCountSnapshotThreshold)) {
			log.info("Session {} has fewer than {} new patches and {} new patch bytes since latest snapshot. Skipping export.",
					sessionId, patchCountSnapshotThreshold, PATCH_BYTES_SNAPSHOT_THRESHOLD);
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
	 * @return True if either the number or the total size of the patches newer than the given snapshot clock
	 *         reaches its threshold.
	 */
	public static boolean isSnapshotNeeded(GridDao gridDao, String sessionId, List<LogicalTimestamp> snapshotClock,
			int patchCountThreshold) {
		if (gridDao.countMissingPatchesForClock(sessionId, snapshotClock) >= patchCountThreshold) {
			return true;
		}
		return gridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock) >= PATCH_BYTES_SNAPSHOT_THRESHOLD;
	}
}
