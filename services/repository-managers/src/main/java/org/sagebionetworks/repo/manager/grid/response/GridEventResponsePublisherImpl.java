package org.sagebionetworks.repo.manager.grid.response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridEventResponsePublisherImpl implements GridEventResponsePublisher {

	private Map<EventSource, GridEventResponsePublishHandler> handlerMap;

	public GridEventResponsePublisherImpl(List<GridEventResponsePublishHandler> handlers) {
		handlerMap = handlers.stream()
				.collect(Collectors.toMap(GridEventResponsePublishHandler::getEventSource, handler -> handler));
	}

	@Override
	public void publishEventResponse(EventContext context, String event) {
		ValidateArgument.required(context, "context");
		GridEventResponsePublishHandler handler = handlerMap.get(context.getEventSource());
		if (handler == null) {
			throw new IllegalStateException("No handler found for: " + context.getEventSource());
		}
		handler.publishEventResponse(context, event);
	}

	@Override
	public void publishEventResponse(EventContext context, JsonRxMessageType type, String method) {
		String message = String.format("[%d,\"%s\"]", type.getCode(), method);
		publishEventResponse(context, message);
	}

	@Override
	public void publishEventResponse(EventContext context, JsonRxMessageType type, int requestId) {
		String message = String.format("[%d,%d]", type.getCode(), requestId);
		publishEventResponse(context, message);
	}

	@Override
	public void publishEventResponse(EventContext context, JsonRxMessageType type, int requestId, String payload) {
		String message = String.format("[%d,%d,%s]", type.getCode(), requestId, payload);
		publishEventResponse(context, message);
	}

}
