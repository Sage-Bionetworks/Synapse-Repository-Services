package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.HashSet;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

public class PermissionsManagerUtils {

	/**
	 * Verifies that the caller does not lose the right to change permissions.
	 */
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
		String callerPrincipalId = userInfo.getId().toString();
		boolean callerIsOwner = callerPrincipalId.equals(ownerId.toString());
		boolean foundCallerInAcl = false;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getPrincipalId()==null) throw new InvalidModelException("Group ID is null");
			if (ra.getAccessType().isEmpty()) throw new InvalidModelException("No access types specified.");
			if (userInfo.getGroups().contains(ra.getPrincipalId())) { 
				if (ra.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS)) {
					// Found caller in the ACL, with access to change permissions
					foundCallerInAcl = true;
				}
			}
			// Does not allow ACL for the anonymous user
			if (ra.getPrincipalId().equals(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())) {
				throw new InvalidModelException("Cannot assign permissions to anonymous. To share resources with anonymous users, use the PUBLIC group id (" + BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId() + ")");
			}
			// Does not allow anything other than READ for the public group
			if (ra.getPrincipalId().equals(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId())) {
				long notReadCount = ra.getAccessType().stream().filter( type -> !ACCESS_TYPE.READ.equals(type)).count();
				if (notReadCount != 0) {
					throw new InvalidModelException("Only READ permissions can be assigned to the public group");
				}
			}
			if (ra.getPrincipalId().equals(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId())
					&& ra.getAccessType().contains(ACCESS_TYPE.DOWNLOAD)
					&& !AuthorizationUtils.isCertifiedUser(userInfo)) {
				throw new UserCertificationRequiredException("Only certified users can allow authenticated users to download.");
			}
		}
		
		if (!foundCallerInAcl && !userInfo.isAdmin() && !callerIsOwner) {
			throw new InvalidModelException("Caller is trying to revoke their own ACL editing permissions.");
		}
	}
}
