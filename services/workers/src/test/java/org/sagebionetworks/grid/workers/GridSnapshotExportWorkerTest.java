package org.sagebionetworks.grid.workers;

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

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportWorkerTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridReplicaSnapshotManager mockSnapshotManager;
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
	public void testRunWithConnection() {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connection));

		// call under test
		worker.run(mockCallback, message);

		verify(mockSnapshotManager).exportSnapshotIfNeeded(connection);
	}

	@Test
	public void testRunWithNoConnection() {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.empty());

		// call under test
		worker.run(mockCallback, message);

		verifyNoInteractions(mockSnapshotManager);
	}
}
