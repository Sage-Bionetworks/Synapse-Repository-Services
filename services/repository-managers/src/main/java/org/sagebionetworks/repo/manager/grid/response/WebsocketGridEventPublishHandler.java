package org.sagebionetworks.repo.manager.grid.response;

import java.nio.charset.StandardCharsets;

import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;

@Service
public class WebsocketGridEventPublishHandler implements GridEventResponsePublishHandler {

	private final ApiGatewayManagementApiClient apiGatewayManagmentClient;
	private final GridManager gridManager;

	public WebsocketGridEventPublishHandler(ApiGatewayManagementApiClient apiGatewayManagmentClient,
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
					.connectionId(context.getConnectionId()).build());
		} catch (GoneException e) {
			gridManager.removeReplicaConnection(context.getConnectionId());
		}
	}

}
