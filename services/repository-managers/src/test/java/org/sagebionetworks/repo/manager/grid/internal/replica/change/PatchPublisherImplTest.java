package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
public class PatchPublisherImplTest {

	@Mock
	private InternalReplicaToHubEventPublisher mockEventPublisher;

	@Spy
	@InjectMocks
	private PatchPublisherImpl publisher;

	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private EventSource eventSource;

	private GridConnectionInfo con;
	private JSONArray patchBody;

	@BeforeEach
	public void before() {
		connectionId = "con123";
		replicaId = 3L;
		sessionId = "session34";
		eventSource = EventSource.VALIDATION;
		con = new GridConnectionInfo().setConnectionId(connectionId).setSessionId(sessionId).setReplicaId(replicaId)
				.setSource(eventSource);
		patchBody = PatchCompactSerializable.serialize(new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(5L)).setOperations(List.of()));
	}

	@Test
	public void testPublishPatch() {
		// call under test
		publisher.publishPatch(con, patchBody, 101L);
		verify(mockEventPublisher).publishEvent(new EventContext(EventType.MESSAGE, eventSource, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setId(1010).setMethod("patch").setBody(patchBody));
	}

}
