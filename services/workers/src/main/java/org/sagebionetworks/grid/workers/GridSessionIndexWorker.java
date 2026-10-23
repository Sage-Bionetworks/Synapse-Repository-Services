package org.sagebionetworks.grid.workers;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springframework.stereotype.Service;

/**
 * Worker that consumes GRID_SESSION change messages and fires "new-patch"
 * notifications to internal replicas, triggering the synchronization chain that
 * rebuilds the grid index database.
 */
@Service
public class GridSessionIndexWorker implements ChangeMessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridSessionIndexWorker.class);

	private final GridManager gridManager;
	private final GridEventResponsePublisher publisher;

	public GridSessionIndexWorker(GridManager gridManager, GridEventResponsePublisher publisher) {
		this.gridManager = gridManager;
		this.publisher = publisher;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message) throws Exception {
		if (!ObjectType.GRID_SESSION.equals(message.getObjectType())) {
			return;
		}
		String sessionId = GridUtils.gridSessionIdAsString(Long.parseLong(message.getObjectId()));
		Optional<GridConnectionInfo> connection = gridManager.getSingletonConnection(sessionId,
				EventSource.INTERNAL);
		if (connection.isEmpty()) {
			log.info("No INTERNAL connection for grid session: {}, skipping.", sessionId);
			return;
		}
		log.info("Firing new-patch for grid session: {}", sessionId);
		publisher.publishEventResponses(
				List.of(new EventContext(EventType.MESSAGE, EventSource.INTERNAL,
						connection.get().getConnectionId())),
				JsonRxMessageType.Notification, "new-patch");
	}
}
