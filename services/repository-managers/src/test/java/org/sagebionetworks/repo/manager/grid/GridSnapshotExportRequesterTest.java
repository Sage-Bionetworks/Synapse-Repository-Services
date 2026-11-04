package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
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
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportRequesterTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private GridEventResponsePublisher mockPublisher;
	@Mock
	private Clock mockClock;

	@InjectMocks
	private GridSnapshotExportRequester requester;

	private String sessionId;
	private String internalConnectionId;
	private EventContext internalReplica;
	private List<LogicalTimestamp> snapshotClock;
	private long snapshotCreatedOn;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		internalConnectionId = "internal-con";
		internalReplica = new EventContext(EventType.MESSAGE, EventSource.INTERNAL, internalConnectionId);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(111L).setSequenceNumber(42L));
		snapshotCreatedOn = 1_700_000_000_000L;
	}

	private void setupConnection() {
		when(mockGridDao.getConnection(internalConnectionId)).thenReturn(Optional
				.of(new GridConnectionInfo().setSessionId(sessionId).setConnectionId(internalConnectionId)));
	}

	private void setupSnapshotWithAge(long ageMs) {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(new GridSnapshot()
				.setClockTable(new ClockTable(snapshotClock)).setCreatedOn(new Timestamp(snapshotCreatedOn))));
		when(mockClock.currentTimeMillis()).thenReturn(snapshotCreatedOn + ageMs);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithIntervalElapsedAndNewPatches() {
		setupConnection();
		setupSnapshotWithAge(GridSnapshotExportRequester.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockPublisher).publishEventResponse(internalReplica, JsonRxMessageType.Notification, "new-snapshot");
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithIntervalElapsedAndNoNewPatches() {
		setupConnection();
		setupSnapshotWithAge(GridSnapshotExportRequester.SNAPSHOT_INTERVAL_MS * 10);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(0);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verifyNoInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithIntervalNotElapsed() {
		setupConnection();
		setupSnapshotWithAge(GridSnapshotExportRequester.SNAPSHOT_INTERVAL_MS - 1);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockGridDao, never()).countMissingPatchesForClock(any(), anyList());
		verifyNoInteractions(mockPublisher);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNoSnapshotAndNewPatches() {
		setupConnection();
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verify(mockPublisher).publishEventResponse(internalReplica, JsonRxMessageType.Notification, "new-snapshot");
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotExportIfNeededWithNoSnapshotAndNoPatches() {
		setupConnection();
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(0);

		// call under test
		requester.requestSnapshotExportIfNeeded(internalReplica);

		verifyNoInteractions(mockPublisher);
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
		verifyNoInteractions(mockGridDao, mockPublisher, mockClock);
	}
}
