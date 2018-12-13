package org.sagebionetworks.repo.model.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

public class AccessControlListUtil {
	/**
	 * Will create an ACL that will grant all permissions to a given user for the given node.
	 * @param objectId The entity, team, or evaluation to which the ACL refers
	 * @param info The user or group info which refers to the principal given access to the object
	 * @param permissions The access type permissions to give the user group
	 * @param now The date to set on the DTO
	 * @return a corresponding AccessControlList DTO
	 */
	public static AccessControlList createACL(String objectId, UserInfo info, Set<ACCESS_TYPE> permissions, Date now) {
		if(objectId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(info);
		AccessControlList acl = new AccessControlList();
		acl.setCreationDate(now);
		acl.setId(objectId);
		Set<ResourceAccess> set = new HashSet<>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(new HashSet<>(permissions));
		access.setPrincipalId(info.getId());
		set.add(access);
		return acl;
	}

	public static AccessControlList createACLToGrantEntityAdminAccess(String nodeId, UserInfo info, Date now) {
		return createACL(nodeId, info, ModelConstants.ENITY_ADMIN_ACCESS_PERMISSIONS, now);
	}

	public static AccessControlList createACLToGrantEvaluationAdminAccess(String evaluationId, UserInfo info, Date now) {
		return createACL(evaluationId, info, ModelConstants.EVALUATION_ADMIN_ACCESS_PERMISSIONS, now);
	}

}
