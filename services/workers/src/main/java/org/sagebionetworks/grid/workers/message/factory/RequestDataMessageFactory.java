package org.sagebionetworks.grid.workers.message.factory;

import org.json.JSONArray;
import org.sagebionetworks.grid.workers.message.NewPatchRegistrationMessage;
import org.sagebionetworks.grid.workers.message.RequestDataMessage;
import org.sagebionetworks.grid.workers.message.SynchronizeClockMessage;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.event.JsonRxMessageType;
import org.springframework.stereotype.Component;

@Component
public class RequestDataMessageFactory implements JsonRxMessageFactory<RequestDataMessage> {

	@Override
	public JsonRxMessageType type() {
		return JsonRxMessageType.RequestData;
	}

	@Override
	public RequestDataMessage createMessage(EventContext context, Integer id, String method, Object body) {
		if ("patch".equals(method)) {
			return new NewPatchRegistrationMessage(context, id, bodyAsJSONArray(body));
		}
		if ("synchronize-clock".equals(method)) {
			return new SynchronizeClockMessage(context, id, bodyAsJSONArray(body));
		}
		throw new IllegalArgumentException("Unknown notification message with method: " + method);
	}

	/**
	 * Cast the body as JSONArray.
	 * 
	 * @param body
	 * @return
	 */
	static JSONArray bodyAsJSONArray(Object body) {
		if (!(body instanceof JSONArray)) {
			throw new IllegalArgumentException("patch body must be a JSON array.");
		}
		return (JSONArray) body;
	}

}
