package org.sagebionetworks.web.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface PersistSuccessHandler extends EventHandler {

	void onPersistSuccess(PersistSuccessEvent event);
}
