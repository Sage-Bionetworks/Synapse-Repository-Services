package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportRequesterTest {

	@Mock
	private GridReplicaSnapshotManager mockSnapshotManager;
	@Mock
	private GridEventResponsePublisher mockPublisher;

	@InjectMocks
	private GridSnapshotExportRequester requester;

	private String sessionId;
	private Long replicaId;
	private String internalConnectionId;
	private GridConnectionInfo internalConnection;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		internalConnectionId = "internal-con";
		internalConnection = new GridConnectionInfo().setSessionId(sessionId).setConnectionId(internalConnectionId)
				.setReplicaId(replicaId);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithSnapshotNeeded() {
		when(mockSnapshotManager.isSnapshotNeeded(sessionId, replicaId)).thenReturn(true);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalConnection);

		verify(mockPublisher).publishEventResponse(
				new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internalConnectionId),
				JsonRxMessageType.Notification, "new-snapshot");
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithSnapshotNotNeeded() {
		when(mockSnapshotManager.isSnapshotNeeded(sessionId, replicaId)).thenReturn(false);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalConnection);

		verifyNoMoreInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNullConnection() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			requester.requestSnapshotExportIfNeeded(null);
		}).getMessage();
		assertEquals("internal is required.", message);
		verifyNoMoreInteractions(mockSnapshotManager, mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNullReplicaId() {
		internalConnection.setReplicaId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			requester.requestSnapshotExportIfNeeded(internalConnection);
		}).getMessage();
		assertEquals("internal.replicaId is required.", message);
		verifyNoMoreInteractions(mockSnapshotManager, mockPublisher);
	}
}
