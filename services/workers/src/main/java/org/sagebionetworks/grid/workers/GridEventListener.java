package org.sagebionetworks.grid.workers;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
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
		// notify the caller they are connected.
		publisher.publishEventResponse(message.getContext(), JsonRxMessageType.Notification, "connected");
		// Broadcast replica-connected to all other active connections.
		List<EventContext> contexts = manager.listActiveConnections(message.getContext().getConnectionId()).stream()
				.filter(Predicate.not(c -> message.getContext().getConnectionId().equals(c.getConnectionId())))
				.map(c -> new EventContext(EventType.MESSAGE, c.getSource(), c.getConnectionId()))
				.collect(Collectors.toList());
		publisher.publishEventResponses(contexts, JsonRxMessageType.Notification, "replica-connected");
	}

	@EventListener
	public void onDisconnected(DisconnectedMessage message) {
		ValidateArgument.required(message, "message");
		ValidateArgument.required(message.getContext(), "message.context");
		String connectionId = message.getContext().getConnectionId();
		// Get active connections BEFORE removing (need connection to look up session).
		List<EventContext> contexts = List.of();
		Optional<GridConnectionInfo> connection = manager.getConnectionInfoOptional(connectionId);
		if (connection.isPresent()) {
			contexts = manager.listActiveConnections(connectionId).stream()
					.filter(Predicate.not(c -> connectionId.equals(c.getConnectionId())))
					.map(c -> new EventContext(EventType.MESSAGE, c.getSource(), c.getConnectionId()))
					.collect(Collectors.toList());
		}
		// Remove the connection.
		manager.removeReplicatConnection(message.getContext().getEventType(), connectionId);
		// Broadcast replica-disconnected to remaining connections.
		publisher.publishEventResponses(contexts, JsonRxMessageType.Notification, "replica-disconnected");
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
			List<EventContext> contexts = manager.listActiveConnections(message.getContext().getConnectionId()).stream()
					// exclude the caller from the patch notification.
					.filter(Predicate.not(c -> message.getContext().getConnectionId().equals(c.getConnectionId())))
					.map(c -> new EventContext(EventType.MESSAGE, c.getSource(), c.getConnectionId()))
					.collect(Collectors.toList());
			
			publisher.publishEventResponses(contexts, JsonRxMessageType.Notification, "new-patch");
		}
	}

	@EventListener
	public void onSynchronizeClock(SynchronizeClockMessage message) {
		ValidateArgument.required(message, "message");

		Optional<String> nextMessage = manager.getNextSynchronizeResponse(message.getContext(), message.getClock());
		if (nextMessage.isEmpty()) {
			// The clock is up-to-date.
			publisher.publishEventResponse(message.getContext(), JsonRxMessageType.ResponseComplete,
					message.getRequestId());
		} else {
			// Send the patch/snapshot to the replica.
			publisher.publishEventResponse(message.getContext(), JsonRxMessageType.ResponseData,
					message.getRequestId(), nextMessage.get());
		}
	}

}
