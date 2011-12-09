package org.sagebionetworks.web.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface EntityUpdatedHandler extends EventHandler {

	void onPersistSuccess(EntityUpdatedEvent event);
}
