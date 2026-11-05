package org.sagebionetworks.grid.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingCallable;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportWorkerTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridReplicaSnapshotManager mockSnapshotManager;
	@Mock
	private WriteReadSemaphore mockSemaphore;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private GridSnapshotExportWorker worker;

	private String connectionId;
	private GridConnectionInfo connection;
	private Message message;

	@BeforeEach
	public void before() {
		connectionId = "connection-one";
		connection = new GridConnectionInfo().setConnectionId(connectionId).setSessionId("123").setReplicaId(456L);
		message = new Message().withBody(connectionId);
	}

	@Test
	public void testRunWithConnection() throws Exception {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(120L);
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connection));
		WriteLockRequest expectedLockRequest = new WriteLockRequest(mockCallback, "gridSnapshotExport-connection-one",
				"gridSnapshotExport-123");
		doAnswer(invocation -> {
			ProgressingCallable<?> callable = invocation.getArgument(1);
			return callable.call(mockCallback);
		}).when(mockSemaphore).tryRunWithWriteLock(eq(expectedLockRequest), any());

		// call under test
		worker.run(mockCallback, message);

		verify(mockSnapshotManager).exportSnapshotIfNeeded(connection);
	}

	@Test
	public void testRunWithNoConnection() {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.empty());

		// call under test
		worker.run(mockCallback, message);

		verifyNoInteractions(mockSemaphore, mockSnapshotManager);
	}

	@Test
	public void testRunWithExportAlreadyRunning() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(120L);
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connection));
		doThrow(new LockUnavilableException(LockType.Write, "gridSnapshotExport-123", "other export"))
				.when(mockSemaphore).tryRunWithWriteLock(any(WriteLockRequest.class), any());

		// call under test
		worker.run(mockCallback, message);

		verifyNoInteractions(mockSnapshotManager);
	}
}
