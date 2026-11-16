package org.sagebionetworks.repo.manager.grid.internal.replica;

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
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotRequesterTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private Clock mockClock;
	@Mock
	private GridSnapshotExportPublisher mockExportPublisher;

	@InjectMocks
	private GridSnapshotRequester snapshotRequester;

	private GridConnectionInfo connection;
	private String sessionId;
	private List<LogicalTimestamp> snapshotClock;
	private long snapshotCreatedOn;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		connection = new GridConnectionInfo().setReplicaId(111L).setSessionId(sessionId).setCreatedBy(789L);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L));
		snapshotCreatedOn = 1_700_000_000_000L;
	}

	private void setupSnapshot() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(new GridSnapshot()
				.setClockTable(new ClockTable(snapshotClock)).setCreatedOn(new Timestamp(snapshotCreatedOn))));
	}

	private void setupSnapshotAge(long ageMs) {
		when(mockClock.currentTimeMillis()).thenReturn(snapshotCreatedOn + ageMs);
	}

	@Test
	public void testRequestSnapshotIfNeededWithIntervalElapsedAndNewPatches() {
		setupSnapshot();
		setupSnapshotAge(GridSnapshotRequester.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);

		// call under test
		snapshotRequester.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoNewPatches() {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(0);

		// call under test
		snapshotRequester.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockClock, mockExportPublisher);
	}

	@Test
	public void testRequestSnapshotIfNeededWithIntervalNotElapsedAndPatchesBelowThreshold() {
		setupSnapshot();
		setupSnapshotAge(GridSnapshotRequester.SNAPSHOT_INTERVAL_MS - 1);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotRequester.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotRequester.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testRequestSnapshotIfNeededWithPatchesAtThreshold() {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridSnapshotRequester.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		snapshotRequester.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		// The patch count alone triggers the snapshot, whatever the snapshot's age
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndNewPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);

		// call under test
		snapshotRequester.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndNoPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(0);

		// call under test
		snapshotRequester.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher);
	}
}
