package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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

	/**
	 * Export the replica's current state as the session's latest snapshot. Does nothing when the latest snapshot
	 * already has the replica's clock.
	 */
	public void exportSnapshot(GridConnectionInfo connection) {
		String sessionId = connection.getSessionId();
		Long replicaId = connection.getReplicaId();
		Long createdByUserId = connection.getCreatedBy();

		/*
		 * The hub can request a second export before the first is saved, when two synchronize-clock chains are in
		 * flight (a chain expires while the hub is behind on answering it).
		 */
		if (isLatestSnapshotAtClock(sessionId, gridIndexManager.getClock(sessionId, replicaId))) {
			log.info("Latest snapshot of session {} already has the clock of replica {}. Skipping export.", sessionId,
					replicaId);
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

	private boolean isLatestSnapshotAtClock(String sessionId, List<LogicalTimestamp> clock) {
		return gridDao.getLatestSnapshot(sessionId)
				.map(snapshot -> Set.copyOf(snapshot.getClockTable().getClocks()).equals(Set.copyOf(clock)))
				.orElse(false);
	}
}
