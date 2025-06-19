package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.event.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class GridEventListenerTest {

	@Mock
	private GridEventResponsePublisher mockPublisher;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private GridManager mockManager;
	@Mock
	private UserInfo mockUser;

	@InjectMocks
	private GridEventListener listener;

	private EventType eventType;
	private EventSource eventSource;
	private String connectionId;
	private EventContext context;
	private PingMessage pingMessgae;
	private Connection connection;
	private ConnectionMessage connectionMessage;
	private Long userId;
	private DisconnectedMessage disconnectMessage;
	private NewPatchRegistrationMessage patchDataRequest;
	private int requestId;
	private String patch;
	private List<LogicalTimestamp> clock;
	private SynchronizeClockMessage synchronizeClockMessage;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		userId = 987L;
		eventType = EventType.CONNECT;
		eventSource = EventSource.WEBSOCKET;
		connectionId = "con123";
		context = new EventContext(eventType, eventSource, connectionId);
		pingMessgae = new PingMessage(context, 0, null);
		connection = new Connection().setGridSessionId(123L).setUserId(userId);
		connectionMessage = new ConnectionMessage(context, EntityFactory.createJSONObjectForEntity(connection));
		disconnectMessage = new DisconnectedMessage(context, null, null);
		requestId = 1099;
		patch = "[[[9,1]],[0]]";
		patchDataRequest = new NewPatchRegistrationMessage(context, requestId, new JSONArray(patch));
		clock = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		synchronizeClockMessage = new SynchronizeClockMessage(context, requestId,
				LogicalTimestampCompactSerializable.serializeClock(clock));
	}

	@Test
	public void testOnPing() {
		// call under test
		listener.onPing(pingMessgae);
		verify(mockPublisher).publishEventResponse(context, JsonRxMessageType.Notification, "pong");

	}

	@Test
	public void testOnPingWithNullMessage() {
		pingMessgae = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onPing(pingMessgae);
		}).getMessage();
		assertEquals("ping is required.", message);
	}

	@Test
	public void testOnConnection() {
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// call under test
		listener.onConnection(connectionMessage);
		verify(mockManager).createReplicaConnection(mockUser, context, connection);
		verify(mockPublisher).publishEventResponse(context, JsonRxMessageType.Notification, "connected");
	}

	@Test
	public void testOnConnectionWithNullUser() {
		connectionMessage = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onConnection(connectionMessage);
		}).getMessage();
		assertEquals("message is required.", message);

		verifyZeroInteractions(mockPublisher, mockUserManager, mockManager);
	}

	@Test
	public void testOnConnectionWithNullContext() throws JSONObjectAdapterException {
		connectionMessage = new ConnectionMessage(null, EntityFactory.createJSONObjectForEntity(connection));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onConnection(connectionMessage);
		}).getMessage();
		assertEquals("message.context is required.", message);

		verifyZeroInteractions(mockPublisher, mockUserManager, mockManager);
	}

	@ParameterizedTest
	@EnumSource(EventType.class)
	public void testOnDisconnected(EventType type) {
		context = new EventContext(type, eventSource, connectionId);
		disconnectMessage = new DisconnectedMessage(context, null, null);
		// call under test
		listener.onDisconnected(disconnectMessage);
		verify(mockManager).removeReplicatConnection(type, connectionId);
	}

	@Test
	public void testOnDisconnectedWithNullMessage() throws JSONObjectAdapterException {
		disconnectMessage = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onDisconnected(disconnectMessage);
		}).getMessage();
		assertEquals("message is required.", message);

		verifyZeroInteractions(mockPublisher, mockUserManager, mockManager);
	}

	@Test
	public void testOnDisconnectedWithContext() throws JSONObjectAdapterException {
		disconnectMessage = new DisconnectedMessage(null, null, null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onDisconnected(disconnectMessage);
		}).getMessage();
		assertEquals("message.context is required.", message);

		verifyZeroInteractions(mockPublisher, mockUserManager, mockManager);
	}

	@Test
	public void testOnPatchDataRequest() {
		when(mockManager.savePatch(context, patchDataRequest.getPatchId(), patch)).thenReturn(true);
		List<GridConnectionInfo> activeCons = List.of(new GridConnectionInfo().setConnectionId(connectionId),
				new GridConnectionInfo().setConnectionId("con999").setSource(EventSource.INTERNAL),
				new GridConnectionInfo().setConnectionId("con888").setSource(EventSource.WEBSOCKET));
		when(mockManager.listActiveConnections(connectionId)).thenReturn(activeCons);
		// call under test
		listener.onNewPatchRegistration(patchDataRequest);

		verify(mockPublisher, times(1)).publishEventResponse(any(), any(), any(Integer.class));
		verify(mockPublisher).publishEventResponse(context, JsonRxMessageType.ResponseComplete, 1099);
		String patchNotification = "[8,\"patch\",[[[9,1]],[0]]]";
		// only other active connections receive the patch notification
		verify(mockPublisher, times(2)).publishEventResponse(any(), any(), any(String.class));
		verify(mockPublisher).publishEventResponse(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, "con999"),
				JsonRxMessageType.Notification, "new-patch");
		verify(mockPublisher).publishEventResponse(new EventContext(EventType.MESSAGE, EventSource.WEBSOCKET, "con888"),
				JsonRxMessageType.Notification, "new-patch");
		verify(mockPublisher, never()).publishEventResponse(context, patchNotification);
	}

	@Test
	public void testOnPatchDataRequestWithPatchExists() {
		when(mockManager.savePatch(context, patchDataRequest.getPatchId(), patch)).thenReturn(false);

		// call under test
		listener.onNewPatchRegistration(patchDataRequest);

		verify(mockManager, never()).listActiveConnections(any());

		verify(mockPublisher, times(1)).publishEventResponse(any(), any(), any(Integer.class));
		verify(mockPublisher).publishEventResponse(context, JsonRxMessageType.ResponseComplete, 1099);
	}

	@Test
	public void testOnPatchDataRequestWithNullPatch() {
		patchDataRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onNewPatchRegistration(patchDataRequest);
		}).getMessage();
		assertEquals("message is required.", message);

		verifyZeroInteractions(mockPublisher, mockUserManager, mockManager);
	}

	@Test
	public void testOnSynchronizeClockWithPatch() {
		String patch = "[[[9,1]],[0]]]";
		when(mockManager.getNextMissingPatch(context, clock)).thenReturn(Optional.of(patch));

		// call under test
		listener.onSynchronizeClock(synchronizeClockMessage);
		verify(mockPublisher).publishEventResponse(context, JsonRxMessageType.ResponseData, requestId, patch);
	}

	@Test
	public void testOnSynchronizeClockWithDone() {
		when(mockManager.getNextMissingPatch(context, clock)).thenReturn(Optional.empty());

		// call under test
		listener.onSynchronizeClock(synchronizeClockMessage);
		verify(mockPublisher).publishEventResponse(context, JsonRxMessageType.ResponseComplete, requestId);
	}

	@Test
	public void testOnSynchronizeClockWithNullMessage() {
		synchronizeClockMessage = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			listener.onSynchronizeClock(synchronizeClockMessage);
		}).getMessage();
		assertEquals("message is required.", message);
	}
}
