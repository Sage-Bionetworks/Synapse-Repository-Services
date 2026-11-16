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
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
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
	private GridSnapshotRequester mockSnapshotRequester;
	@Mock
	private ProdDetector mockProdDetector;

	@InjectMocks
	private GridReplicaSnapshotManager snapshotManager;

	private GridConnectionInfo connection;
	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> replicaClock;
	private long now;
	private ClockTable exportedClockTable;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId).setCreatedBy(789L);
		replicaClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L));
		now = 1_700_000_000_000L;
		exportedClockTable = new ClockTable(
				List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L)));
	}

	private void setupReadyReplica() {
		when(mockSnapshotRequester.isSnapshotNeeded(sessionId)).thenReturn(true);
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
	public void testExportSnapshotIfNeeded() throws Exception {
		setupReadyReplica();
		File tempFile = setupExport();
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));
		when(mockClock.currentTimeMillis()).thenReturn(now);

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockGridIndexManager).exportSnapshot(sessionId, replicaId, tempFile.toPath());
		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
		verify(mockSnapshotStore).deleteSupersededSnapshots(sessionId,
				Instant.ofEpochMilli(now - GridReplicaSnapshotManager.SUPERSEDED_SNAPSHOT_RETENTION_MS));
	}

	@Test
	public void testExportSnapshotIfNeededWithReplicaBehind() {
		when(mockSnapshotRequester.isSnapshotNeeded(sessionId)).thenReturn(true);
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		when(mockGridDao.countMissingPatchesForClock(sessionId, replicaClock)).thenReturn(1);

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			snapshotManager.exportSnapshotIfNeeded(connection);
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
		snapshotManager.exportSnapshotIfNeeded(connection);

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
		snapshotManager.exportSnapshotIfNeeded(connection);

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
		snapshotManager.exportSnapshotIfNeeded(connection);

		verify(mockSnapshotStore).saveSnapshot(sessionId, exportedClockTable, 789L, tempFile);
	}

	@Test
	public void testExportSnapshotIfNeededWithSnapshotNotNeeded() {
		// For example, a queued request whose snapshot an earlier export already took
		when(mockSnapshotRequester.isSnapshotNeeded(sessionId)).thenReturn(false);

		// call under test
		snapshotManager.exportSnapshotIfNeeded(connection);

		verifyNoInteractions(mockFileProvider, mockGridIndexManager, mockSnapshotStore, mockProdDetector, mockGridDao);
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
			snapshotManager.exportSnapshotIfNeeded(connection);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		verifyNoInteractions(mockProdDetector);
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}
