package org.sagebionetworks.grid.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.sagebionetworks.grid.workers.message.JsonRxMessageBase;
import org.sagebionetworks.grid.workers.message.factory.JsonRxMessageFactory;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.grid.ErrorEvent;
import org.sagebionetworks.repo.model.grid.ErrorType;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.NotificationError;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

/**
 * This worker pulls in JSON-Rx message
 * (<a href="https://jsonjoy.com/specs/json-rx">JSON Reactive RPC</a>) from the
 * queue. Message can either be from a websocket or internal worker. Factory
 * methods are used to convert each message to an internal POJO.
 */
@Service
public class GridEventBrokerWorker implements MessageDrivenRunner {

	private static final Logger log = LogManager.getLogger(GridEventBrokerWorker.class);

	private final GridEventResponsePublisher publisher;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Map<JsonRxMessageType, JsonRxMessageFactory<?>> factoryTypeMap;

	public GridEventBrokerWorker(GridEventResponsePublisher publisher,
			ApplicationEventPublisher applicationEventPublisher, List<JsonRxMessageFactory<?>> builders) {
		super();
		this.publisher = publisher;
		this.applicationEventPublisher = applicationEventPublisher;
		this.factoryTypeMap = builders.stream()
				.collect(Collectors.toMap(JsonRxMessageFactory::type, handler -> handler));
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		log.info(message.getBody());
		EventContext context = null;
		try {
			context = buildEventContext(message);
			try {
				JSONArray eventBatch = new JSONArray(message.getBody());
				createEvents(context, eventBatch).forEach(e -> applicationEventPublisher.publishEvent(e));
			} catch (JSONException | IllegalArgumentException e) {
				log.error("Failed with bad request:", e);
				// Invalid message
				publisher.publishEventResponse(context,
						new NotificationError()
								.setEvent(new ErrorEvent().setMessage(e.getMessage()).setError(ErrorType.BAD_REQUEST))
								.toString());
			}
		} catch (Exception e) {
			log.error("Failed to process message:", e);
			if (context != null) {
				publisher.publishEventResponse(context,
						new NotificationError()
								.setEvent(new ErrorEvent().setMessage(e.getMessage()).setError(ErrorType.SERVER_ERROR))
								.toString());
			}
		}
	}

	/**
	 * Create an event for each JSON-Rx message found in the provided array. If the
	 * array is not a batch, it will be treated as a single message.
	 * 
	 * @param context
	 * @param array
	 * @return
	 */
	List<JsonRxMessageBase> createEvents(EventContext context, JSONArray array) {
		JSONArray child = array.optJSONArray(0);
		if (child != null) {
			List<JSONArray> arrays = new ArrayList<>(array.length());
			// batch of messages
			for (int i = 0; i < array.length(); i++) {
				JSONArray c = array.optJSONArray(i);
				if (c == null) {
					throw new IllegalArgumentException(
							String.format("The message at index: %d is not a JSON array", i));
				}
				arrays.add(c);
			}
			return arrays.stream().map(a -> createEvent(context, a)).collect(Collectors.toList());
		}
		// Not a batch.
		return Collections.singletonList(createEvent(context, array));
	}

	/**
	 * Translate a single JSON-Rx message to an internal event.
	 * 
	 * @param context
	 * @param array
	 * @return
	 */
	JsonRxMessageBase createEvent(EventContext context, JSONArray array) {
		org.sagebionetworks.repo.model.grid.message.JsonRxMessage message = new org.sagebionetworks.repo.model.grid.message.JsonRxMessage(
				array);
		JsonRxMessageFactory<?> factory = factoryTypeMap.get(message.getType());
		if (factory == null) {
			throw new IllegalArgumentException(String.format("Unknown message type -- code: %d and method: '%s'",
					message.getType().getCode(), message.getMethod().orElse("")));
		}
		return factory.createMessage(context, message.getId().orElse(null), message.getMethod().orElse(null),
				message.getBody().orElse(null));
	}

	/**
	 * Extract the message context from the message attributes.
	 * 
	 * @param message
	 * @return
	 */
	static EventContext buildEventContext(Message message) {
		try {
			ValidateArgument.required(message, "message");
			Map<String, MessageAttributeValue> attributes = message.getMessageAttributes();
			MessageAttributeValue eventTypeString = attributes.get("EventType");
			ValidateArgument.required(eventTypeString, "attribute.EventType");
			EventType eventType = EventType.valueOf(eventTypeString.getStringValue());
			MessageAttributeValue eventSourceString = attributes.get("EventSource");
			ValidateArgument.required(eventSourceString, "attribute.EventSource");
			EventSource eventSource = EventSource.valueOf(eventSourceString.getStringValue());
			MessageAttributeValue convalue = attributes.get("ConnectionId");
			ValidateArgument.required(convalue, "attribute.ConnectionId");
			return new EventContext(eventType, eventSource, convalue.getStringValue());
		} catch (IllegalArgumentException e) {
			// Any IllegalArgumentException in this context is a server-side issue.
			throw new IllegalStateException(e.getMessage());
		}
	}

	@Override
	public List<String> getMessageAttributeNames() {
		return Collections.singletonList(".*");
	}

}
