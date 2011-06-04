package org.sagebionetworks.web.client.services;

import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface NodeServiceAsync {

	void getNodeJSONSchema(NodeType type, AsyncCallback<String> callback);

	void getNodeJSON(NodeType type, String id, AsyncCallback<String> callback);

	void createNode(NodeType type, String propertiesJson, AsyncCallback<String> callback);

	void updateNode(NodeType type, String id, String propertiesJson, String eTag, AsyncCallback<String> callback);

	void deleteNode(NodeType type, String id, AsyncCallback<Void> callback);

	void getNodeAnnotationsJSON(NodeType type, String id, AsyncCallback<String> callback);

	void updateNodeAnnotations(NodeType type, String id, String annotationsJson, String etag, AsyncCallback<String> callback);

	
	// hacks
	void createNodeTwoLayer(NodeType type, String propertiesJson,
			NodeType layerOneType, String layerOneId,
			AsyncCallback<String> callback);

	void updateNodeTwoLayer(NodeType type, String id, String propertiesJson,
			String eTag, NodeType layerOneType, String layerOneId,
			AsyncCallback<String> callback);	

	void getNodeJSONSchemaTwoLayer(NodeType type, NodeType layerOneType,
			String layerOneId, AsyncCallback<String> callback);

	void getNodeJSONTwoLayer(NodeType type, String id, NodeType layerOneType,
			String layerOneId, AsyncCallback<String> callback);


}
