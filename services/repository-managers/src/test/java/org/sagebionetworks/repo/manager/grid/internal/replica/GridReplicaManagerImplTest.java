package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ExtendWith(MockitoExtension.class)
public class GridReplicaManagerImplTest {

	@Mock
	private GridIndexManager mockGridIndexManager;
	@Mock
	private InternalReplicaToHubEventPublisher mockPublisher;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	SnsClient mockSnsClient;
	@Captor
	ArgumentCaptor<WriteLockRequest> writeLockRequestCaptor;

	private GridConnectionInfo connection;
	private String sessionId;
	private String connectionId;
	private Long replicaId;
	private Integer methodId;
	private List<LogicalTimestamp> clock;
	private Patch patch;
	private String topicArn;
	private Map<IndexType, Set<LogicalTimestamp>> changes;

	@BeforeEach
	public void before() {
		sessionId = "session456";
		connectionId = "con123";
		replicaId = 111L;
		methodId = 444;
		connection = new GridConnectionInfo().setConnectionId(connectionId).setReplicaId(replicaId)
				.setSessionId(sessionId);
		clock = List.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(99L));
		patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))
				.setOperations(List.of());

		topicArn = "arn:aws:sns:us-east-1:123456789012:test-topic";

		// Set the gridReplicaChangeTopicArn field value
		ReflectionTestUtils.setField(manager, "topicArn", topicArn);

		changes = Map.of(IndexType.arr, Set.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(55L)));
	}

	@Spy
	@InjectMocks
	private GridReplicaManagerImpl manager;

	@Test
	public void testOnConnect() {
		doNothing().when(manager).synchronizeClock(mockCallback, connection);
		// call under test
		manager.onConnected(mockCallback, connection);
	}

	@Test
	public void testOnNewPatch() {
		doNothing().when(manager).synchronizeClock(mockCallback, connection);
		// call under test
		manager.onNewPatch(mockCallback, connection);
	}

	@Test
	public void testOnResponseComplete() {
		// call under test
		manager.onResponseComplete(mockCallback, connection, methodId);
		verify(mockGridIndexManager).completeMessageChain(sessionId, replicaId, methodId);
	}

	@Test
	public void testSendClockMessage() {
		// call under test
		manager.sendClockMessage(methodId, connectionId, clock);
		verify(mockPublisher).publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK)
						.setId(methodId).setBody(LogicalTimestampCompactSerializable.serializeClock(clock)));
	}

	@Test
	public void testOnApplyPatch() {
		when(mockGridIndexManager.applyPatch(sessionId, replicaId, patch)).thenReturn(changes);
		doNothing().when(manager).sendChangesToTopic(connection, patch.getPatchId(), changes);

		// Mock the gridIndexManager calls
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(clock);
		doNothing().when(manager).sendClockMessage(methodId, connectionId, clock);

		// call under test
		manager.onApplyPatch(mockCallback, connection, methodId, patch);

		// verify interactions
		verify(mockGridIndexManager).applyPatch(sessionId, replicaId, patch);
		verify(mockGridIndexManager).getClock(sessionId, replicaId);
		verify(manager).sendClockMessage(methodId, connectionId, clock);
		verify(mockGridIndexManager).refreshMessageChain(sessionId, replicaId, methodId);
	}

	@Test
	public void testSynchronizeClock() {
		when(mockGridIndexManager.getNonExpiredMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK))
				.thenReturn(Optional.empty());
		when(mockGridIndexManager.startMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK))
				.thenReturn(new MessageChain().setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK).setId(methodId));
		when(mockGridIndexManager.getClock(sessionId, replicaId)).thenReturn(clock);

		// call under test
		manager.synchronizeClock(mockCallback, connection);

		verify(mockGridIndexManager).startMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK);
		verify(mockGridIndexManager).getClock(sessionId, replicaId);
		verify(mockPublisher).publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setMethod(GridReplicaManager.SYNCHRONIZE_CLOCK)
						.setId(methodId).setBody(LogicalTimestampCompactSerializable.serializeClock(clock)));
	}
	
	@Test
	public void testSynchronizeClockWithNonExpiredMessageChain() {
		when(mockGridIndexManager.getNonExpiredMessageChain(sessionId, replicaId, GridReplicaManager.SYNCHRONIZE_CLOCK))
				.thenReturn(Optional.of(new MessageChain().setId(123)));
		// call under test
		manager.synchronizeClock(mockCallback, connection);
		
		verifyNoMoreInteractions(mockGridIndexManager, mockPublisher);
	}

	@Test
	public void testSendChangesToTopic() {
		// call under test
		manager.sendChangesToTopic(connection, patch.getPatchId(), changes);
		verify(mockSnsClient).publish(PublishRequest.builder().targetArn(topicArn)
				.message("{\"sessionId\":\"session456\",\"replicaId\":111,\"patchId\":[3,4],"
						+ "\"changes\":{\"arr\":[[111,55]]}}")
				.build());
	}

}
