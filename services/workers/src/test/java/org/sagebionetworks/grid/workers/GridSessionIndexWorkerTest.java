package org.sagebionetworks.grid.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class GridSessionIndexWorkerTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridEventResponsePublisher mockPublisher;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private GridSessionIndexWorker worker;

	private Long gridSessionIdLong;
	private String gridSessionId;
	private String connectionId;
	private ChangeMessage message;

	@BeforeEach
	public void setUp() {
		gridSessionIdLong = 456L;
		gridSessionId = GridUtils.gridSessionIdAsString(gridSessionIdLong);
		connectionId = "conn-123";
		message = new ChangeMessage();
		message.setObjectType(ObjectType.GRID_SESSION);
		message.setObjectId(gridSessionIdLong.toString());
		message.setChangeType(ChangeType.UPDATE);
	}

	@Test
	public void testRun() throws Exception {
		GridConnectionInfo connectionInfo = new GridConnectionInfo()
				.setSessionId(gridSessionId)
				.setConnectionId(connectionId);
		when(mockGridManager.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(connectionInfo));

		// call under test
		worker.run(mockCallback, message);

		verify(mockGridManager).getSingletonConnection(gridSessionId, EventSource.INTERNAL);
		verify(mockPublisher).publishEventResponses(
				eq(List.of(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId))),
				eq(JsonRxMessageType.Notification),
				eq("new-patch"));
	}

	@Test
	public void testRunWithNoConnection() throws Exception {
		when(mockGridManager.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.empty());

		// call under test
		worker.run(mockCallback, message);

		verify(mockGridManager).getSingletonConnection(gridSessionId, EventSource.INTERNAL);
		verifyZeroInteractions(mockPublisher);
	}

	@Test
	public void testRunWithWrongObjectType() throws Exception {
		message.setObjectType(ObjectType.ENTITY);

		// call under test
		worker.run(mockCallback, message);

		verifyZeroInteractions(mockGridManager, mockPublisher);
	}
}
