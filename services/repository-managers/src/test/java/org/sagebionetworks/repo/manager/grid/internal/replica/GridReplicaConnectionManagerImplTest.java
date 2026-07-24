package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ExtendWith(MockitoExtension.class)
public class GridReplicaConnectionManagerImplTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private InternalReplicaToHubEventPublisher mockPublisher;
	@Mock
	private SnsClient mockSnsClient;
	@Mock
	private TransactionSynchronizationProxy mockTransactionSynchronizationProxy;
	@Captor
	private ArgumentCaptor<TransactionSynchronization> transactionSynchronizationCaptor;
	@Captor
	private ArgumentCaptor<EventContext> eventContextCaptor;

	@Spy
	@InjectMocks
	private GridReplicaConnectionManagerImpl manager;

	private String sessionId;
	private String topicArn;
	private Long userId;
	private GridReplica validationReplica;

	@BeforeEach
	public void before() {
		sessionId = GridUtils.gridSessionIdAsString(456L);
		topicArn = "arn:aws:sns:us-east-1:123456789012:test-topic";
		ReflectionTestUtils.setField(manager, "topicArn", topicArn);
		userId = 987L;
		validationReplica = new GridReplica().setReplicaId(222L);
	}

	@Test
	public void testCreateReplica() {
		when(mockGridDao.createReplica(userId, sessionId, false, EventSource.INTERNAL)).thenReturn(validationReplica);

		// call under test
		GridReplica result = manager.createReplica(userId, sessionId, false, EventSource.INTERNAL);

		assertEquals(validationReplica, result);
		verify(mockGridDao).createReplica(userId, sessionId, false, EventSource.INTERNAL);
		// a plain create must not publish a connect event
		verifyNoMoreInteractions(mockPublisher);
	}

	@Test
	public void testCreateReplicaWithNullUserId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createReplica(null, sessionId, false, EventSource.INTERNAL);
		}).getMessage();
		assertEquals("userId is required.", message);
		verifyNoMoreInteractions(mockGridDao, mockPublisher);
	}

	@Test
	public void testCreateReplicaAndConnect() {
		when(mockGridDao.createReplica(userId, sessionId, false, EventSource.VALIDATION)).thenReturn(validationReplica);

		// call under test
		GridReplica result = manager.createReplicaAndConnect(userId, sessionId, false, EventSource.VALIDATION);

		assertEquals(validationReplica, result);
		verify(mockGridDao).createReplica(userId, sessionId, false, EventSource.VALIDATION);
		verify(mockPublisher).publishEventAfterCommit(eventContextCaptor.capture(), eq(JsonRxMessageType.Notification),
				eq("connection"),
				eq(new Connection().setGridSessionId(GridUtils.gridSessionIdAsLong(sessionId))
						.setReplicaId(validationReplica.getReplicaId()).setUserId(userId)));
		EventContext capturedContext = eventContextCaptor.getValue();
		assertEquals(EventType.CONNECT, capturedContext.getEventType());
		assertEquals(EventSource.VALIDATION, capturedContext.getEventSource());
	}

	@Test
	public void testCreateReplicaAndConnectWithNullUserId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createReplicaAndConnect(null, sessionId, false, EventSource.VALIDATION);
		}).getMessage();
		assertEquals("userId is required.", message);
		verifyNoMoreInteractions(mockGridDao, mockPublisher);
	}

	@Test
	public void testPublishSchemaChangedEventWithActiveTransaction() {
		when(mockTransactionSynchronizationProxy.isSynchronizationActive()).thenReturn(true);
		doNothing().when(manager).sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));

		// call under test
		manager.publishSchemaChangedEvent(sessionId);

		// must not publish before the transaction commits
		verify(manager, never()).sendChangesToTopic(any());
		verify(mockTransactionSynchronizationProxy).registerSynchronization(transactionSynchronizationCaptor.capture());

		// simulate the transaction committing
		transactionSynchronizationCaptor.getValue().afterCommit();
		verify(manager).sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));
	}

	@Test
	public void testPublishSchemaChangedEventWithoutActiveTransaction() {
		when(mockTransactionSynchronizationProxy.isSynchronizationActive()).thenReturn(false);
		doNothing().when(manager).sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));

		// call under test
		manager.publishSchemaChangedEvent(sessionId);

		verify(manager).sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));
		verifyNoMoreInteractions(mockTransactionSynchronizationProxy);
	}

	@Test
	public void testPublishSchemaChangedEventWithNullSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.publishSchemaChangedEvent(null);
		}).getMessage();
		assertEquals("sessionId is required.", message);
		verifyNoMoreInteractions(mockTransactionSynchronizationProxy, mockSnsClient);
	}

	@Test
	public void testSendChangesToTopic() {
		// call under test
		manager.sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));
		verify(mockSnsClient).publish(PublishRequest.builder().targetArn(topicArn)
				.message(ReplicaChangeSet.fromSchemaChange(sessionId).toJson()).build());
	}
}
