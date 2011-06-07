package org.sagebionetworks.web.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface CancelHandler extends EventHandler {

	void onCancel(CancelEvent event);
}
