package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.EntityWrapper;

import com.google.gwt.user.client.rpc.AsyncCallback;
	
public interface SynapseClientAsync {

	void getEntity(String entityId, AsyncCallback<EntityWrapper> callback);

}
