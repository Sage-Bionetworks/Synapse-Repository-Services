package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class GridReplicaManagerImplTest {

	@Mock
	private GridIndexManager mockGridIndexManager;
	@Mock
	private GridReplicaSnapshotManager mockSnapshotManager;
	@Mock
	private InternalReplicaToHubEventPublisher mockPublisher;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	HttpClient mockHttpClient;
	@Mock
	private HttpResponse<Path> mockHttpResponse;
	@Mock
	private GridReplicaConnectionManager mockGridReplicaConnectionManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private String connectionId;
	private Long replicaId;
	private Integer methodId;
	private List<LogicalTimestamp> clock;
	private Patch patch1;
	private Patch patch2;
	private Map<IndexType, Set<LogicalTimestamp>> changes;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		connectionId = "con123";
		replicaId = 111L;
		methodId = 444;
		connection = new GridConnectionInfo().setConnectionId(connectionId).setReplicaId(replicaId)
				.setSessionId(sessionId);
		clock = List.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(99L));
		patch1 = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))
				.setOperations(List.of());
		patch2 = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L))
				.setOperations(List.of());

		changes = Map.of(IndexType.arr, Set.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(55L)));
	}

	@Spy
	@InjectMocks
	private GridReplicaManagerImpl manager;

	@Test
	public void testOnConnect() {
		doNothing().when(manager).synchronizeClock(mockCallback, connection);
		// call under test
		manager.onConnected(mockCallback, connection);
	}

	@Test
	public void testOnNewPatch() {
		doNothing().when(manager).synchronizeClock(mockCallback, connection);
		// call under test
		manager.onNewPatch(mockCallback, connection);
	}

	@Test
	public void testOnResponseComplete() {
		// call under test
		manager.onResponseComplete(mockCallback, connection, methodId);
		verify(mockGridIndexManager).completeMessageChain(sessionId, replicaId, methodId);
		verifyNoMoreInteractions(mockSnapshotManager);
	}

	@Test
	public void testOnExportSnapshot() {
		// call under test
		manager.onExportSnapshot(mockCallback, connection);
		verify(mockSnapshotManager).createSnapshotIfPatchCountIsExceeded(connection);
	}

	@Test
	public void testSendClockMessage() {
		// call under test
		manager.sendClockMessage(methodId, connectionId, clock);
		verify(mockPublisher).publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK)
						.setId(methodId).setBody(LogicalTimestampCompactSerializable.serializeClock(clock)));
	}

	@Test
	public void testOnApplyPatchesSinglePatch() {
		when(mockGridIndexManager.applyPatch(sessionId, replicaId, patch1)).thenReturn(changes);

		// Mock the gridIndexManager calls
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(clock);
		doNothing().when(manager).sendClockMessage(methodId, connectionId, clock);

		// call under test
		manager.onApplyPatches(mockCallback, connection, methodId, List.of(patch1));

		// verify interactions
		verify(mockGridIndexManager).applyPatch(sessionId, replicaId, patch1);
		verify(mockGridReplicaConnectionManager).sendChangesToTopic(ReplicaChangeSet.fromPatch(connection, changes));
		verify(mockGridIndexManager).getClock(sessionId, replicaId);
		verify(manager).sendClockMessage(methodId, connectionId, clock);
		verify(mockGridIndexManager).refreshMessageChain(sessionId, replicaId, methodId);
	}

	@Test
	public void testOnApplyMultiplePatches() {
		Map<IndexType, Set<LogicalTimestamp>> changes2 = Map.of(IndexType.arr, Set.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(77L)));
		when(mockGridIndexManager.applyPatch(sessionId, replicaId, patch1)).thenReturn(changes);
		when(mockGridIndexManager.applyPatch(sessionId, replicaId, patch2)).thenReturn(changes2);
		Map<IndexType, Set<LogicalTimestamp>> expectedCumulativeChanges = Map.of(IndexType.arr, Set.of(
				new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(55L),
				new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(77L)));

		// Mock the gridIndexManager calls
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(clock);
		doNothing().when(manager).sendClockMessage(methodId, connectionId, clock);

		// call under test
		manager.onApplyPatches(mockCallback, connection, methodId, List.of(patch1, patch2));

		// verify patches are applied in sequence
		InOrder inOrder = inOrder(mockGridIndexManager, manager, mockGridReplicaConnectionManager);
		inOrder.verify(mockGridIndexManager).refreshMessageChain(sessionId, replicaId, methodId);
		inOrder.verify(mockGridIndexManager).applyPatch(sessionId, replicaId, patch1);
		inOrder.verify(mockGridIndexManager).applyPatch(sessionId, replicaId, patch2);
		inOrder.verify(mockGridReplicaConnectionManager)
				.sendChangesToTopic(ReplicaChangeSet.fromPatch(connection, expectedCumulativeChanges));
		inOrder.verify(mockGridIndexManager).getClock(sessionId, replicaId);
		inOrder.verify(manager).sendClockMessage(methodId, connectionId, clock);
	}

	@Test
	public void testOnApplySnapshot() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.bin");

		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(clock);
		doNothing().when(manager).sendClockMessage(methodId, connectionId, clock);
		Path tempFile = Files.createTempFile("test-snapshot-", ".cbor");
		try {
			doReturn(tempFile).when(manager).downloadSnapshotFile(snapshotUrl);

			// call under test
			manager.onApplySnapshot(mockCallback, connection, methodId, snapshotUrl);

			// verify interactions
			verify(mockGridIndexManager).refreshMessageChain(sessionId, replicaId, methodId);
			verify(mockGridIndexManager).applySnapshot(sessionId, replicaId, tempFile);
			verify(mockGridIndexManager).getClock(sessionId, replicaId);
			verify(manager).sendClockMessage(methodId, connectionId, clock);
			verify(mockGridReplicaConnectionManager).sendChangesToTopic(ReplicaChangeSet.fromSnapshot(connection));

			// Verify cleanup occurred
			assertFalse(Files.exists(tempFile), "Temp file should be deleted after success");
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void testApplySnapshotCleansUpOnApplyFailure() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");
		Path tempFile = Files.createTempFile("test-snapshot-", ".cbor");
		assertTrue(Files.exists(tempFile), "Temp file should exist before test");

		doReturn(tempFile).when(manager).downloadSnapshotFile(snapshotUrl);
		doThrow(new RuntimeException("Import failed")).when(mockGridIndexManager).applySnapshot(sessionId, replicaId, tempFile);

		// call under test
		assertThrows(RuntimeException.class, () -> {
			manager.onApplySnapshot(mockCallback, connection, methodId, snapshotUrl);
		});

		// Verify cleanup occurred
		assertFalse(Files.exists(tempFile), "Temp file should be deleted after failure");
	}

	@Test
	public void testSynchronizeClock() {
		when(mockGridIndexManager.getNonExpiredMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK))
				.thenReturn(Optional.empty());
		when(mockGridIndexManager.startMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK))
				.thenReturn(new MessageChain().setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK).setId(methodId));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(clock);

		// call under test
		manager.synchronizeClock(mockCallback, connection);

		verify(mockGridIndexManager).startMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK);
		verify(mockGridIndexManager).getClock(sessionId, replicaId);
		verify(mockPublisher).publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK)
						.setId(methodId).setBody(LogicalTimestampCompactSerializable.serializeClock(clock)));
	}
	
	@Test
	public void testSynchronizeClockWithNonExpiredMessageChain() {
		when(mockGridIndexManager.getNonExpiredMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK))
				.thenReturn(Optional.of(new MessageChain().setId(123)));
		// call under test
		manager.synchronizeClock(mockCallback, connection);
		
		verifyNoMoreInteractions(mockGridIndexManager, mockPublisher);
	}

	@Test
	public void testDownloadSnapshotFileWithNullUrl() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.downloadSnapshotFile(null);
		}).getMessage();
		assertEquals("snapshotPresignedUrl is required.", message);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDownloadSnapshotFileSuccess() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");
		Path expectedPath = Path.of("/tmp/test-snapshot.cbor");

		when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(mockHttpResponse);
		when(mockHttpResponse.statusCode()).thenReturn(200);
		when(mockHttpResponse.body()).thenReturn(expectedPath);

		// call under test
		Path result = manager.downloadSnapshotFile(snapshotUrl);

		assertEquals(expectedPath, result);
		verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDownloadSnapshotFileWithNon200Status() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");

		when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(mockHttpResponse);
		when(mockHttpResponse.statusCode()).thenReturn(404);

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			manager.downloadSnapshotFile(snapshotUrl);
		});
		assertTrue(ex.getMessage().contains("Failed to download snapshot from"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDownloadSnapshotFileWithRetry() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");
		Path expectedPath = Path.of("/tmp/test-snapshot.cbor");

		when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(mockHttpResponse);
		when(mockHttpResponse.statusCode())
				.thenReturn(429)
				.thenReturn(503)
				.thenReturn(200);
		when(mockHttpResponse.body()).thenReturn(expectedPath);

		// call under test
		Path result = manager.downloadSnapshotFile(snapshotUrl);

		assertEquals(expectedPath, result);
		verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDownloadSnapshotFileWithMaxRetries() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");

		when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(mockHttpResponse);
		when(mockHttpResponse.statusCode())
				.thenReturn(429);

		// call under test
		assertThrows(RuntimeException.class, () -> manager.downloadSnapshotFile(snapshotUrl));

		verify(mockHttpClient, times(5)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testDownloadSnapshotFileWithIOException() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");

		when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenThrow(new IOException("Network error"));

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			manager.downloadSnapshotFile(snapshotUrl);
		});
		assertTrue(ex.getMessage().contains("Failed to download snapshot from"));
		assertTrue(ex.getCause() instanceof IOException);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDownloadSnapshotFileWithInterruptedException() throws Exception {
		URL snapshotUrl = new URL("https://example.com/snapshot.cbor");

		when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenThrow(new InterruptedException("Interrupted"));

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			manager.downloadSnapshotFile(snapshotUrl);
		});
		assertTrue(ex.getMessage().contains("Interrupted while downloading snapshot"));
		assertTrue(Thread.currentThread().isInterrupted());
		// Clear the interrupted status for other tests
		Thread.interrupted();
	}


}
