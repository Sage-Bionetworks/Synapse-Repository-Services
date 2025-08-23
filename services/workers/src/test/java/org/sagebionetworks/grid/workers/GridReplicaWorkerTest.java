package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.InternalMessageDispatcher;
import org.sagebionetworks.repo.manager.grid.internal.replica.JsonRxMessageBundle;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

@ExtendWith(MockitoExtension.class)
public class GridReplicaWorkerTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private InternalMessageDispatcher mockDispatcher;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private GridReplicaWorker worker;

	private String connectionId;
	private String messageBody;
	private Message message;
	private GridConnectionInfo connectionInfo;

	@BeforeEach
	public void before() {
		connectionId = "connection123";
		messageBody = "[8,\"pong\"]";
		message = new Message().withBody(messageBody).withMessageAttributes(
				Map.of("ConnectionId", new MessageAttributeValue().withStringValue(connectionId)));
		connectionInfo = new GridConnectionInfo().setConnectionId(connectionId);
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connectionInfo));
		// call under test
		worker.run(mockCallback, message);
		verify(mockDispatcher)
				.dispatchMessage(new JsonRxMessageBundle(new JsonRxMessage(messageBody), connectionInfo, mockCallback));
	}

	@Test
	public void testRunWithNoConnection() throws RecoverableMessageException, Exception {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.empty());
		// call under test
		worker.run(mockCallback, message);
		verifyZeroInteractions(mockDispatcher);
	}

	@Test
	public void testRunWithRecoverableException() {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connectionInfo));
		RecoverableMessageException e = new RecoverableMessageException("not now");
		doThrow(e).when(mockDispatcher).dispatchMessage(any());
		RecoverableMessageException th = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockCallback, message);
		});
		assertEquals(e, th);
	}

	@Test
	public void testRunWithLockUnavailableException() {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connectionInfo));
		LockUnavilableException e = new LockUnavilableException(LockType.Write, "not now", connectionId);
		doThrow(e).when(mockDispatcher).dispatchMessage(any());
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockCallback, message);
		});
		assertEquals(e, thrown.getCause());
	}

	@Test
	public void testRunWithOtherException() throws RecoverableMessageException, Exception {
		when(mockGridManager.getConnectionInfoOptional(connectionId)).thenReturn(Optional.of(connectionInfo));
		IllegalArgumentException e = new IllegalArgumentException("not null");
		doThrow(e).when(mockDispatcher).dispatchMessage(any());
		// call under test -- the exception is logged.
		worker.run(mockCallback, message);
	}

}
