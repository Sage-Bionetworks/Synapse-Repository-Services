package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.SerializableWhitelist;

import com.google.gwt.user.client.rpc.AsyncCallback;
	
public interface SynapseClientAsync {

	void getEntity(String entityId, AsyncCallback<EntityWrapper> callback);

	void getEntityTypeRegistryJSON(AsyncCallback<String> callback);

	void getEntityPath(String entityId, String urlPrefix, AsyncCallback<EntityWrapper> callback);

	void junk(SerializableWhitelist l,
			AsyncCallback<SerializableWhitelist> callback);
	
}
