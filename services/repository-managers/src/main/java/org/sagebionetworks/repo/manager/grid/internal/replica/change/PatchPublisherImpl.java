package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class PatchPublisherImpl implements PatchPublisher {

	private static final Logger log = LogManager.getLogger(PatchPublisherImpl.class);

	public static final long MAX_WAIT_MS = 60_000L;

	private final InternalReplicaToHubEventPublisher eventPublisher;
	private final GridIndexManager gridIndexManager;
	private final Clock clock;

	public PatchPublisherImpl(InternalReplicaToHubEventPublisher eventPublisher, GridIndexManager gridIndexManager,
			Clock clock) {
		super();
		this.eventPublisher = eventPublisher;
		this.gridIndexManager = gridIndexManager;
		this.clock = clock;
	}

	@Override
	public void publishPatch(GridConnectionInfo connection, JSONArray patchBody) {
		MessageChain chain = gridIndexManager.startMessageChain(connection.getSessionId(), connection.getReplicaId(),
				"patch");
		JsonRxMessage message = new JsonRxMessage(JsonRxMessageType.RequestData).setId(chain.getId()).setMethod("patch")
				.setBody(patchBody);

		EventContext context = new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connection.getConnectionId());
		eventPublisher.publishEvent(context, message);

		waitForPatchToBeAccepted(chain);
	}

	void waitForPatchToBeAccepted(MessageChain chain) {
	    long start = clock.currentTimeMillis();
	    long sleepMs = 50L; // Start with 50ms
	    long maxSleepMs = 2000L; // Cap at 2 seconds
	    
	    while (gridIndexManager.getMessageChain(chain.getSessionId(), chain.getReplicaId(), chain.getId())
	            .isPresent()) {
	        log.debug("Waiting for patch to be accepted for message chain: {}...", chain);
	        try {
	            clock.sleep(sleepMs);
	            sleepMs = Math.min(sleepMs * 2, maxSleepMs); // Exponential backoff
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	            throw new RuntimeException(e);
	        }
	        long current = clock.currentTimeMillis();
	        if (current - start > MAX_WAIT_MS) {
	            throw new RecoverableMessageException("Timed out waiting for a patch to be accepted for: " + chain);
	        }
	    }
	}

}
