package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.ReplicaChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridReplicaValidationManager;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class GridReplicaValidationWorkerTest {

	@Mock
	private GridReplicaValidationManager mockValidationManager;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private GridReplicaValidationWorker worker;

	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private GridConnectionInfo connectionInfo;
	private LogicalTimestamp vectorId;
	private ReplicaChangeSet changeSet;
	private Message messageFromTopic;
	private Message message;

	@BeforeEach
	public void before() {
		sessionId = "session434";
		replicaId = 2L;
		connectionId = "con123";
		connectionInfo = new GridConnectionInfo().setConnectionId(connectionId).setReplicaId(replicaId)
				.setSessionId(sessionId);
		vectorId = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L);
		Map<IndexType, Set<LogicalTimestamp>> changes = Map.of(IndexType.arr,
				Set.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)), IndexType.vec, Set.of(vectorId));
		changeSet = new ReplicaChangeSet(connectionInfo, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				changes);
		message = new Message().withBody(changeSet.toJson());
		JSONObject topicBody = new JSONObject();
		topicBody.put("TopicArn", "topic-arn");
		topicBody.put("Message", changeSet.toJson());
		messageFromTopic = new Message().withBody(topicBody.toString());
	}

	@Test
	public void testRunWithMessage() throws RecoverableMessageException, Exception {

		// call under test
		worker.run(mockCallback, message);
		verify(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));
	}

	@Test
	public void testRunWithMessageFromTopic() throws RecoverableMessageException, Exception {

		// call under test
		worker.run(mockCallback, messageFromTopic);
		verify(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));
	}

	@Test
	public void testRunWithNoVectorChanges() throws RecoverableMessageException, Exception {
		changeSet = new ReplicaChangeSet(connectionInfo, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				Map.of(IndexType.arr, Set.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))));
		message = new Message().withBody(changeSet.toJson());
		// call under test
		worker.run(mockCallback, message);
		verifyZeroInteractions(mockValidationManager);
	}

	@Test
	public void testRunWithNoChanges() throws RecoverableMessageException, Exception {
		changeSet = new ReplicaChangeSet(connectionInfo, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				Collections.emptyMap());
		message = new Message().withBody(changeSet.toJson());
		// call under test
		worker.run(mockCallback, message);
		verifyZeroInteractions(mockValidationManager);
	}

	@Test
	public void testRunWithNullChanges() throws RecoverableMessageException, Exception {
		changeSet = new ReplicaChangeSet(connectionInfo, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				null);
		message = new Message().withBody(changeSet.toJson());
		// call under test
		worker.run(mockCallback, message);
		verifyZeroInteractions(mockValidationManager);
	}

	@Test
	public void testRunWithMessageWithRecoverableException() throws RecoverableMessageException, Exception {
		RecoverableMessageException e = new RecoverableMessageException("now now");
		doThrow(e).when(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));

		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockCallback, message);
		});
		assertEquals(e, thrown);

		verify(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));
	}

	@Test
	public void testRunWithMessageWithLockUnavailableException() throws RecoverableMessageException, Exception {
		LockUnavilableException e = new LockUnavilableException(LockType.Write, "key", "context");
		doThrow(e).when(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));

		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockCallback, message);
		});
		assertEquals(e, thrown.getCause());

		verify(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));
	}

	@Test
	public void testRunWithMessageWithException() throws RecoverableMessageException, Exception {
		IllegalArgumentException e = new IllegalArgumentException("not");
		doThrow(e).when(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));

		// call under test
		worker.run(mockCallback, message);
		verify(mockValidationManager).validateChanges(sessionId, replicaId, Set.of(vectorId));
	}
}
