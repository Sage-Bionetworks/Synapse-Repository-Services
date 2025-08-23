package org.sagebionetworks.grid.workers.message.factory;

import org.json.JSONObject;
import org.sagebionetworks.grid.workers.message.ConnectionMessage;
import org.sagebionetworks.grid.workers.message.DisconnectedMessage;
import org.sagebionetworks.grid.workers.message.NotificationMessage;
import org.sagebionetworks.grid.workers.message.PingMessage;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory implements JsonRxMessageFactory<NotificationMessage> {

	@Override
	public JsonRxMessageType type() {
		return JsonRxMessageType.Notification;
	}

	@Override
	public NotificationMessage createMessage(EventContext context, Integer id, String method, Object body) {
		if ("ping".equals(method)) {
			return new PingMessage(context, id, body);
		}
		if ("connection".equals(method)) {
			return new ConnectionMessage(context, (JSONObject)body);
		}
		if ("disconnected".equals(method)) {
			return new DisconnectedMessage(context, id, body);
		}
		throw new IllegalArgumentException("Unknown notification message with method: "+method);
	}

}
