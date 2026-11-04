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
	private List<LogicalTimestamp> replicaClock;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		replicaId = 111L;
		connection = new GridConnectionInfo().setReplicaId(replicaId).setSessionId(sessionId);
		replicaClock = List.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L));
	}

	private File setupExport(ClockTable exportedClockTable) throws Exception {
		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenReturn(exportedClockTable);
		return tempFile;
	}

	@Test
	public void testExportSnapshotWithReplicaAheadOfLatestSnapshot() throws Exception {
		connection.setCreatedBy(789L);
		List<LogicalTimestamp> latestSnapshotClock = List
				.of(new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(100L));
		when(mockGridDao.getLatestSnapshot(sessionId))
				.thenReturn(Optional.of(new GridSnapshot().setClockTable(new ClockTable(latestSnapshotClock))));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		ClockTable exportedClockTable = new ClockTable(replicaClock);
		File tempFile = setupExport(exportedClockTable);

		// call under test
		snapshotManager.exportSnapshot(connection);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(exportedClockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testExportSnapshotWithNoSnapshot() throws Exception {
		connection.setCreatedBy(789L);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		ClockTable exportedClockTable = new ClockTable(replicaClock);
		File tempFile = setupExport(exportedClockTable);

		// call under test
		snapshotManager.exportSnapshot(connection);

		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), eq(exportedClockTable), eq(789L), any(File.class));
		assertFalse(tempFile.exists(), "Temp file should be deleted after successful export");
	}

	@Test
	public void testExportSnapshotWithLatestSnapshotAtReplicaClock() {
		LogicalTimestamp first = new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(5000L);
		LogicalTimestamp second = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(12L);
		// The stored snapshot clock can list the same entries in a different order.
		when(mockGridDao.getLatestSnapshot(sessionId))
				.thenReturn(Optional.of(new GridSnapshot().setClockTable(new ClockTable(List.of(second, first)))));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(List.of(first, second));

		// call under test
		snapshotManager.exportSnapshot(connection);

		verify(mockGridIndexManager, never()).exportSnapshot(any(), any(), any());
		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
	}

	@Test
	public void testExportSnapshotWithExportFailure() throws Exception {
		connection.setCreatedBy(789L);
		when(mockGridDao.getLatestSnapshot(sessionId)).thenReturn(Optional.empty());
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(replicaClock);
		File tempFile = File.createTempFile("test-", ".cbor");
		tempFile.deleteOnExit();
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockGridIndexManager.exportSnapshot(eq(sessionId), eq(replicaId), any(Path.class)))
				.thenThrow(new RuntimeException("Export failed"));

		assertThrows(RuntimeException.class, () -> {
			// call under test
			snapshotManager.exportSnapshot(connection);
		});

		verify(mockSnapshotStore, never()).saveSnapshot(any(), any(), any(), any());
		assertFalse(tempFile.exists(), "Temp file should be deleted after failure");
	}
}
