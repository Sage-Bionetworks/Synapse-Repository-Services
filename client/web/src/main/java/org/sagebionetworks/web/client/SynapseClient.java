package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.EntityWrapper;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("synapse")	
public interface SynapseClient extends RemoteService {

	public EntityWrapper getEntity(String entityId);

	//public EntityWrapper createEntity(EntityType type, JSONObjectAdaptor properties);
	
	public String getEntityTypeRegistryJSON();
	
}
