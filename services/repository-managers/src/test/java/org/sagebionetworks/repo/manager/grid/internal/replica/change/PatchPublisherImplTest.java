package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class PatchPublisherImplTest {

	@Mock
	private InternalReplicaToHubEventPublisher mockEventPublisher;
	@Mock
	private GridIndexManager mockGridIndexManager;
	@Mock
	private Clock mockClock;

	@Spy
	@InjectMocks
	private PatchPublisherImpl publisher;

	private String sessionId;
	private Long replicaId;
	private String connectionId;

	private GridConnectionInfo con;
	private JSONArray patchBody;

	@BeforeEach
	public void before() {
		connectionId = "con123";
		replicaId = 3L;
		sessionId = "session34";
		con = new GridConnectionInfo().setConnectionId(connectionId).setSessionId(sessionId).setReplicaId(replicaId);
		patchBody = PatchCompactSerializable.serialize(new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(5L)).setOperations(List.of()));
	}

	@Test
	public void testPublishPatch() {
		MessageChain chain = new MessageChain().setId(99).setMethod("patch");
		when(mockGridIndexManager.startMessageChain(sessionId, replicaId, "patch")).thenReturn(chain);
		doNothing().when(publisher).waitForPatchToBeAccepted(chain);
		// call under test
		publisher.publishPatch(con, patchBody);
		verify(mockEventPublisher).publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setId(99).setMethod("patch").setBody(patchBody));
	}

	@Test
	public void testWaitForPatchToBeAccepted() throws InterruptedException {
		MessageChain chain = new MessageChain().setId(99).setSessionId(sessionId).setReplicaId(replicaId);
		when(mockGridIndexManager.getMessageChain(sessionId, replicaId, chain.getId())).thenReturn(Optional.of(chain),
				Optional.of(chain), Optional.empty());

		long startTime = 1000L;
		when(mockClock.currentTimeMillis()).thenReturn(startTime);

		// call under test
		publisher.waitForPatchToBeAccepted(chain);

		verify(mockClock, times(2)).sleep(anyLong());
		// exponential back-off
		verify(mockClock).sleep(50L);
		verify(mockClock).sleep(100L);
		verify(mockGridIndexManager, times(3)).getMessageChain(sessionId, replicaId, chain.getId());
	}

	@Test
	public void testWaitForPatchToBeAcceptedWithOverLimit() throws InterruptedException {
		MessageChain chain = new MessageChain().setId(99).setSessionId(sessionId).setReplicaId(replicaId);
		when(mockGridIndexManager.getMessageChain(sessionId, replicaId, chain.getId())).thenReturn(Optional.of(chain));

		when(mockClock.currentTimeMillis()).thenReturn(0L, PatchPublisherImpl.MAX_WAIT_MS,
				PatchPublisherImpl.MAX_WAIT_MS + 10L);

		String message = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			publisher.waitForPatchToBeAccepted(chain);
		}).getMessage();
		assertEquals(
				"Timed out waiting for a patch to be accepted for: "
						+ "MessageChain [sessionId=session34, replicaId=3, id=99, method=null, createdOn=null]",
				message);

		verify(mockClock, times(2)).sleep(anyLong());
		// exponential back-off
		verify(mockClock).sleep(50L);
		verify(mockClock).sleep(100L);
		verify(mockGridIndexManager, times(2)).getMessageChain(sessionId, replicaId, chain.getId());
	}

	@Test
	public void testWaitForPatchToBeAcceptedWithInterupt() throws InterruptedException {
		MessageChain chain = new MessageChain().setId(99).setSessionId(sessionId).setReplicaId(replicaId);
		when(mockGridIndexManager.getMessageChain(sessionId, replicaId, chain.getId())).thenReturn(Optional.of(chain));

		when(mockClock.currentTimeMillis()).thenReturn(0L, PatchPublisherImpl.MAX_WAIT_MS,
				PatchPublisherImpl.MAX_WAIT_MS + 10L);
		InterruptedException e = new InterruptedException("Interrupt");
		doThrow(e).when(mockClock).sleep(50L);

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			// call under test
			publisher.waitForPatchToBeAccepted(chain);
		});
		assertEquals(e, thrown.getCause());

		verify(mockClock, times(1)).sleep(anyLong());
		// exponential back-off
		verify(mockClock).sleep(50L);
		verify(mockGridIndexManager, times(1)).getMessageChain(sessionId, replicaId, chain.getId());
	}
}
