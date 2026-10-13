package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.FileProvider;

@ExtendWith(MockitoExtension.class)
public class GridReplicaSnapshotManagerTest {

	@Mock
	private GridIndexManager mockGridIndexManager;
	@Mock
	private SnapshotStore mockSnapshotStore;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private GridDao mockGridDao;

	@InjectMocks
	private GridReplicaSnapshotManager snapshotManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> replicaClock;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId);
		replicaClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L));
	}

	@Test
	public void testCreateSnapshotIfNeededSuccess() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);
		List<LogicalTimestamp> latestSnapshotClock = List.of(clockEntry);

		ClockTable latestSnapshotClockTable = new ClockTable(latestSnapshotClock);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(latestSnapshotClockTable);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(600);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		ClockTable clockTable = new ClockTable(List.of(clockEntry));
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 500);

		verify(mockGridIndexManager).exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class));
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testCreateSnapshotIfNeededWithNoSnapshotAndHasPatches() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);

		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(10);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		ClockTable clockTable = new ClockTable(List.of(clockEntry));
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 9);

		verify(mockGridIndexManager).exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class));
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testCreateSnapshotIfNeededWithNoSnapshotAndNoPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(0);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 10);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testCreateSnapshotIfNeededWithNoPatchesSinceLatestSnapshot() {
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);
		List<LogicalTimestamp> latestSnapshotClock = List.of(clockEntry);

		ClockTable latestSnapshotClockTable = new ClockTable(latestSnapshotClock);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(latestSnapshotClockTable);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(latestSnapshotClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(0);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 500);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testCreateSnapshotIfNeededWithReplicaLaggingStoredPatches() {
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(100L);
		List<LogicalTimestamp> latestSnapshotClock = List.of(clockEntry);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(new ClockTable(latestSnapshotClock));
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		// The hub holds 2,000 patches (25 MB) the snapshot lacks, but the replica has applied only 10 (100 KB) of them
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(2000);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(1990);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, latestSnapshotClock)).thenReturn(25_000_000L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(24_900_000L);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 1000);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testCreateSnapshotIfNeededWithFewLargePatches() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);

		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(447);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, List.of()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(0L);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		ClockTable clockTable = new ClockTable(List.of(clockEntry));
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 1000);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
	}

	@Test
	public void testCreateSnapshotIfNeededWithFewSmallPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(447);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, List.of()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD - 1);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(0L);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 1000);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testOnExportSnapshotCleansUpOnFailure() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);
		List<LogicalTimestamp> latestSnapshotClock = List.of(clockEntry);

		ClockTable latestSnapshotClockTable = new ClockTable(latestSnapshotClock);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(latestSnapshotClockTable);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(600);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenThrow(new RuntimeException("Export failed"));

		// call under test
		assertThrows(RuntimeException.class, () -> {
			snapshotManager.createSnapshotIfNeeded(connection, 500);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}

	private List<LogicalTimestamp> setupSnapshotAndReplicaClocks() {
		List<LogicalTimestamp> snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(100L));
		when(mockGridDao.getLatestSnapshot(sessionId))
				.thenReturn(Optional.of(new GridSnapshot().setClockTable(new ClockTable(snapshotClock))));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		return snapshotClock;
	}

	@Test
	public void testIsSnapshotNeededWithAppliedCountAtThreshold() {
		List<LogicalTimestamp> snapshotClock = setupSnapshotAndReplicaClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD + 5);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(5);

		// call under test
		assertTrue(snapshotManager.isSnapshotNeeded(sessionId, replicaId));
	}

	@Test
	public void testIsSnapshotNeededWithAppliedCountBelowThreshold() {
		List<LogicalTimestamp> snapshotClock = setupSnapshotAndReplicaClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD + 4);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(5);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock)).thenReturn(1_000L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(0L);

		// call under test
		assertFalse(snapshotManager.isSnapshotNeeded(sessionId, replicaId));
	}

	@Test
	public void testIsSnapshotNeededWithAppliedBytesAtThreshold() {
		List<LogicalTimestamp> snapshotClock = setupSnapshotAndReplicaClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(460);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(13);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD + 1_000L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(1_000L);

		// call under test
		assertTrue(snapshotManager.isSnapshotNeeded(sessionId, replicaId));
	}

	@Test
	public void testIsSnapshotNeededWithAppliedBytesBelowThreshold() {
		List<LogicalTimestamp> snapshotClock = setupSnapshotAndReplicaClocks();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(460);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(13);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD + 999L);
		when(mockGridDao.sumMissingPatchBytesForClock(sessionId, replicaClock)).thenReturn(1_000L);

		// call under test
		assertFalse(snapshotManager.isSnapshotNeeded(sessionId, replicaId));
	}

	@Test
	public void testCreateSnapshotIfNeededWithReplicaBehindHubPastThreshold() throws Exception {
		connection.setCreatedBy(789L);
		List<LogicalTimestamp> snapshotClock = setupSnapshotAndReplicaClocks();
		// The replica has applied 1,000 patches since the snapshot while 500 more are still waiting on the hub
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1500);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(500);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		ClockTable clockTable = new ClockTable(replicaClock);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection, 1000);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
	}
}

