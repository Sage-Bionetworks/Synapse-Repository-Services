package org.sagebionetworks.grid.workers.message;

import org.sagebionetworks.repo.model.grid.EventContext;

public class DisconnectedMessage extends AbstractJsonRxMessage implements NotificationMessage {

	public DisconnectedMessage(EventContext context, Integer id, Object body) {
		super(context, id, body);
	}
}