package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

public class PermissionsManagerUtils {

	/**
	 * Verifies that the caller does not lose the right to change permissions.
	 * @param acl the acl to be created or updated
	 * @param userInfo the caller's user info
	 * @param realmIds realmIds is the list of realm ids for the principals listed in the TENTATIVE ACL.
	 * @param ownerId the principal(user) id of the owner of the resource.
	 */
	public static void validateACLContent(AccessControlList acl, UserInfo userInfo, Set<String> realmIds, ObjectType objectType, Long ownerId) throws InvalidModelException {

		if (acl.getId() == null) {
			throw new InvalidModelException("Resource ID is null");
		}
		if(acl.getResourceAccess() == null) {
			acl.setResourceAccess(new HashSet<ResourceAccess>());
		}
		if(acl.getCreationDate() == null) {
			acl.setCreationDate(new Date(System.currentTimeMillis()));
		}
		if (objectType == ObjectType.ACCESS_REQUIREMENT) {
			// we do not check permissions because only ACT can create or update ACLs for Access Requirements.
			return;
		}
		// Verify that the caller maintains permissions access
		String callerPrincipalId = userInfo.getId().toString();
		boolean callerIsOwner = ownerId !=null && callerPrincipalId.equals(ownerId.toString());
		boolean foundCallerInAcl = false;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getPrincipalId()==null) throw new InvalidModelException("Group ID is null");
			if (ra.getAccessType().isEmpty()) throw new InvalidModelException("No access types specified.");
			if (userInfo.getGroups().contains(ra.getPrincipalId())) { 
				if (ra.getAccessType().contains(ObjectTypeToAccessType.getAccessTypesForObjectType(objectType))) {
					// Found caller in the ACL, with access to change permissions
					foundCallerInAcl = true;
				}
			}
			// Does not allow ACL for the anonymous user
			// We need to disallow anonymous users from all realms
			// but this is addressed by the constraint that all principals
			// in the ACL must be in the same realm.
			if (ra.getPrincipalId().equals(userInfo.getRealmAnonymousUserId())) {
				throw new InvalidModelException("Cannot assign permissions to anonymous. To share resources with anonymous users, use the PUBLIC group id (" + userInfo.getRealmPublicUsersId() + ")");
			}
			// Does not allow anything other than READ for the public group
			// As explained above, we don't have to check for Public Groups in other realms
			if (ra.getPrincipalId().equals(userInfo.getRealmPublicUsersId())) {
				long notReadCount = ra.getAccessType().stream().filter( type -> !ACCESS_TYPE.READ.equals(type)).count();
				if (notReadCount != 0) {
					throw new InvalidModelException("Only READ permissions can be assigned to the public group");
				}
			}
			// Note that we don't have to check 'authenticated users' groups in other realms, since
			// there is the constraint that all ACL members must be in the same realm
			if (ra.getPrincipalId().equals(userInfo.getRealmAuthenticatedUsersId())
					&& ra.getAccessType().contains(ACCESS_TYPE.DOWNLOAD)
					&& !AuthorizationUtils.isCertifiedUser(userInfo)) {
				throw new UserCertificationRequiredException("Only certified users can allow authenticated users to download.");
			}
		}
		
		if (!foundCallerInAcl && !userInfo.isAdmin() && !callerIsOwner) {
			throw new InvalidModelException("Caller is trying to revoke their own ACL editing permissions.");
		}

		if (realmIds.size() > 1) {
			throw new InvalidModelException("All principals in the ACL must be from the same realm.");
		}
		if (realmIds.size() == 1 && !realmIds.contains(userInfo.getRealmId())) {
			throw new InvalidModelException("All principals in the ACL must be from the same realm as the caller principal.");
		}
	}
}
