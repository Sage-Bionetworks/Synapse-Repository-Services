package org.sagebionetworks.grid.workers.message;

import org.sagebionetworks.repo.model.grid.EventContext;

public class PingMessage extends AbstractJsonRxMessage implements NotificationMessage {

	public PingMessage(EventContext context, Integer id, Object body) {
		super(context, id, body);
	}
}