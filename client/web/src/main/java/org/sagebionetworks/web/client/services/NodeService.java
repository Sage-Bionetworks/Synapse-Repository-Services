package org.sagebionetworks.web.client.services;

import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("node")
public interface NodeService extends RemoteService {	

	public String getNodeJSONSchema(NodeType type);
	
	public String getNodeJSON(NodeType type, String id);
	
	public String createNode(NodeType type, String propertiesJson);	
	
	public String updateNode(NodeType type, String id, String propertiesJson, String eTag);
	
	public void deleteNode(NodeType type, String id);
	
	public String getNodeAnnotationsJSON(NodeType type, String id);
	
	public String updateNodeAnnotations(NodeType type, String id, String annotationsJson, String etag);

	/**
	 * This is essentially a hack for the layer type which needs a compound path: /dataset/{id}/layer
	 * In the future, layer will be a primary node type
	 */
	public String createNodeTwoLayer(NodeType type, String propertiesJson, NodeType layerOneType, String layerOneId);
	public String updateNodeTwoLayer(NodeType type, String id, String propertiesJson, String eTag, NodeType layerOneType, String layerOneId);
	public String getNodeJSONSchemaTwoLayer(NodeType type, NodeType layerOneType, String layerOneId);	
	public String getNodeJSONTwoLayer(NodeType type, String id, NodeType layerOneType, String layerOneId);
	
}
