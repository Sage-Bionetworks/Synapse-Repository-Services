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

	private final GridIndexManager gridIndexManager;
	private final SnapshotStore snapshotStore;
	private final FileProvider fileProvider;
	private final GridDao gridDao;
	private final GridSnapshotExportPublisher exportPublisher;

	public GridReplicaSnapshotManager(GridIndexManager gridIndexManager, SnapshotStore snapshotStore,
			FileProvider fileProvider, GridDao gridDao, GridSnapshotExportPublisher exportPublisher) {
		this.gridIndexManager = gridIndexManager;
		this.snapshotStore = snapshotStore;
		this.fileProvider = fileProvider;
		this.gridDao = gridDao;
		this.exportPublisher = exportPublisher;
	}

	/**
	 * Queue a snapshot export for the replica when at least {@link #PATCH_COUNT_SNAPSHOT_THRESHOLD} patches have been
	 * stored since the session's latest snapshot.
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
		List<LogicalTimestamp> snapshotClock = gridDao.getLatestSnapshot(sessionId)
				.map(snapshot -> snapshot.getClockTable().getClocks()).orElse(List.of());
		int patchesSinceSnapshot = gridDao.countMissingPatchesForClock(sessionId, snapshotClock);
		return patchesSinceSnapshot >= PATCH_COUNT_SNAPSHOT_THRESHOLD;
	}
}
