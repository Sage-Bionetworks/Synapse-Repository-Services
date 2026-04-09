package org.sagebionetworks.repo.manager.grid.response;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiAsyncClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionResponse;

@Service
public class WebsocketGridEventPublishHandler implements GridEventResponsePublishHandler {

	private final ApiGatewayManagementApiAsyncClient apiGatewayManagmentClient;
	private final GridManager gridManager;

	public WebsocketGridEventPublishHandler(ApiGatewayManagementApiAsyncClient apiGatewayManagmentClient,
			GridManager gridManager) {
		super();
		this.apiGatewayManagmentClient = apiGatewayManagmentClient;
		this.gridManager = gridManager;
	}

	@Override
	public EventSource getEventSource() {
		return EventSource.WEBSOCKET;
	}

	@Override
	public void publishEventResponse(EventContext context, String event) {
		ValidateArgument.required(context, "context");
		try {
			apiGatewayManagmentClient.postToConnection(PostToConnectionRequest.builder()
					.data(SdkBytes.fromByteArray(event.getBytes(StandardCharsets.UTF_8)))
					.connectionId(context.getConnectionId()).build())
				.join();
		} catch (CompletionException e) {
			handleException(context, e);
		}
	}

	@Override
	public void publishEventResponses(List<EventContext> contexts, String event) {
		ValidateArgument.required(contexts, "contexts");
		ValidateArgument.required(event, "event");
		
		if (contexts.isEmpty()) {
			return;
		}
		
		SdkBytes data = SdkBytes.fromByteArray(event.getBytes(StandardCharsets.UTF_8));
		
		// Fire all async requests in parallel, each with its own error handling
		CompletableFuture<?>[] futures = contexts.stream()
			.map(context -> {
				return apiGatewayManagmentClient.postToConnection(
						PostToConnectionRequest.builder()
							.data(data)
							.connectionId(context.getConnectionId())
							.build())
					.handle((PostToConnectionResponse response, Throwable throwable) -> {
						if (throwable != null) {
							handleException(context, throwable);
						}
						return null;
					});
			})
			.toArray(CompletableFuture[]::new);
		
		// Wait for all to complete
		CompletableFuture.allOf(futures).join();
	}

	/**
	 * Handle exceptions from async post operations. GoneException indicates the
	 * connection is no longer valid and should be removed. Other exceptions are
	 * logged but do not propagate (to allow batch operations to continue).
	 */
	private void handleException(EventContext context, Throwable throwable) {
		Throwable cause = throwable;
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			cause = throwable.getCause();
		}
		if (cause instanceof GoneException) {
			gridManager.removeReplicaConnection(context.getConnectionId());
		} else {
			throw new RuntimeException("Failed to post to connection: " + context.getConnectionId(), throwable);
		}
	}

}
