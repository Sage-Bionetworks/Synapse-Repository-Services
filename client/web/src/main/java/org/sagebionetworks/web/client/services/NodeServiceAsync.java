package org.sagebionetworks.web.client.services;

import java.util.List;

import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.users.AclAccessType;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface NodeServiceAsync {

	void getNodeJSONSchema(NodeType type, AsyncCallback<String> callback);

	void getNodeJSON(NodeType type, String id, AsyncCallback<String> callback);

	void createNode(NodeType type, String propertiesJson, AsyncCallback<String> callback);

	void updateNode(NodeType type, String id, String propertiesJson, String eTag, AsyncCallback<String> callback);

	void deleteNode(NodeType type, String id, AsyncCallback<Void> callback);

	void getNodeAnnotationsJSON(NodeType type, String id, AsyncCallback<String> callback);

	void getNodePreview(NodeType type, String id, AsyncCallback<String> callback);

	void getNodeLocations(NodeType type, String id, AsyncCallback<String> callback);

	void updateNodeAnnotations(NodeType type, String id, String annotationsJson, String etag, AsyncCallback<String> callback);
	
	void getNodeAclJSON(NodeType type, String id, AsyncCallback<String> callback);

	void createAcl(NodeType type, String id, String userGroupId,
			List<AclAccessType> accessTypes, AsyncCallback<String> callback);

	void updateAcl(NodeType type, String id, String aclJson, String etag,
			AsyncCallback<String> callback);

	void deleteAcl(NodeType type, String id, AsyncCallback<String> callback);

	void hasAccess(NodeType resourceType, String resourceId, AclAccessType accessType, AsyncCallback<Boolean> callback);

	void getNodeType(String resourceId, AsyncCallback<String> callback);

}
