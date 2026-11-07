package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.sql.Timestamp;
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
import org.sagebionetworks.util.Clock;
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
	@Mock
	private Clock mockClock;

	@InjectMocks
	private GridReplicaSnapshotManager snapshotManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> snapshotClock;
	private long snapshotCreatedOn;
	private ClockTable exportedClockTable;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId).setCreatedBy(789L);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L));
		snapshotCreatedOn = 1_700_000_000_000L;
		exportedClockTable = new ClockTable(
				List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L)));
	}

	private void setupSnapshot() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.of(new GridSnapshot()
				.setClockTable(new ClockTable(snapshotClock)).setCreatedOn(new Timestamp(snapshotCreatedOn))));
	}

	private void setupSnapshotAge(long ageMs) {
		when(mockClock.currentTimeMillis()).thenReturn(snapshotCreatedOn + ageMs);
	}

	private File setupExport() throws Exception {
		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenReturn(exportedClockTable);
		return tempFile;
	}

	@Test
	public void testCreateSnapshotIfNeededWithIntervalElapsedAndNewPatches() throws Exception {
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);
		File tempFile = setupExport();

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(exportedClockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testCreateSnapshotIfNeededWithNoNewPatches() {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(0);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection);

		verifyNoInteractions(mockClock, mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testCreateSnapshotIfNeededWithIntervalNotElapsedAndPatchesBelowThreshold() {
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS - 1);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection);

		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testCreateSnapshotIfNeededWithPatchesAtThreshold() throws Exception {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);
		setupExport();

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(exportedClockTable), eq(789L), any(File.class));
		// The patch count alone triggers the snapshot, whatever the snapshot's age
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testCreateSnapshotIfNeededWithNoSnapshotAndNewPatches() throws Exception {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);
		setupExport();

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(exportedClockTable), eq(789L), any(File.class));
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testCreateSnapshotIfNeededWithNoSnapshotAndNoPatches() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(0);

		// call under test
		snapshotManager.createSnapshotIfNeeded(connection);

		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testCreateSnapshotIfNeededWithExportFailure() throws Exception {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);
		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenThrow(new RuntimeException("Export failed"));

		assertThrows(RuntimeException.class, () -> {
			// call under test
			snapshotManager.createSnapshotIfNeeded(connection);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}
