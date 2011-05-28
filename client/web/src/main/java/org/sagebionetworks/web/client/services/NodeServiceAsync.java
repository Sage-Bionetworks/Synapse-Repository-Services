package org.sagebionetworks.web.client.services;

import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface NodeServiceAsync {

	void getNodeJSONSchema(NodeType type, AsyncCallback<String> callback);

	void getNodeJSON(NodeType type, String id, AsyncCallback<String> callback);
	
}
