package org.sagebionetworks.repo.manager.grid;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * The hub's decision of when a session needs a new snapshot.
 */
@Service
public class GridSnapshotExportRequester {

	/**
	 * Minimum age of the latest snapshot before a new one is requested. Bounds both how many patches a joining
	 * client replays on top of the snapshot and how many snapshots an active session accumulates.
	 */
	public static final long SNAPSHOT_INTERVAL_MS = 60_000L;

	private final GridDao gridDao;
	private final GridEventResponsePublisher gridEventResponsePublisher;
	private final Clock clock;

	public GridSnapshotExportRequester(GridDao gridDao, GridEventResponsePublisher gridEventResponsePublisher,
			Clock clock) {
		this.gridDao = gridDao;
		this.gridEventResponsePublisher = gridEventResponsePublisher;
		this.clock = clock;
	}

	/**
	 * Ask the internal replica to export a snapshot when patches have been stored since the latest snapshot and
	 * that snapshot is at least {@link #SNAPSHOT_INTERVAL_MS} old. A session without a snapshot gets one as soon
	 * as it has a patch.
	 * <p>
	 * Call only when the internal replica has applied every stored patch, so the snapshot it exports includes all
	 * the patches measured here.
	 *
	 * @param internalReplica The context of the internal replica's connection.
	 */
	public void requestSnapshotExportIfNeeded(EventContext internalReplica) {
		ValidateArgument.required(internalReplica, "internalReplica");
		gridDao.getConnection(internalReplica.getConnectionId())
				.filter(connection -> isSnapshotNeeded(connection.getSessionId()))
				.ifPresent(connection -> gridEventResponsePublisher.publishEventResponse(internalReplica,
						JsonRxMessageType.Notification, "new-snapshot"));
	}

	boolean isSnapshotNeeded(String sessionId) {
		Optional<GridSnapshot> latestSnapshot = gridDao.getLatestSnapshot(sessionId);
		if (latestSnapshot.isPresent() && !isIntervalElapsedSince(latestSnapshot.get())) {
			return false;
		}
		List<LogicalTimestamp> snapshotClock = latestSnapshot.map(snapshot -> snapshot.getClockTable().getClocks())
				.orElse(List.of());
		return gridDao.countMissingPatchesForClock(sessionId, snapshotClock) > 0;
	}

	private boolean isIntervalElapsedSince(GridSnapshot snapshot) {
		return clock.currentTimeMillis() - snapshot.getCreatedOn().getTime() >= SNAPSHOT_INTERVAL_MS;
	}
}
