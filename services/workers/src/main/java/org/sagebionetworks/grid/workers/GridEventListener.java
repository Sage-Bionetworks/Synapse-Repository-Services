package org.sagebionetworks.grid.workers;

import java.net.URL;
import java.util.Optional;
import java.util.function.Predicate;

import org.json.JSONObject;
import org.sagebionetworks.grid.workers.message.ConnectionMessage;
import org.sagebionetworks.grid.workers.message.DisconnectedMessage;
import org.sagebionetworks.grid.workers.message.NewPatchRegistrationMessage;
import org.sagebionetworks.grid.workers.message.PingMessage;
import org.sagebionetworks.grid.workers.message.SynchronizeClockMessage;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GridEventListener {

	private final GridEventResponsePublisher publisher;
	private final UserManager userManager;
	private final GridManager manager;

	public GridEventListener(GridEventResponsePublisher publisher, UserManager userManager, GridManager manager) {
		super();
		this.publisher = publisher;
		this.userManager = userManager;
		this.manager = manager;
	}

	@EventListener
	public void onPing(PingMessage ping) {
		ValidateArgument.required(ping, "ping");
		publisher.publishEventResponse(ping.getContext(), JsonRxMessageType.Notification, "pong");
	}

	@EventListener
	public void onConnection(ConnectionMessage message) {
		ValidateArgument.required(message, "message");
		ValidateArgument.required(message.getConnection(), "message.connection");
		ValidateArgument.required(message.getContext(), "message.context");
		UserInfo user = userManager.getUserInfo(message.getConnection().getUserId());
		// Save the connection.
		manager.createReplicaConnection(user, message.getContext(), message.getConnection());
		// notify the call they are connected.
		publisher.publishEventResponse(message.getContext(), JsonRxMessageType.Notification, "connected");
	}

	@EventListener
	public void onDisconnected(DisconnectedMessage message) {
		ValidateArgument.required(message, "message");
		ValidateArgument.required(message.getContext(), "message.context");
		manager.removeReplicatConnection(message.getContext().getEventType(), message.getContext().getConnectionId());
	}

	@EventListener
	public void onNewPatchRegistration(NewPatchRegistrationMessage message) {
		ValidateArgument.required(message, "message");
		boolean isNew = manager.savePatch(message.getContext(), message.getPatchId(), message.getBody());
		// Let the caller know we have accepted their patch so they no longer need to
		// keep a copy.
		publisher.publishEventResponse(message.getContext(), JsonRxMessageType.ResponseComplete,
				message.getRequestId());

		if (isNew) {
			/*
			 * This is a new patch so let all other connected replicas know there is a new
			 * patch.
			 */
			manager.listActiveConnections(message.getContext().getConnectionId()).stream()
					// exclude the caller from the patch notification.
					.filter(Predicate.not(c -> message.getContext().getConnectionId().equals(c.getConnectionId())))
					.forEach(c -> {
						publisher.publishEventResponse(
								new EventContext(EventType.MESSAGE, c.getSource(), c.getConnectionId()),
								JsonRxMessageType.Notification, "new-patch");
					});
		}
	}

	@EventListener
	public void onSynchronizeClock(SynchronizeClockMessage message) {
		ValidateArgument.required(message, "message");

		// Always start a new replica with a snapshot
		boolean getSnapshot = message.getClock() == null || message.getClock().isEmpty();

		if (getSnapshot) {
			Optional<URL> snapshotPresignedUrl = manager.getLatestSnapshotPresignedUrl(message.getContext());
			if (snapshotPresignedUrl.isPresent()) {
				// Send the snapshot URL to the caller
				JSONObject messageBody = new JSONObject();
				messageBody.put("type", "snapshot");
				messageBody.put("body", snapshotPresignedUrl.get().toString());
				publisher.publishEventResponse(message.getContext(), JsonRxMessageType.ResponseData,
						message.getRequestId(), messageBody.toString());
				return;
			}
		}

		// Otherwise, find and send the next missing patch
		Optional<String> optional = manager.getNextMissingPatch(message.getContext(), message.getClock());
		if (optional.isEmpty()) {
			// The clock is up-to-date.
			publisher.publishEventResponse(message.getContext(), JsonRxMessageType.ResponseComplete,
					message.getRequestId());
		} else {
			// The caller needs to apply the provided patch
			String messageBody = "{\"type\":\"patch\",\"body\":" + optional.get() + "}"; // directly inline the patch body (it is always a JSON array)
			publisher.publishEventResponse(message.getContext(), JsonRxMessageType.ResponseData,
					message.getRequestId(), messageBody);
		}

	}

}
