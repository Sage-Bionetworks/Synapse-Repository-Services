package org.sagebionetworks.web.client.services;

import java.util.List;

import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.users.AclAccessType;
import org.sagebionetworks.web.shared.users.AclPrincipal;

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
	
	public String getNodePreview(NodeType type, String id);
	
	public String getNodeLocations(NodeType type, String id);
	
	public String updateNodeAnnotations(NodeType type, String id, String annotationsJson, String etag);
	
	public String getNodeAclJSON(NodeType type, String id);
	
	public String createAcl(NodeType type, String id, String ownerUserName, List<AclAccessType> accessTypes);
	
	public String updateAcl(NodeType type, String id, String aclJson, String etag);
	
	public String deleteAcl(NodeType type, String id);

	public boolean hasAccess(NodeType resourceType, String resourceId, AclAccessType accessType);

	public String getNodeType(String resourceId);

	public List<AclPrincipal> getAllUsers();
	
	public List<AclPrincipal> getAllGroups();
	
	public List<AclPrincipal> getAllUsersAndGroups();

}
