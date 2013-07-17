package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.HashSet;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

public class PermissionsManagerUtils {
	
	public static void validateACLContent(AccessControlList acl, UserInfo userInfo, Long ownerId) throws InvalidModelException {

		if (acl.getId() == null) {
			throw new InvalidModelException("Resource ID is null");
		}
		if(acl.getResourceAccess() == null) {
			acl.setResourceAccess(new HashSet<ResourceAccess>());
		}
		if(acl.getCreationDate() == null) {
			acl.setCreationDate(new Date(System.currentTimeMillis()));
		}

		// Verify that the caller maintains permissions access
		String callerPrincipalId = userInfo.getIndividualGroup().getId();
		boolean callerIsOwner = callerPrincipalId.equals(ownerId.toString());
		boolean foundCallerInAcl = false;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getPrincipalId()==null) throw new InvalidModelException("Group ID is null");
			if (ra.getAccessType().isEmpty()) throw new InvalidModelException("No access types specified.");
			if (ra.getPrincipalId().toString().equals(callerPrincipalId)) { 
				if (ra.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS)) {
					// Found caller in the ACL, with access to change permissions
					foundCallerInAcl = true;
				}
			}
		}
		
		if (!foundCallerInAcl && !userInfo.isAdmin() && !callerIsOwner) {
			throw new InvalidModelException("Caller is trying to revoke their own ACL editing permissions.");
		}
	}
}