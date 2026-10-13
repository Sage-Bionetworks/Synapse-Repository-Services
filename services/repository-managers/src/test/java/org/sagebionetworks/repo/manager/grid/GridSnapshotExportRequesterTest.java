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
import org.sagebionetworks.grid.db.GridIndexManager;
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
	private GridIndexManager mockGridIndexManager;
	@Mock
	private GridEventResponsePublisher mockPublisher;

	@InjectMocks
	private GridSnapshotExportRequester requester;

	private String sessionId;
	private Long replicaId;
	private String internalConnectionId;
	private GridConnectionInfo internalConnection;
	private List<LogicalTimestamp> snapshotClock;
	private List<LogicalTimestamp> replicaClock;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		internalConnectionId = "internal-con";
		internalConnection = new GridConnectionInfo().setSessionId(sessionId).setConnectionId(internalConnectionId)
				.setReplicaId(replicaId);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L));
		replicaClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(900L));
	}

	private void setupClocks() {
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridDao.getLatestSnapshot(sessionId))
				.thenReturn(Optional.of(new GridSnapshot().setClockTable(new ClockTable(snapshotClock))));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
	}

	private void verifyNewSnapshotRequested() {
		verify(mockPublisher).publishEventResponse(
				new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internalConnectionId),
				JsonRxMessageType.Notification, "new-snapshot");
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithReplicaLaggingStoredPatches() {
		setupClocks();
		// Far more patches are stored than the snapshot covers, but the replica has applied few of them since
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(3000);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(2990);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock)).thenReturn(30_000_000L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(29_900_000L);

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verifyNoMoreInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithAppliedCountAtThreshold() {
		setupClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD + 5);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(5);

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verifyNewSnapshotRequested();
		verify(mockGridDao, never()).sumMissingPatchBytesForClock(any(), any());
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithAppliedBytesAtThreshold() {
		setupClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(460);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(13);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD + 1_000L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(1_000L);

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verifyNewSnapshotRequested();
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithAppliedBelowBothThresholds() {
		setupClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(460);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(13);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD + 999L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(1_000L);

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verifyNoMoreInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNoSnapshot() {
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verifyNewSnapshotRequested();
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNoInternalConnection() {
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// call under test
		requester.requestSnapshotExportIfNeeded(sessionId);

		verify(mockGridDao, never()).getLatestSnapshot(any());
		verifyNoMoreInteractions(mockGridIndexManager, mockPublisher);
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
