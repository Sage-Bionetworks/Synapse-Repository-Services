package org.sagebionetworks.web.client.services;

import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("node")
public interface NodeService extends RemoteService {	

	public String getNodeJSONSchema(NodeType type);
	
	public String getNodeJSON(NodeType type, String id);
	
}
