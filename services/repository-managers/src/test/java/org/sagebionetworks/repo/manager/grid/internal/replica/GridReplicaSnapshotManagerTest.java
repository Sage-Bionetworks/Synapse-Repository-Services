package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId);
	}

	@Test
	public void testCreateSnapshotIfPatchCountIsExceededSuccess() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);
		List<LogicalTimestamp> latestSnapshotClock = List.of(clockEntry);

		ClockTable latestSnapshotClockTable = new ClockTable(latestSnapshotClock);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(latestSnapshotClockTable);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(600);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		ClockTable clockTable = new ClockTable(List.of(clockEntry));
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 500);

		verify(mockGridIndexManager).exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class));
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testCreateSnapshotIfPatchCountIsExceededWithNoSnapshotAndHasPatches() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);

		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(eq(sessionId), any())).thenReturn(10);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		ClockTable clockTable = new ClockTable(List.of(clockEntry));
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 9);

		verify(mockGridIndexManager).exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class));
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testCreateSnapshotIfPatchCountIsExceededWithNoSnapshotAndNoPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(eq(sessionId), any())).thenReturn(0);

		// call under test
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 10);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testCreateSnapshotWithNoPatchesSinceLatestSnapshotIfPatchCountIsExceeded() {
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);
		List<LogicalTimestamp> latestSnapshotClock = List.of(clockEntry);

		ClockTable latestSnapshotClockTable = new ClockTable(latestSnapshotClock);
		GridSnapshot latestSnapshot = new GridSnapshot().setClockTable(latestSnapshotClockTable);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(latestSnapshot));
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(0);

		// call under test
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 500);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testCreateSnapshotIfPatchCountIsExceededWithFewLargePatches() throws Exception {
		connection.setCreatedBy(789L);
		LogicalTimestamp clockEntry = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(100L);

		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(eq(sessionId), any())).thenReturn(447);
		when(mockGridDao.sumMissingPatchBytesForClock(eq(sessionId), any()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		ClockTable clockTable = new ClockTable(List.of(clockEntry));
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class))).thenReturn(clockTable);

		// call under test
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 1000);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(clockTable), eq(789L), any(File.class));
	}

	@Test
	public void testCreateSnapshotIfPatchCountIsExceededWithFewSmallPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(eq(sessionId), any())).thenReturn(447);
		when(mockGridDao.sumMissingPatchBytesForClock(eq(sessionId), any()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_BYTES_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 1000);

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
		when(mockGridDao.countMissingPatchesForClock(sessionId, latestSnapshotClock)).thenReturn(600);

		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);

		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenThrow(new RuntimeException("Export failed"));

		// call under test
		assertThrows(RuntimeException.class, () -> {
			snapshotManager.createSnapshotIfPatchCountIsExceeded(connection, 500);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}

