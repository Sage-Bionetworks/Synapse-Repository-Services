package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
	private String internalConnectionId;
	private EventContext internalReplica;
	private List<LogicalTimestamp> snapshotClock;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		internalConnectionId = "internal-con";
		internalReplica = new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internalConnectionId);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(111L).setSequenceNumber(42L));
	}

	private void setupConnectionAndSnapshot() {
		when(mockGridDao.getConnection(internalConnectionId)).thenReturn(Optional
				.of(new GridConnectionInfo().setSessionId(sessionId).setConnectionId(internalConnectionId)));
		when(mockGridDao.getLatestSnapshot(sessionId))
				.thenReturn(Optional.of(new GridSnapshot().setClockTable(new ClockTable(snapshotClock))));
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithPatchCountAtThreshold() {
		setupConnectionAndSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotExportRequester.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockPublisher).publishEventResponse(internalReplica, JsonRxMessageType.Notification, "new-snapshot");
		verify(mockGridDao, never()).sumMissingPatchBytesForClock(any(), any());
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithPatchBytesAtThreshold() {
		setupConnectionAndSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotExportRequester.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotExportRequester.PATCH_BYTES_SNAPSHOT_THRESHOLD);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockPublisher).publishEventResponse(internalReplica, JsonRxMessageType.Notification, "new-snapshot");
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithBothBelowThreshold() {
		setupConnectionAndSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotExportRequester.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotExportRequester.PATCH_BYTES_SNAPSHOT_THRESHOLD - 1);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verifyNoInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNoSnapshot() {
		when(mockGridDao.getConnection(internalConnectionId)).thenReturn(Optional
				.of(new GridConnectionInfo().setSessionId(sessionId).setConnectionId(internalConnectionId)));
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of()))
				.thenReturn(GridSnapshotExportRequester.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockPublisher).publishEventResponse(internalReplica, JsonRxMessageType.Notification, "new-snapshot");
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithClosedConnection() {
		when(mockGridDao.getConnection(internalConnectionId)).thenReturn(Optional.empty());

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockGridDao, never()).getLatestSnapshot(any());
		verifyNoInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNullContext() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			requester.requestSnapshotExportIfNeeded(null);
		}).getMessage();
		assertEquals("internalReplica is required.", message);
		verifyNoInteractions(mockGridDao, mockPublisher);
	}
}
