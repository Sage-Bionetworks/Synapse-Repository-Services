package org.sagebionetworks.repo.manager.grid.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;

@ExtendWith(MockitoExtension.class)
public class WebsocketGridEventPublishHandlerTest {

	@Mock
	private ApiGatewayManagementApiClient mockApiGatewayManagmentClient;
	@Mock
	private GridManager mockGridManager;
	@InjectMocks
	private WebsocketGridEventPublishHandler handler;
	private EventType eventType;
	private EventSource eventSource;
	private String connectionId;
	private EventContext context;
	private String body;

	@BeforeEach
	public void before() {
		eventType = EventType.CONNECT;
		eventSource = EventSource.WEBSOCKET;
		connectionId = "con123";
		context = new EventContext(eventType, eventSource, connectionId);
		body = "[8,1]";
	}

	@Test
	public void testGetEventSource() {
		assertEquals(EventSource.WEBSOCKET, handler.getEventSource());
	}

	@Test
	public void testPublishEventResponse() {

		// call under test
		handler.publishEventResponse(context, body);

		verify(mockApiGatewayManagmentClient).postToConnection(
				PostToConnectionRequest.builder().data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
						.connectionId(context.getConnectionId()).build());
		verifyZeroInteractions(mockGridManager);
	}

	@Test
	public void testPublishEventResponseWithGone() {
		var builder = PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId(context.getConnectionId()).build();
		doThrow(GoneException.builder().build()).when(mockApiGatewayManagmentClient).postToConnection(builder);

		// call under test
		handler.publishEventResponse(context, body);

		// remove the connection info in the DB when the connection is lost.
		verify(mockGridManager).removeReplicaConnection(connectionId);

	}
}
