package org.sagebionetworks.repo.manager.grid;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * The hub's decision of when a session needs a new snapshot.
 */
@Service
public class GridSnapshotExportRequester {

	public static final int PATCH_COUNT_SNAPSHOT_THRESHOLD = 1000;

	/**
	 * Patches can be large (a CSV import produces patches close to the maximum patch size), so a count alone
	 * can leave a client replaying many megabytes one websocket round trip at a time.
	 */
	public static final long PATCH_BYTES_SNAPSHOT_THRESHOLD = 5L * 1024 * 1024;

	private final GridDao gridDao;
	private final GridEventResponsePublisher gridEventResponsePublisher;

	public GridSnapshotExportRequester(GridDao gridDao, GridEventResponsePublisher gridEventResponsePublisher) {
		this.gridDao = gridDao;
		this.gridEventResponsePublisher = gridEventResponsePublisher;
	}

	/**
	 * Ask the internal replica to export a snapshot when either the number or the total size of the patches stored
	 * since the latest snapshot reaches its threshold.
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
		List<LogicalTimestamp> snapshotClock = gridDao.getLatestSnapshot(sessionId)
				.map(snapshot -> snapshot.getClockTable().getClocks())
				.orElse(List.of());
		return gridDao.countMissingPatchesForClock(sessionId, snapshotClock) >= PATCH_COUNT_SNAPSHOT_THRESHOLD
				|| gridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock) >= PATCH_BYTES_SNAPSHOT_THRESHOLD;
	}
}
