package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class InternalMessageDispatcherTest {

	@Mock
	private GridReplicaManager mockGridReplicaManager;
	@Mock
	private GridIndexManager mockGridIndexManager;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private InternalMessageDispatcher dispatcher;

	private JsonRxMessageBundle bundle;
	private JsonRxMessage message;
	private String sessionId;
	private Long replicaId;
	private GridConnectionInfo connection;
	private Integer chainId;
	private MessageChain messageChain;

	@BeforeEach
	public void before() {
		sessionId = "session123";
		replicaId = 111L;
		message = new JsonRxMessage("[8,\"connected\"]");
		connection = new GridConnectionInfo().setConnectionId("connectionId").setSessionId(sessionId)
				.setReplicaId(replicaId);
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		chainId = 99;
		messageChain = new MessageChain().setId(chainId).setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK)
				.setReplicaId(replicaId).setSessionId(sessionId);
	}

	@Test
	public void testDispatchMessageWithConnected() {
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onConnected(mockCallback, connection);
	}

	@Test
	public void testDispatchMessageWithReplicaConnected() {
		message = new JsonRxMessage("[8,\"replica-connected\"]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onConnected(mockCallback, connection);
	}

	@Test
	public void testDispatchWithNewPatch() {
		message = new JsonRxMessage("[8,\"new-patch\"]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onNewPatch(mockCallback, connection);
	}

	@Test
	public void testDispatchWithNewSnapshot() {
		message = new JsonRxMessage("[8,\"new-snapshot\"]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onExportSnapshot(mockCallback, connection);
	}

	@Test
	public void testDispatchWithNotificationOther() {
		message = new JsonRxMessage("[8,\"other\"]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dispatcher.dispatchMessage(bundle);
		}).getMessage();
		assertEquals("Cannot handle: '[8,\"other\"]'", message);
		verifyNoMoreInteractions(mockGridReplicaManager);
	}

	@Test
	public void testDispatchWithNotSupportedYet() {
		message = new JsonRxMessage("[7,\"other\"]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dispatcher.dispatchMessage(bundle);
		}).getMessage();
		assertEquals("Cannot handle: '[7,\"other\"]'", message);
		verifyNoMoreInteractions(mockGridReplicaManager);
	}

	@Test
	public void testDispatchWithResponseSynch() {
		when(mockGridIndexManager.getMessageChain(sessionId, replicaId, chainId)).thenReturn(Optional.of(messageChain));
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L))
				.setOperations(List.of());
		JSONArray array = new JSONArray();
		array.put(JsonRxMessageType.ResponseData.getCode());
		array.put(chainId);
		JSONObject body = new JSONObject();
		body.put("type", "patches");
		JSONArray patches = new JSONArray();
		patches.put(PatchCompactSerializable.serialize(patch));
		body.put("body", patches);
		array.put(body);
		message = new JsonRxMessage(array);
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onApplyPatches(mockCallback, connection, chainId, List.of(patch));
	}

	@Test
	public void testDispatchWithResponseSynchMissingId() {
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L))
				.setOperations(List.of());
		JSONArray array = new JSONArray();
		array.put(JsonRxMessageType.ResponseData.getCode());
		array.put(PatchCompactSerializable.serialize(patch));
		message = new JsonRxMessage(array);
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dispatcher.dispatchMessage(bundle);
		}).getMessage();
		assertEquals("ResponseData must have an ID.", message);
		verifyNoMoreInteractions(mockGridReplicaManager);
	}

	@Test
	public void testDispatchWithResponseUnknownMethod() {
		messageChain.setMethod("not-it");
		when(mockGridIndexManager.getMessageChain(sessionId, replicaId, chainId)).thenReturn(Optional.of(messageChain));
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L))
				.setOperations(List.of());
		JSONArray array = new JSONArray();
		array.put(JsonRxMessageType.ResponseData.getCode());
		array.put(chainId);
		array.put(PatchCompactSerializable.serialize(patch));
		message = new JsonRxMessage(array);
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dispatcher.dispatchMessage(bundle);
		}).getMessage();
		assertEquals("Cannot handle: '[4,99,[[[111,1]]]]'", message);
		verifyNoMoreInteractions(mockGridReplicaManager);
	}

	@Test
	public void testDispatchWithResponseSynchAndNoChain() {
		when(mockGridIndexManager.getMessageChain(sessionId, replicaId, chainId)).thenReturn(Optional.empty());
		Patch patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L))
				.setOperations(List.of());
		JSONArray array = new JSONArray();
		array.put(JsonRxMessageType.ResponseData.getCode());
		array.put(chainId);
		array.put(PatchCompactSerializable.serialize(patch));
		message = new JsonRxMessage(array);
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dispatcher.dispatchMessage(bundle);
		}).getMessage();
		assertEquals("No message chain found for session: session123, replica: 111, id: 99", message);
		verifyNoMoreInteractions(mockGridReplicaManager);
	}

	@Test
	public void testDispatchWithComplete() {
		message = new JsonRxMessage("[8,\"new-patch\"]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onNewPatch(mockCallback, connection);
	}

	@Test
	public void testDispatchWithResponseComplete() {
		message = new JsonRxMessage("[5,99]");
		bundle = new JsonRxMessageBundle(message, connection, mockCallback);
		// call under test
		dispatcher.dispatchMessage(bundle);
		verify(mockGridReplicaManager).onResponseComplete(mockCallback, connection, chainId);
	}
}
