package org.sagebionetworks.repo.manager.grid.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiAsyncClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionResponse;

@ExtendWith(MockitoExtension.class)
public class WebsocketGridEventPublishHandlerTest {

	@Mock
	private ApiGatewayManagementApiAsyncClient mockApiGatewayManagmentClient;
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
		PostToConnectionRequest expectedRequest = PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId(context.getConnectionId()).build();
		when(mockApiGatewayManagmentClient.postToConnection(expectedRequest))
				.thenReturn(CompletableFuture.completedFuture(PostToConnectionResponse.builder().build()));

		// call under test
		handler.publishEventResponse(context, body);

		verify(mockApiGatewayManagmentClient).postToConnection(expectedRequest);
		verifyNoMoreInteractions(mockGridManager);
	}

	@Test
	public void testPublishEventResponseWithGone() {
		PostToConnectionRequest expectedRequest = PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId(context.getConnectionId()).build();
		when(mockApiGatewayManagmentClient.postToConnection(expectedRequest))
				.thenReturn(CompletableFuture.failedFuture(GoneException.builder().build()));

		// call under test
		handler.publishEventResponse(context, body);

		// remove the connection info in the DB when the connection is lost.
		verify(mockGridManager).removeReplicaConnection(connectionId);
	}

	@Test
	public void testPublishEventResponseWithOtherException() {
		PostToConnectionRequest expectedRequest = PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId(context.getConnectionId()).build();
		when(mockApiGatewayManagmentClient.postToConnection(expectedRequest))
				.thenReturn(CompletableFuture.failedFuture(new RuntimeException("Some error")));

		// call under test
		assertThrows(RuntimeException.class, () -> handler.publishEventResponse(context, body));

		verifyNoMoreInteractions(mockGridManager);
	}

	@Test
	public void testPublishEventResponses() {
		EventContext context1 = new EventContext(eventType, eventSource, "conn1");
		EventContext context2 = new EventContext(eventType, eventSource, "conn2");
		List<EventContext> contexts = Arrays.asList(context1, context2);

		when(mockApiGatewayManagmentClient.postToConnection(any(PostToConnectionRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(PostToConnectionResponse.builder().build()));

		// call under test
		handler.publishEventResponses(contexts, body);

		verify(mockApiGatewayManagmentClient).postToConnection(PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId("conn1").build());
		verify(mockApiGatewayManagmentClient).postToConnection(PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId("conn2").build());
		verifyNoMoreInteractions(mockGridManager);
	}

	@Test
	public void testPublishEventResponsesWithEmptyList() {
		// call under test
		handler.publishEventResponses(Collections.emptyList(), body);

		verifyNoMoreInteractions(mockApiGatewayManagmentClient);
		verifyNoMoreInteractions(mockGridManager);
	}

	@Test
	public void testPublishEventResponsesWithGoneException() {
		EventContext context1 = new EventContext(eventType, eventSource, "conn1");
		EventContext context2 = new EventContext(eventType, eventSource, "conn2");
		List<EventContext> contexts = Arrays.asList(context1, context2);

		// First connection succeeds, second has GoneException
		when(mockApiGatewayManagmentClient.postToConnection(PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId("conn1").build()))
				.thenReturn(CompletableFuture.completedFuture(PostToConnectionResponse.builder().build()));
		when(mockApiGatewayManagmentClient.postToConnection(PostToConnectionRequest.builder()
				.data(SdkBytes.fromByteArray(body.getBytes(StandardCharsets.UTF_8)))
				.connectionId("conn2").build()))
				.thenReturn(CompletableFuture.failedFuture(GoneException.builder().build()));

		// call under test
		handler.publishEventResponses(contexts, body);

		// Only conn2 should be removed
		verify(mockGridManager).removeReplicaConnection("conn2");
	}
}
