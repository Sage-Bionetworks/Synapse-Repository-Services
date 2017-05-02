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
	 * @param objectId
	 * @param userId
	 * @return
	 */
	public static AccessControlList createACL(String objectId, long userId, Set<ACCESS_TYPE> permissions, Date now) {
		if(objectId == null) throw new IllegalArgumentException("NodeId cannot be null");
		AccessControlList acl = new AccessControlList();
		acl.setCreatedBy(""+userId);
		acl.setCreationDate(now);
		acl.setId(objectId);
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(new HashSet<ACCESS_TYPE>(permissions));
		access.setPrincipalId(userId);
		set.add(access);
		return acl;
	}

	public static AccessControlList createACLToGrantEntityAdminAccess(String nodeId, UserInfo info, Date now) {
		return createACL(nodeId, info.getId(), ModelConstants.ENITY_ADMIN_ACCESS_PERMISSIONS, now);
	}

	public static AccessControlList createACLToGrantEvaluationAdminAccess(String evaluationId, UserInfo info, Date now) {
		return createACL(evaluationId, info.getId(), ModelConstants.EVALUATION_ADMIN_ACCESS_PERMISSIONS, now);
	}

}
