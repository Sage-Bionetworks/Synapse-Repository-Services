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
import java.time.Instant;
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
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

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
	@Mock
	private GridSnapshotExportPublisher mockExportPublisher;
	@Mock
	private ProdDetector mockProdDetector;

	@InjectMocks
	private GridReplicaSnapshotManager snapshotManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> replicaClock;
	private List<LogicalTimestamp> snapshotClock;
	private long snapshotCreatedOn;
	private long now;
	private ClockTable exportedClockTable;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId).setCreatedBy(789L);
		replicaClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L));
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L));
		snapshotCreatedOn = 1_700_000_000_000L;
		now = 1_700_000_000_000L;
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

	private void setupConnection() {
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)).thenReturn(Optional.of(connection));
	}

	/**
	 * A session without a snapshot and with a stored patch needs one.
	 */
	private void setupSnapshotNeeded() {
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);
	}

	private void setupReadyReplica() {
		setupConnection();
		setupSnapshotNeeded();
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(0);
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
	public void testRequestSnapshotIfNeededWithIntervalElapsedAndNewPatches() {
		setupConnection();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verify(mockExportPublisher).publishExportRequest(sessionId);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoNewPatches() {
		setupConnection();
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(0);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verifyNoInteractions(mockClock, mockExportPublisher);
	}

	@Test
	public void testRequestSnapshotIfNeededWithIntervalNotElapsedAndPatchesBelowThreshold() {
		setupConnection();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS - 1);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testRequestSnapshotIfNeededWithPatchesAtThreshold() {
		setupConnection();
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verify(mockExportPublisher).publishExportRequest(sessionId);
		// The patch count alone triggers the snapshot, whatever the snapshot's age
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndNewPatches() {
		setupConnection();
		setupSnapshotNeeded();

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verify(mockExportPublisher).publishExportRequest(sessionId);
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndNoPatches() {
		setupConnection();
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(0);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoInternalConnection() {
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// call under test
		snapshotManager.requestSnapshotIfNeeded(sessionId);

		verifyNoInteractions(mockExportPublisher);
		verify(mockGridDao, never()).getLatestSnapshot(any());
	}

	@Test
	public void testExportSnapshotIfNeeded() throws Exception {
		setupReadyReplica();
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		when(mockClock.currentTimeMillis()).thenReturn(now);

		// call under test
		snapshotManager.exportSnapshotIfNeeded(sessionId);

		verify(mockGridIndexManager).exportSnapshot(sessionId, replicaId, tempFile.toPath());
		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
		verify(mockSnapshotStore).deleteSupersededSnapshots(sessionId,
				Instant.ofEpochMilli(now - GridReplicaSnapshotManager.SUPERSEDED_SNAPSHOT_RETENTION_MS));
	}

	@Test
	public void testExportSnapshotIfNeededWithNoInternalConnection() {
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// call under test
		snapshotManager.exportSnapshotIfNeeded(sessionId);

		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore, mockProdDetector,
				mockExportPublisher);
		verify(mockGridDao, never()).getLatestSnapshot(any());
	}

	@Test
	public void testExportSnapshotIfNeededWithLegacyConnectionIdBody() {
		// A request queued before deploy carries the internal replica's connection ID, which matches no session
		String connectionId = "abc123=";
		when(mockGridDao.getSingletonConnection(connectionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connectionId);

		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore, mockProdDetector,
				mockExportPublisher);
	}

	@Test
	public void testExportSnapshotIfNeededWithReplicaBehind() {
		setupConnection();
		setupSnapshotNeeded();
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(1);

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			snapshotManager.exportSnapshotIfNeeded(sessionId);
		});

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verifyNoInteractions(mockFileProvider, mockSnapshotStore, mockProdDetector);
	}

	@Test
	public void testExportSnapshotIfNeededWithStagingStack() throws Exception {
		setupReadyReplica();
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(false));

		// call under test
		snapshotManager.exportSnapshotIfNeeded(sessionId);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		// Staging shares the snapshot bucket with production, so it never deletes
		verify(mockSnapshotStore, never()).deleteSupersededSnapshots(any(), any());
	}

	@Test
	public void testExportSnapshotIfNeededWithUndetectedStack() throws Exception {
		setupReadyReplica();
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.empty());

		// call under test
		snapshotManager.exportSnapshotIfNeeded(sessionId);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		verify(mockSnapshotStore, never()).deleteSupersededSnapshots(any(), any());
	}

	@Test
	public void testExportSnapshotIfNeededWithDeleteFailure() throws Exception {
		setupReadyReplica();
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		when(mockClock.currentTimeMillis()).thenReturn(now);
		when(mockSnapshotStore.deleteSupersededSnapshots(eq(sessionId), any(Instant.class)))
				.thenThrow(new RuntimeException("S3 unavailable"));

		// call under test: the saved snapshot stands and the next export retries the deletion
		snapshotManager.exportSnapshotIfNeeded(sessionId);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
	}

	@Test
	public void testExportSnapshotIfNeededWithSnapshotNotNeeded() {
		// For example, a queued request whose snapshot an earlier export already took
		setupConnection();
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(0);

		// call under test
		snapshotManager.exportSnapshotIfNeeded(sessionId);

		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore, mockProdDetector);
	}

	@Test
	public void testExportSnapshotIfNeededWithExportFailure() throws Exception {
		setupReadyReplica();
		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenThrow(new RuntimeException("Export failed"));

		assertThrows(RuntimeException.class, () -> {
			// call under test
			snapshotManager.exportSnapshotIfNeeded(sessionId);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		verifyNoInteractions(mockProdDetector);
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}
