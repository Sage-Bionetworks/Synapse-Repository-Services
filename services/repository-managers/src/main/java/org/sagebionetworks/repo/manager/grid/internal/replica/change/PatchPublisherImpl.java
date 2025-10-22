package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import org.json.JSONArray;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.springframework.stereotype.Service;

@Service
public class PatchPublisherImpl implements PatchPublisher {

	private final InternalReplicaToHubEventPublisher eventPublisher;

	public PatchPublisherImpl(InternalReplicaToHubEventPublisher eventPublisher) {
		super();
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void publishPatch(GridConnectionInfo connection, JSONArray patchBody, Long patchSpan) {
		// we use a constant id since we are not currently listening to the response.
		JsonRxMessage message = new JsonRxMessage(JsonRxMessageType.RequestData).setId(1010).setMethod("patch")
				.setBody(patchBody);

		EventContext context = new EventContext(EventType.MESSAGE, connection.getSource(),
				connection.getConnectionId());
		eventPublisher.publishEvent(context, message);
	}

}
