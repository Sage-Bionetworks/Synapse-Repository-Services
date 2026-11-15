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
import org.sagebionetworks.repo.model.dbo.grid.SnapshotEligibleMarker;
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
	@Mock
	private GridSnapshotExportPublisher mockExportPublisher;
	@Mock
	private ProdDetector mockProdDetector;

	@InjectMocks
	private GridReplicaSnapshotManager snapshotManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> snapshotClock;
	private long snapshotCreatedOn;
	private ClockTable exportedClockTable;
	private SnapshotEligibleMarker marker;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId).setCreatedBy(789L);
		snapshotClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L));
		snapshotCreatedOn = 1_700_000_000_000L;
		exportedClockTable = new ClockTable(
				List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L)));
		marker = new SnapshotEligibleMarker(sessionId,
				new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(4999L), "patch-key.json",
				new Timestamp(snapshotCreatedOn));
	}

	/**
	 * This stack saved a patch for the session that is still stored.
	 */
	private void setupEligible() {
		when(mockGridDao.getSnapshotEligibleMarker(sessionId)).thenReturn(Optional.of(marker));
		when(mockGridDao.patchExists(sessionId, marker.patchId(), marker.s3Key())).thenReturn(true);
	}

	/**
	 * The marked patch is no longer stored, as after a migration.
	 */
	private void setupStaleMarker() {
		when(mockGridDao.getSnapshotEligibleMarker(sessionId)).thenReturn(Optional.of(marker));
		when(mockGridDao.patchExists(sessionId, marker.patchId(), marker.s3Key())).thenReturn(false);
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
	public void testRequestSnapshotIfNeededWithIntervalElapsedAndNewPatches() {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoNewPatches() {
		setupEligible();
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(0);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockClock, mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testRequestSnapshotIfNeededWithIntervalNotElapsedAndPatchesBelowThreshold() {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS - 1);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD - 1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore);
	}

	@Test
	public void testRequestSnapshotIfNeededWithPatchesAtThreshold() {
		setupEligible();
		setupSnapshot();
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock))
				.thenReturn(GridReplicaSnapshotManager.PATCH_COUNT_SNAPSHOT_THRESHOLD);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		// The patch count alone triggers the snapshot, whatever the snapshot's age
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndNewPatches() {
		setupEligible();
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		verifyNoInteractions(mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoSnapshotAndNoPatches() {
		setupEligible();
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(0);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testRequestSnapshotIfNeededWithNoMarker() {
		// A staging rebuild or a migrated change: this stack never saved a patch for the session
		when(mockGridDao.getSnapshotEligibleMarker(sessionId)).thenReturn(Optional.empty());

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockGridDao, never()).patchExists(any(), any(), any());
		verify(mockGridDao, never()).clearSnapshotEligibleMarker(any(), any());
		verify(mockGridDao, never()).getLatestSnapshot(any());
		verify(mockGridDao, never()).countMissingPatchesForClock(any(), any());
		verifyNoInteractions(mockExportPublisher, mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithStaleMarker() {
		setupStaleMarker();

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockGridDao).clearSnapshotEligibleMarker(sessionId, marker.s3Key());
		verify(mockGridDao, never()).getLatestSnapshot(any());
		verify(mockGridDao, never()).countMissingPatchesForClock(any(), any());
		verifyNoInteractions(mockExportPublisher, mockClock);
	}

	@Test
	public void testRequestSnapshotIfNeededWithValidMarker() {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);

		// call under test
		snapshotManager.requestSnapshotIfNeeded(connection);

		verify(mockExportPublisher).publishExportRequest(connection);
		verify(mockGridDao, never()).clearSnapshotEligibleMarker(any(), any());
	}

	@Test
	public void testExportSnapshotIfNeededWithIntervalElapsedAndNewPatches() throws Exception {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockGridIndexManager).exportSnapshot(sessionId, replicaId, tempFile.toPath());
		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
		verify(mockSnapshotStore).deleteSupersededSnapshots(sessionId,
				Instant.ofEpochMilli(snapshotCreatedOn + GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS
						- GridReplicaSnapshotManager.SUPERSEDED_SNAPSHOT_RETENTION_MS));
		verifyNoInteractions(mockExportPublisher);
	}

	@Test
	public void testExportSnapshotIfNeededWithStagingStack() throws Exception {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(false));

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		// Staging shares the snapshot bucket with production, so it never deletes
		verify(mockSnapshotStore, never()).deleteSupersededSnapshots(any(), any());
	}

	@Test
	public void testExportSnapshotIfNeededWithUndetectedStack() throws Exception {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.empty());

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		verify(mockSnapshotStore, never()).deleteSupersededSnapshots(any(), any());
	}

	@Test
	public void testExportSnapshotIfNeededWithDeleteFailure() throws Exception {
		setupEligible();
		setupSnapshot();
		setupSnapshotAge(GridReplicaSnapshotManager.SNAPSHOT_INTERVAL_MS);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(1);
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		when(mockSnapshotStore.deleteSupersededSnapshots(eq(sessionId), any(Instant.class)))
				.thenThrow(new RuntimeException("S3 unavailable"));

		// call under test: the saved snapshot stands and the next export retries the deletion
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
	}

	@Test
	public void testExportSnapshotIfNeededWithSnapshotSavedSinceRequest() {
		setupEligible();
		// A queued request whose snapshot an earlier export already took: few patches since a fresh snapshot
		setupSnapshot();
		setupSnapshotAge(1_000L);
		when(mockGridDao.countMissingPatchesForClock(sessionId, snapshotClock)).thenReturn(2);

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verifyNoInteractions(mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore,
				mockProdDetector);
	}

	@Test
	public void testExportSnapshotIfNeededWithMarkerStaleSinceRequest() {
		// A migration removed this stack's patch after the request was queued
		setupStaleMarker();

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockGridDao).clearSnapshotEligibleMarker(sessionId, marker.s3Key());
		verify(mockGridDao, never()).getLatestSnapshot(any());
		verify(mockGridDao, never()).countMissingPatchesForClock(any(), any());
		verifyNoInteractions(mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore,
				mockProdDetector);
	}

	@Test
	public void testExportSnapshotIfNeededWithNoMarker() {
		when(mockGridDao.getSnapshotEligibleMarker(sessionId)).thenReturn(Optional.empty());

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockGridDao, never()).countMissingPatchesForClock(any(), any());
		verifyNoInteractions(mockExportPublisher, mockFileProvider, mockGridIndexManager, mockSnapshotStore,
				mockProdDetector);
	}

	@Test
	public void testExportSnapshotIfNeededWithExportFailure() throws Exception {
		setupEligible();
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridDao.countMissingPatchesForClock(sessionId, List.of())).thenReturn(1);
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
		verifyNoInteractions(mockProdDetector);
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}
