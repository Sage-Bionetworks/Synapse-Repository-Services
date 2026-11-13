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
	@Mock
	private GridSnapshotExportPublisher mockExportPublisher;

	@InjectMocks
	private GridReplicaSnapshotManager snapshotManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> snapshotClock;
	private ClockTable exportedClockTable;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId).setCreatedBy(789L);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L));
		exportedClockTable = new ClockTable(
				List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L)));
	}

	private void setupSnapshot() {
		when(mockGridDao.getLatestSnapshot(sessionId))
				.thenReturn(Optional.of(new GridSnapshot().setClockTable(new ClockTable(snapshotClock))));
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
	public void testRequestSnapshotIfNeededWithPatchesAtThreshold() {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testRequestSnapshotIfNeededWithPatchesBelowThreshold() {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndPatchesAtThreshold() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndPatchesBelowThreshold() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testExportSnapshotIfNeededWithPatchesAtThreshold() throws Exception {
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);
		File tempFile = setupExport();

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockGridIndexManager).exportSnapshot(sessionId, replicaId, tempFile.toPath());
		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testExportSnapshotIfNeededWithSnapshotSavedSinceRequest() {
		// A queued request whose patches an earlier export already took: few patches since the latest snapshot
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(2);

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testExportSnapshotIfNeededWithExportFailure() throws Exception {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of()))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);
		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenThrow(new RuntimeException("Export failed"));

		assertThrows(RuntimeException.class, () -> {
			// call under test
			snapshotManager.exportSnapshotIfNeeded(connection);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}
