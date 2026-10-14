package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportRequesterTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private GridEventResponsePublisher mockPublisher;

	@InjectMocks
	private GridSnapshotExportRequester requester;

	private String sessionId;
	private Long replicaId;
	private String internalConnectionId;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		internalConnectionId = "internal-con";
	}

	@Test
	public void testRequestSnapshotExportIfNeededBelowThreshold() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(eq(sessionId), eq(List.of())))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verify(mockGridDao).countMissingPatchesForClock(sessionId, List.of());
		verify(mockGridDao, never()).getSingletonConnection(any(), any());
		verifyNoMoreInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededAtThresholdWithInternalConnection() {
		LogicalTimestamp snapshotClockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(42L);
		List<LogicalTimestamp> snapshotClock = List.of(snapshotClockEntry);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(new ClockTable(snapshotClock));
		GridConnectionInfo internalConnection = new GridConnectionInfo().setSessionId(sessionId)
				.setConnectionId(internalConnectionId);

		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verify(mockPublisher).publishEventResponse(
				new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internalConnectionId),
				JsonRxMessageType.Notification, "new-snapshot");
	}

	@Test
	public void testRequestSnapshotExportIfNeededAboveThresholdWithNoInternalConnection() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(eq(sessionId), eq(List.of())))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD + 500);
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.empty());

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verify(mockGridDao).getSingletonConnection(sessionId, EventSource.INTERNAL);
		verifyNoMoreInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNullSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			requester.requestSnapshotExportIfNeeded(null);
		}).getMessage();
		assertEquals("sessionId is required.", message);
		verifyNoMoreInteractions(mockGridDao, mockPublisher);
	}
}
