package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSnapshotManager;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportWorkerTest {

	@Mock
	private GridReplicaSnapshotManager mockSnapshotManager;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private GridSnapshotExportWorker worker;

	private String sessionId;
	private Message message;

	@BeforeEach
	public void before() {
		sessionId = "123";
		message = new Message().withBody(sessionId);
	}

	@Test
	public void testRun() {
		// call under test
		worker.run(mockCallback, message);

		verify(mockSnapshotManager).exportSnapshotIfNeeded(sessionId);
	}

	@Test
	public void testRunWithRecoverableException() {
		RecoverableMessageException exception = new RecoverableMessageException("Replica behind");
		doThrow(exception).when(mockSnapshotManager).exportSnapshotIfNeeded(sessionId);

		// The message goes back on the queue to be retried
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockCallback, message);
		});
		assertSame(exception, thrown);
	}

	@Test
	public void testRunWithOtherException() {
		doThrow(new IllegalStateException("Export failed")).when(mockSnapshotManager).exportSnapshotIfNeeded(sessionId);

		// call under test: the failure is logged and the message consumed
		worker.run(mockCallback, message);

		verify(mockSnapshotManager).exportSnapshotIfNeeded(sessionId);
	}

	@Test
	public void testRunWithNoBody() {
		message = new Message();
		doThrow(new IllegalArgumentException("sessionId is required.")).when(mockSnapshotManager)
				.exportSnapshotIfNeeded(null);

		// call under test: an unparseable message is logged and consumed
		worker.run(mockCallback, message);

		verify(mockSnapshotManager).exportSnapshotIfNeeded(null);
	}
}
