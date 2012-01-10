package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.SerializableWhitelist;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("synapse")	
public interface SynapseClient extends RemoteService {

	public EntityWrapper getEntity(String entityId);

	//public EntityWrapper createEntity(EntityType type, JSONObjectAdaptor properties);
	
	public String getEntityTypeRegistryJSON();
	
	public EntityWrapper getEntityPath(String entityId, String urlPrefix);
	
	public SerializableWhitelist junk(SerializableWhitelist l); 
	
}
