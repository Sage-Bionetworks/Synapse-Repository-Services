package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class PermissionsManagerImpl implements PermissionsManager {
	
	@Autowired
	private NodeInheritanceDAO nodeInheritanceDAO;
	
	@Autowired
	private AccessControlListDAO aclDAO;

	@Override
	public AccessControlList getACL(String nodeId, String userName) throws NotFoundException {
		// get the id that this node inherits its permissions from
		String benefactor = nodeInheritanceDAO.getBenefactor(nodeId);
		return aclDAO.getACL(benefactor);
	}

	@Override
	public AccessControlList updateACL(AccessControlList acl, String userName) {
		// check permissions of userName to change permissions for nodeId
		// validate content
		return aclDAO.updateACL(acl);
	}

	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, String userName) {
		String nodeId = acl.getResourceId();
		// check permissions of userName to change permissions for nodeId
		// set permissions 'benefactor' for nodeId and all nodeId's descendants to nodeId
		// persist acl and return
		return aclDAO.createACL(acl);
	}

	@Override
	public AccessControlList restoreInheritance(String nodeId, String userName) {
		// check permissions of userName to change permissions for nodeId
		// get parent
		// get parent's permissions benefactor
		// set permission's benefactor to equal parent's
		// delete access control list
		return null;
	}

}
